package net.sumomo_planning.taskringk.data.repository

import android.util.Log
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.Json
import net.sumomo_planning.taskringk.core.common.DeviceIdService
import net.sumomo_planning.taskringk.core.network.NetworkMonitor
import net.sumomo_planning.taskringk.data.local.room.dao.SharedListDao
import net.sumomo_planning.taskringk.data.mapper.toDomain
import net.sumomo_planning.taskringk.data.mapper.toEntity
import net.sumomo_planning.taskringk.data.remote.firestore.FirestoreSharedListDataSource
import net.sumomo_planning.taskringk.domain.model.ListKind
import net.sumomo_planning.taskringk.domain.model.ListType
import net.sumomo_planning.taskringk.domain.model.SharedItem
import net.sumomo_planning.taskringk.domain.model.SharedList
import net.sumomo_planning.taskringk.domain.repository.SharedListRepository

/**
 * Hybrid implementation of [SharedListRepository].
 *
 * Write strategy (porting_spec §6-2):
 *  Firestore is attempted first; a failure is logged but does NOT block the
 *  local Room write, so the UI is always updated immediately.
 *
 * Read strategy (Phase 5 — porting_spec §7 / flutter_vs_kotlin §4):
 *  [observeListsByGroup] and [observeList] use [NetworkMonitor.isOnlineFlow]
 *  via flatMapLatest:
 *   - Online  → Firestore listener → cache to Room → emit
 *   - Offline → Room cache directly
 *   - Error   → fallback to Room
 *  When connectivity changes the inner flow is automatically restarted.
 *
 * Item partial update (porting_spec §12-1):
 *  [addOrUpdateItem] and [removeItem] write only `items.{itemId}` in Firestore.
 *  The local Room cache is updated optimistically.
 *
 * Logical delete (porting_spec §12-2):
 *  [removeItem] sets isDeleted=true; the item is never physically deleted.
 */
@Singleton
class HybridSharedListRepositoryImpl @Inject constructor(
    private val firestoreDataSource: FirestoreSharedListDataSource,
    private val sharedListDao: SharedListDao,
    private val deviceIdService: DeviceIdService,
    private val networkMonitor: NetworkMonitor,
    private val json: Json,
) : SharedListRepository {

    override fun observeListsByGroup(groupId: String): Flow<List<SharedList>> =
        networkMonitor.isOnlineFlow.flatMapLatest { isOnline ->
            if (isOnline) {
                firestoreDataSource.observeByGroup(groupId)
                    .onEach { lists ->
                        sharedListDao.upsertAll(lists.map { it.toEntity(json) })
                    }
                    .catch { error ->
                        Log.w(TAG, "Firestore observeListsByGroup error — falling back to Room", error)
                        emitAll(
                            sharedListDao.observeByGroup(groupId)
                                .map { entities -> entities.map { it.toDomain(json) } }
                        )
                    }
            } else {
                Log.d(TAG, "Offline — observeListsByGroup serving from Room cache")
                sharedListDao.observeByGroup(groupId)
                    .map { entities -> entities.map { it.toDomain(json) } }
            }
        }

    override fun observeList(groupId: String, listId: String): Flow<SharedList?> =
        networkMonitor.isOnlineFlow.flatMapLatest { isOnline ->
            if (isOnline) {
                firestoreDataSource.observeList(groupId, listId)
                    .onEach { list ->
                        if (list != null) sharedListDao.upsert(list.toEntity(json))
                    }
                    .catch { error ->
                        Log.w(TAG, "Firestore observeList error — falling back to Room", error)
                        emitAll(
                            sharedListDao.observeById(listId)
                                .map { entity -> entity?.toDomain(json) }
                        )
                    }
            } else {
                Log.d(TAG, "Offline — observeList serving from Room cache")
                sharedListDao.observeById(listId)
                    .map { entity -> entity?.toDomain(json) }
            }
        }

    override suspend fun createList(
        groupId: String,
        groupName: String,
        listName: String,
        description: String,
        ownerUid: String,
    ): Result<SharedList> = runCatching {
        val devicePrefix = ownerUid.take(8)
        val listId = deviceIdService.generateListId(devicePrefix)
        val now = Instant.now()

        val list = SharedList(
            listId = listId,
            listName = listName,
            ownerUid = ownerUid,
            groupId = groupId,
            groupName = groupName,
            description = description,
            listType = ListType.SHOPPING,
            listKind = ListKind.SHOPPING_LIST,
            items = emptyMap(),
            createdAt = now,
            updatedAt = now,
        )

        // Firestore first; failure does not block the local write (§6-2)
        runCatching { firestoreDataSource.createList(groupId, list) }
            .onFailure { Log.w(TAG, "Firestore createList failed (non-fatal)", it) }

        sharedListDao.upsert(list.toEntity(json))
        list
    }

    override suspend fun deleteList(groupId: String, listId: String): Result<Unit> = runCatching {
        runCatching { firestoreDataSource.deleteList(groupId, listId) }
            .onFailure { Log.w(TAG, "Firestore deleteList failed (non-fatal)", it) }

        sharedListDao.deleteById(listId)
    }

    override suspend fun addOrUpdateItem(
        groupId: String,
        listId: String,
        item: SharedItem,
    ): Result<Unit> = runCatching {
        // Partial Firestore update — items.{itemId} only (§12-1)
        runCatching { firestoreDataSource.addOrUpdateItem(groupId, listId, item) }
            .onFailure { Log.w(TAG, "Firestore addOrUpdateItem failed (non-fatal)", it) }

        // Optimistic Room cache update
        updateRoomItem(listId, item)
    }

    override suspend fun removeItem(
        groupId: String,
        listId: String,
        itemId: String,
    ): Result<Unit> = runCatching {
        // Logical delete in Firestore (§12-2)
        runCatching { firestoreDataSource.removeItem(groupId, listId, itemId) }
            .onFailure { Log.w(TAG, "Firestore removeItem failed (non-fatal)", it) }

        // Optimistic Room cache: set isDeleted=true
        val deletedItem = getItemFromRoom(listId, itemId)?.copy(
            isDeleted = true,
            deletedAt = Instant.now(),
        ) ?: return@runCatching
        updateRoomItem(listId, deletedItem)
    }

    // ---- Room helpers ----

    /** Fetches the entity from Room, updates the given item, and upserts back. */
    private suspend fun updateRoomItem(listId: String, item: SharedItem) {
        val entity = sharedListDao.observeById(listId).firstOrNull() ?: return
        val list = entity.toDomain(json)
        val updatedList = list.copy(
            items = list.items.toMutableMap().apply { put(item.itemId, item) },
            updatedAt = Instant.now(),
        )
        sharedListDao.upsert(updatedList.toEntity(json))
    }

    private suspend fun getItemFromRoom(listId: String, itemId: String): SharedItem? {
        val entity = sharedListDao.observeById(listId).firstOrNull() ?: return null
        return entity.toDomain(json).items[itemId]
    }

    private companion object {
        const val TAG = "HybridListRepository"
    }
}
