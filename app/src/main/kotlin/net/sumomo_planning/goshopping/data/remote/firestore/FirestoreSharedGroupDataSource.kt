package net.sumomo_planning.goshopping.data.remote.firestore

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import net.sumomo_planning.goshopping.data.mapper.toDomain
import net.sumomo_planning.goshopping.data.mapper.toFirestoreMap
import net.sumomo_planning.goshopping.data.mapper.toUpdateMap
import net.sumomo_planning.goshopping.data.remote.dto.SharedGroupDto
import net.sumomo_planning.goshopping.domain.model.SharedGroup

/**
 * Firestore data source for SharedGroup documents at /SharedGroups/{groupId}.
 *
 * Key rules from porting_spec:
 *  - Query with `whereArrayContains("allowedUid", uid)` (§12-5)
 *  - Never use runTransaction() — use set/update (§12-3)
 *  - observeGroups uses callbackFlow (§7-1 pattern)
 */
@Singleton
class FirestoreSharedGroupDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    private val collection get() = firestore.collection("SharedGroups")

    /**
     * Emits all groups the user belongs to in real time.
     * Closes with error on Firestore failure (caller should fall back to Room).
     */
    fun observeGroups(uid: String): Flow<List<SharedGroup>> = callbackFlow {
        val listener = collection
            .whereArrayContains("allowedUid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val groups = snapshot?.documents?.mapNotNull { doc ->
                    runCatching { doc.toObject(SharedGroupDto::class.java)?.toDomain() }
                        .onFailure { Log.w(TAG, "Failed to deserialize group ${doc.id}", it) }
                        .getOrNull()
                } ?: emptyList()
                trySend(groups)
            }
        awaitClose { listener.remove() }
    }

    /** Writes a new group document. Throws on network failure. */
    suspend fun createGroup(group: SharedGroup) {
        collection.document(group.groupId).set(group.toFirestoreMap()).await()
    }

    /** Deletes a group document. Throws on network failure. */
    suspend fun deleteGroup(groupId: String) {
        collection.document(groupId).delete().await()
    }

    /**
     * Removes [uid] from `allowedUid` and removes the member with [memberId]
     * from the `members` array, then updates `updatedAt` with server time.
     *
     * Uses update() without runTransaction() to avoid offline hangs (§12-3).
     */
    suspend fun leaveGroup(groupId: String, uid: String, memberId: String) {
        val docRef = collection.document(groupId)
        val snapshot = docRef.get().await()
        val dto = snapshot.toObject(SharedGroupDto::class.java)
            ?: throw IllegalStateException("Group $groupId not found in Firestore")

        val updatedMembers = dto.members
            .filter { it.memberId != memberId }
            .map { it.toUpdateMap() }

        docRef.update(
            mapOf(
                "allowedUid" to FieldValue.arrayRemove(uid),
                "members" to updatedMembers,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        ).await()
    }

    private companion object {
        const val TAG = "FirestoreGroupDataSource"
    }
}
