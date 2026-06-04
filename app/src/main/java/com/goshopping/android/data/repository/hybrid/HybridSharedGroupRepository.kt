package com.goshopping.android.data.repository.hybrid

import com.goshopping.android.data.model.SharedGroup
import com.goshopping.android.data.model.SharedGroupMember
import com.goshopping.android.data.repository.SharedGroupRepository
import com.goshopping.android.data.repository.firestore.FirestoreSharedGroupRepository
import com.goshopping.android.data.repository.local.LocalSharedGroupRepository

/**
 * Hybrid実装: Firestore ファーストで操作し、失敗時はローカルにフォールバックする。
 * 仕様書 §6-2 参照。
 */
class HybridSharedGroupRepository(
    private val firestoreRepo: FirestoreSharedGroupRepository,
    private val localRepo: LocalSharedGroupRepository
) : SharedGroupRepository {

    override suspend fun createGroup(
        groupId: String,
        groupName: String,
        member: SharedGroupMember
    ): SharedGroup {
        try {
            firestoreRepo.createGroup(groupId, groupName, member)
        } catch (_: Exception) {
            // Firestore 失敗でもローカルへの書き込みは続行する
        }
        return localRepo.createGroup(groupId, groupName, member)
    }

    override suspend fun getAllGroups(): List<SharedGroup> {
        return try {
            val groups = firestoreRepo.getAllGroups()
            localRepo.saveGroups(groups)
            groups
        } catch (_: Exception) {
            localRepo.getAllGroups()
        }
    }

    override suspend fun getGroupById(groupId: String): SharedGroup {
        return try {
            val group = firestoreRepo.getGroupById(groupId)
            localRepo.updateGroup(groupId, group)
            group
        } catch (_: Exception) {
            localRepo.getGroupById(groupId)
        }
    }

    override suspend fun updateGroup(groupId: String, group: SharedGroup): SharedGroup {
        try {
            firestoreRepo.updateGroup(groupId, group)
        } catch (_: Exception) {}
        return localRepo.updateGroup(groupId, group)
    }

    override suspend fun deleteGroup(groupId: String): SharedGroup {
        try {
            firestoreRepo.deleteGroup(groupId)
        } catch (_: Exception) {}
        return localRepo.deleteGroup(groupId)
    }

    override suspend fun addMember(groupId: String, member: SharedGroupMember): SharedGroup {
        try {
            firestoreRepo.addMember(groupId, member)
        } catch (_: Exception) {}
        return localRepo.addMember(groupId, member)
    }

    override suspend fun removeMember(groupId: String, member: SharedGroupMember): SharedGroup {
        try {
            firestoreRepo.removeMember(groupId, member)
        } catch (_: Exception) {}
        // 仕様書 §12-9: ローカルからも即座に削除する
        return localRepo.removeMember(groupId, member)
    }
}
