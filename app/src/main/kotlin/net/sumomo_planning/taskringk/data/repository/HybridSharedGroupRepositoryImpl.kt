package net.sumomo_planning.taskringk.data.repository

import android.util.Log
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.Json
import net.sumomo_planning.taskringk.core.common.DeviceIdService
import net.sumomo_planning.taskringk.core.network.NetworkMonitor
import net.sumomo_planning.taskringk.data.local.room.dao.SharedGroupDao
import net.sumomo_planning.taskringk.data.local.room.dao.SharedListDao
import net.sumomo_planning.taskringk.data.mapper.toDomain
import net.sumomo_planning.taskringk.data.mapper.toEntity
import net.sumomo_planning.taskringk.data.remote.firestore.FirestoreSharedGroupDataSource
import net.sumomo_planning.taskringk.domain.model.InvitationStatus
import net.sumomo_planning.taskringk.domain.model.SharedGroup
import net.sumomo_planning.taskringk.domain.model.SharedGroupMember
import net.sumomo_planning.taskringk.domain.model.SharedGroupRole
import net.sumomo_planning.taskringk.domain.model.SyncStatus
import net.sumomo_planning.taskringk.domain.repository.SharedGroupRepository

/**
 * Hybrid implementation of [SharedGroupRepository].
 *
 * Write strategy (porting_spec §6-2):
 *  Firestore is attempted first; a Firestore failure is logged but does NOT
 *  block the local Room write, so the UI is always updated immediately.
 *
 * Read strategy (Phase 5 — porting_spec §7 / flutter_vs_kotlin §4):
 *  [observeGroups] uses [NetworkMonitor.isOnlineFlow] via flatMapLatest:
 *   - Online  → Firestore listener → cache to Room → emit
 *   - Offline → Room cache directly
 *   - Error   → fallback to Room
 *  When connectivity changes the inner flow is automatically restarted.
 *
 * Local cleanup (porting_spec §12-9):
 *  deleteGroup and leaveGroup both delete the group AND its associated lists
 *  from Room immediately so the UI reflects the change without waiting for
 *  Firestore.
 */
@Singleton
class HybridSharedGroupRepositoryImpl @Inject constructor(
    private val firestoreDataSource: FirestoreSharedGroupDataSource,
    private val sharedGroupDao: SharedGroupDao,
    private val sharedListDao: SharedListDao,
    private val deviceIdService: DeviceIdService,
    private val networkMonitor: NetworkMonitor,
    private val json: Json,
) : SharedGroupRepository {

    override fun observeGroups(uid: String): Flow<List<SharedGroup>> =
        networkMonitor.isOnlineFlow.flatMapLatest { isOnline ->
            if (isOnline) {
                firestoreDataSource.observeGroups(uid)
                    .onEach { groups ->
                        sharedGroupDao.upsertAll(groups.map { it.toEntity(json) })
                    }
                    .catch { error ->
                        Log.w(TAG, "Firestore observeGroups error — falling back to Room", error)
                        emitAll(
                            sharedGroupDao.observeAll()
                                .map { entities -> entities.map { it.toDomain(json) } }
                        )
                    }
            } else {
                Log.d(TAG, "Offline — observeGroups serving from Room cache")
                sharedGroupDao.observeAll()
                    .map { entities -> entities.map { it.toDomain(json) } }
            }
        }

    override suspend fun createGroup(
        groupName: String,
        ownerUid: String,
        ownerDisplayName: String,
        ownerEmail: String,
    ): Result<SharedGroup> = runCatching {
        val devicePrefix = ownerUid.take(8)
        val groupId = deviceIdService.generateGroupId(devicePrefix)
        val memberId = deviceIdService.generateMemberId()
        val now = Instant.now()

        val ownerMember = SharedGroupMember(
            memberId = memberId,
            name = ownerDisplayName,
            contact = ownerEmail,
            role = SharedGroupRole.OWNER,
            isSignedIn = true,
            invitationStatus = InvitationStatus.SELF,
            invitedAt = now,
            acceptedAt = now,
        )
        val group = SharedGroup(
            groupId = groupId,
            groupName = groupName,
            ownerUid = ownerUid,
            allowedUid = listOf(ownerUid),
            members = listOf(ownerMember),
            syncStatus = SyncStatus.SYNCED,
            createdAt = now,
            updatedAt = now,
        )

        // Firestore first; failure does not block the local write (§6-2)
        runCatching { firestoreDataSource.createGroup(group) }
            .onFailure { Log.w(TAG, "Firestore createGroup failed (non-fatal)", it) }

        sharedGroupDao.upsert(group.toEntity(json))
        group
    }

    override suspend fun deleteGroup(groupId: String): Result<Unit> = runCatching {
        // Firestore first; failure is non-fatal
        runCatching { firestoreDataSource.deleteGroup(groupId) }
            .onFailure { Log.w(TAG, "Firestore deleteGroup failed (non-fatal)", it) }

        // Immediate local cleanup (§12-9): group + all its lists
        sharedGroupDao.deleteById(groupId)
        sharedListDao.deleteByGroup(groupId)
    }

    override suspend fun leaveGroup(
        groupId: String,
        uid: String,
        memberId: String,
    ): Result<Unit> = runCatching {
        // Firestore first; failure is non-fatal
        runCatching { firestoreDataSource.leaveGroup(groupId, uid, memberId) }
            .onFailure { Log.w(TAG, "Firestore leaveGroup failed (non-fatal)", it) }

        // Immediate local cleanup (§12-9)
        sharedGroupDao.deleteById(groupId)
        sharedListDao.deleteByGroup(groupId)
    }

    private companion object {
        const val TAG = "HybridGroupRepository"
    }
}
