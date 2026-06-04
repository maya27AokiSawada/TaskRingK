package com.goshopping.android.data.repository

import com.goshopping.android.data.model.SharedItem
import com.goshopping.android.data.model.SharedList
import kotlinx.coroutines.flow.Flow

interface SharedListRepository {
    suspend fun createSharedList(
        ownerUid: String,
        groupId: String,
        listName: String,
        description: String? = null,
        customListId: String? = null
    ): SharedList

    suspend fun getSharedListById(groupId: String, listId: String): SharedList?

    suspend fun getSharedListsByGroup(groupId: String): List<SharedList>

    suspend fun updateSharedList(list: SharedList)

    suspend fun deleteSharedList(groupId: String, listId: String)

    // ――― アイテム差分更新（全件置換禁止） ―――
    /** アイテムを追加する（items.{itemId} フィールドの差分更新） */
    suspend fun addSingleItem(groupId: String, listId: String, item: SharedItem)

    /** アイテムを更新する（items.{itemId} フィールドの差分更新） */
    suspend fun updateSingleItem(groupId: String, listId: String, item: SharedItem)

    /** アイテムを論理削除する（isDeleted=true に設定、物理削除は行わない） */
    suspend fun removeSingleItem(groupId: String, listId: String, itemId: String)

    // ――― リアルタイム監視 ―――
    /** Firestore のリアルタイムリスナーで sharedList を監視する Flow を返す */
    fun watchSharedList(groupId: String, listId: String): Flow<SharedList?>
}
