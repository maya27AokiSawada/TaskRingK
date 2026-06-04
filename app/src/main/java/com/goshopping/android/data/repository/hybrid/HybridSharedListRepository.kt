package com.goshopping.android.data.repository.hybrid

import com.goshopping.android.data.model.SharedItem
import com.goshopping.android.data.model.SharedList
import com.goshopping.android.data.repository.SharedListRepository
import com.goshopping.android.data.repository.firestore.FirestoreSharedListRepository
import com.goshopping.android.data.repository.local.LocalSharedListRepository
import kotlinx.coroutines.flow.Flow

/**
 * Hybrid実装: Firestore ファーストで操作し、失敗時はローカルにフォールバックする。
 * watchSharedList は Firestore のリアルタイムリスナーを優先する。
 */
class HybridSharedListRepository(
    private val firestoreRepo: FirestoreSharedListRepository,
    private val localRepo: LocalSharedListRepository
) : SharedListRepository {

    override suspend fun createSharedList(
        ownerUid: String,
        groupId: String,
        listName: String,
        description: String?,
        customListId: String?
    ): SharedList {
        try {
            firestoreRepo.createSharedList(ownerUid, groupId, listName, description, customListId)
        } catch (_: Exception) {}
        return localRepo.createSharedList(ownerUid, groupId, listName, description, customListId)
    }

    override suspend fun getSharedListById(groupId: String, listId: String): SharedList? {
        return try {
            val list = firestoreRepo.getSharedListById(groupId, listId)
            if (list != null) {
                localRepo.saveLists(listOf(list))
            }
            list
        } catch (_: Exception) {
            localRepo.getSharedListById(groupId, listId)
        }
    }

    override suspend fun getSharedListsByGroup(groupId: String): List<SharedList> {
        return try {
            val lists = firestoreRepo.getSharedListsByGroup(groupId)
            localRepo.saveLists(lists)
            lists
        } catch (_: Exception) {
            localRepo.getSharedListsByGroup(groupId)
        }
    }

    override suspend fun updateSharedList(list: SharedList) {
        try {
            firestoreRepo.updateSharedList(list)
        } catch (_: Exception) {}
        localRepo.updateSharedList(list)
    }

    override suspend fun deleteSharedList(groupId: String, listId: String) {
        try {
            firestoreRepo.deleteSharedList(groupId, listId)
        } catch (_: Exception) {}
        localRepo.deleteSharedList(groupId, listId)
    }

    override suspend fun addSingleItem(groupId: String, listId: String, item: SharedItem) {
        try {
            firestoreRepo.addSingleItem(groupId, listId, item)
        } catch (_: Exception) {}
        localRepo.addSingleItem(groupId, listId, item)
    }

    override suspend fun updateSingleItem(groupId: String, listId: String, item: SharedItem) {
        try {
            firestoreRepo.updateSingleItem(groupId, listId, item)
        } catch (_: Exception) {}
        localRepo.updateSingleItem(groupId, listId, item)
    }

    override suspend fun removeSingleItem(groupId: String, listId: String, itemId: String) {
        try {
            firestoreRepo.removeSingleItem(groupId, listId, itemId)
        } catch (_: Exception) {}
        // 仕様書 §12-2: 論理削除（isDeleted=true）
        localRepo.removeSingleItem(groupId, listId, itemId)
    }

    /** Firestore のリアルタイム Flow を返す（オフライン時はローカルにフォールバック） */
    override fun watchSharedList(groupId: String, listId: String): Flow<SharedList?> =
        firestoreRepo.watchSharedList(groupId, listId)
}
