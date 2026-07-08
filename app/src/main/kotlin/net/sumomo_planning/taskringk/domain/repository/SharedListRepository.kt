package net.sumomo_planning.taskringk.domain.repository

import kotlinx.coroutines.flow.Flow
import net.sumomo_planning.taskringk.domain.model.SharedItem
import net.sumomo_planning.taskringk.domain.model.SharedList

/**
 * SharedList operations exposed to the domain layer.
 *
 * Hybrid strategy (porting_spec §6-2):
 *  - Firestore is the source of truth; Room is a read cache.
 *  - Writes attempt Firestore first; failures do not block local updates.
 *
 * Item write rules (porting_spec §12-1, §12-2):
 *  - addOrUpdateItem uses `items.{itemId}` partial update — never full map replacement.
 *  - removeItem is a logical delete (isDeleted=true); UI filters via activeItems.
 */
interface SharedListRepository {

    /**
     * Observe all lists in [groupId].
     * Emits from Firestore and caches to Room; falls back to Room on Firestore error.
     */
    fun observeListsByGroup(groupId: String): Flow<List<SharedList>>

    /**
     * Observe a single list document.
     * Emits from Firestore and caches to Room; falls back to Room on Firestore error.
     */
    fun observeList(groupId: String, listId: String): Flow<SharedList?>

    /** Create a new list in [groupId]. Firestore first, then Room. */
    suspend fun createList(
        groupId: String,
        groupName: String,
        listName: String,
        description: String,
        ownerUid: String,
    ): Result<SharedList>

    /** Delete a list. Firestore first, then Room. */
    suspend fun deleteList(groupId: String, listId: String): Result<Unit>

    /**
     * Add or update [item] in [listId] using partial update (§12-1).
     * Writes `items.{itemId}` field only.
     */
    suspend fun addOrUpdateItem(groupId: String, listId: String, item: SharedItem): Result<Unit>

    /**
     * Logically delete [itemId] from [listId] (§12-2).
     * Sets isDeleted=true; item stays in Firestore and Room.
     */
    suspend fun removeItem(groupId: String, listId: String, itemId: String): Result<Unit>
}
