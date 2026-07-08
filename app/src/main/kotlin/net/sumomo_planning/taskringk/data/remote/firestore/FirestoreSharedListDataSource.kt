package net.sumomo_planning.taskringk.data.remote.firestore

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import net.sumomo_planning.taskringk.data.mapper.toDomain
import net.sumomo_planning.taskringk.data.mapper.toFirestoreMap
import net.sumomo_planning.taskringk.data.mapper.toItemMap
import net.sumomo_planning.taskringk.data.remote.dto.SharedListDto
import net.sumomo_planning.taskringk.domain.model.SharedItem
import net.sumomo_planning.taskringk.domain.model.SharedList

/**
 * Firestore data source for SharedList documents at
 * /SharedGroups/{groupId}/sharedLists/{listId}.
 *
 * Key rules:
 *  - Item add/update: `items.{itemId}` partial update — never replace entire map (§12-1)
 *  - Item delete: logical delete (isDeleted=true) only (§12-2)
 *  - Never use runTransaction() — use update() to avoid offline hangs (§12-3)
 */
@Singleton
class FirestoreSharedListDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    private fun listsCollection(groupId: String) =
        firestore.collection("SharedGroups").document(groupId).collection("sharedLists")

    /**
     * Emits all lists in [groupId] in real time.
     * Closes with error on Firestore failure (caller falls back to Room).
     */
    fun observeByGroup(groupId: String): Flow<List<SharedList>> = callbackFlow {
        val listener = listsCollection(groupId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val lists = snapshot?.documents?.mapNotNull { doc ->
                    runCatching { doc.toObject(SharedListDto::class.java)?.toDomain() }
                        .onFailure { Log.w(TAG, "Failed to deserialize list ${doc.id}", it) }
                        .getOrNull()
                } ?: emptyList()
                trySend(lists)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Emits a single list document in real time.
     * Closes with error on Firestore failure.
     */
    fun observeList(groupId: String, listId: String): Flow<SharedList?> = callbackFlow {
        val listener = listsCollection(groupId).document(listId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.let { doc ->
                    runCatching { doc.toObject(SharedListDto::class.java)?.toDomain() }
                        .onFailure { Log.w(TAG, "Failed to deserialize list $listId", it) }
                        .getOrNull()
                }
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    /** Creates a new list document. Throws on network failure. */
    suspend fun createList(groupId: String, list: SharedList) {
        listsCollection(groupId).document(list.listId).set(list.toFirestoreMap()).await()
    }

    /** Deletes a list document. Throws on network failure. */
    suspend fun deleteList(groupId: String, listId: String) {
        listsCollection(groupId).document(listId).delete().await()
    }

    /**
     * Partial update for add/update item — writes `items.{itemId}` only (§12-1).
     * Never replaces the entire items map.
     */
    suspend fun addOrUpdateItem(groupId: String, listId: String, item: SharedItem) {
        listsCollection(groupId).document(listId).update(
            mapOf(
                "items.${item.itemId}" to item.toItemMap(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        ).await()
    }

    /**
     * Logical delete: sets `isDeleted=true` and `deletedAt=serverTimestamp()` (§12-2).
     * The item remains in Firestore; UI hides it via activeItems filter.
     */
    suspend fun removeItem(groupId: String, listId: String, itemId: String) {
        listsCollection(groupId).document(listId).update(
            mapOf(
                "items.$itemId.isDeleted" to true,
                "items.$itemId.deletedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        ).await()
    }

    private companion object {
        const val TAG = "FirestoreListDataSource"
    }
}
