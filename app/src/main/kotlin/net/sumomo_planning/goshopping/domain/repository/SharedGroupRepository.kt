package net.sumomo_planning.goshopping.domain.repository

import kotlinx.coroutines.flow.Flow
import net.sumomo_planning.goshopping.domain.model.SharedGroup

/**
 * Group management operations exposed to the domain layer.
 *
 * Hybrid strategy (porting_spec §6-2):
 *  - Firestore is the source of truth; Room is a read cache.
 *  - Writes attempt Firestore first; failures do not block local updates.
 */
interface SharedGroupRepository {

    /**
     * Observe all groups the given [uid] belongs to.
     *
     * Emits from Firestore (real-time) and caches to Room on each emission.
     * Falls back to Room when Firestore is unavailable.
     *
     * Query: `whereArrayContains("allowedUid", uid)` (porting_spec §12-5)
     */
    fun observeGroups(uid: String): Flow<List<SharedGroup>>

    /**
     * Create a new group owned by [ownerUid].
     *
     * Writes to Firestore first; failure does not block the local Room write.
     */
    suspend fun createGroup(
        groupName: String,
        ownerUid: String,
        ownerDisplayName: String,
        ownerEmail: String,
    ): Result<SharedGroup>

    /**
     * Delete a group (owner only).
     *
     * Cleans up Firestore and removes the group + its lists from Room (§12-9).
     */
    suspend fun deleteGroup(groupId: String): Result<Unit>

    /**
     * Leave a group (member only).
     *
     * Removes the member's UID from `allowedUid` in Firestore, then
     * immediately cleans up the group and its lists from Room (§12-9).
     */
    suspend fun leaveGroup(groupId: String, uid: String, memberId: String): Result<Unit>
}
