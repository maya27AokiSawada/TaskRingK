package com.goshopping.android.data.repository.local

import com.goshopping.android.data.model.GroupType
import com.goshopping.android.data.model.InvitationStatus
import com.goshopping.android.data.model.SharedGroup
import com.goshopping.android.data.model.SharedGroupMember
import com.goshopping.android.data.model.SharedGroupRole
import com.goshopping.android.data.model.SyncStatus
import com.goshopping.android.data.repository.SharedGroupRepository
import com.goshopping.android.data.repository.room.MemberJson
import com.goshopping.android.data.repository.room.SharedGroupDao
import com.goshopping.android.data.repository.room.SharedGroupEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class LocalSharedGroupRepository(
    private val dao: SharedGroupDao
) : SharedGroupRepository {

    private val gson = Gson()

    override suspend fun createGroup(
        groupId: String,
        groupName: String,
        member: SharedGroupMember
    ): SharedGroup {
        val group = SharedGroup(
            groupId = groupId,
            groupName = groupName,
            ownerUid = member.memberId,
            allowedUid = listOf(member.memberId),
            members = listOf(member),
            syncStatus = SyncStatus.LOCAL,
            createdAt = Date()
        )
        dao.upsert(group.toEntity())
        return group
    }

    override suspend fun getAllGroups(): List<SharedGroup> =
        dao.getAll().map { it.toModel() }

    override suspend fun getGroupById(groupId: String): SharedGroup {
        return dao.getById(groupId)?.toModel()
            ?: throw NoSuchElementException("Group $groupId not found in local DB")
    }

    override suspend fun updateGroup(groupId: String, group: SharedGroup): SharedGroup {
        dao.upsert(group.toEntity())
        return group
    }

    override suspend fun deleteGroup(groupId: String): SharedGroup {
        val group = getGroupById(groupId)
        dao.deleteById(groupId)
        return group
    }

    override suspend fun addMember(groupId: String, member: SharedGroupMember): SharedGroup {
        val group = getGroupById(groupId)
        val updated = group.copy(
            members = group.members + member,
            updatedAt = Date()
        )
        dao.upsert(updated.toEntity())
        return updated
    }

    override suspend fun removeMember(groupId: String, member: SharedGroupMember): SharedGroup {
        val group = getGroupById(groupId)
        val updated = group.copy(
            members = group.members.filter { it.memberId != member.memberId },
            updatedAt = Date()
        )
        dao.upsert(updated.toEntity())
        return updated
    }

    /** Firestoreから取得したグループ一覧をローカルにキャッシュする */
    suspend fun saveGroups(groups: List<SharedGroup>) {
        dao.upsertAll(groups.map { it.toEntity() })
    }

    suspend fun deleteAllGroups() {
        dao.deleteAll()
    }

    // ――― 変換 ―――

    private fun SharedGroup.toEntity(): SharedGroupEntity {
        val memberJsonList = members.map { m ->
            MemberJson(
                memberId = m.memberId,
                name = m.name,
                contact = m.contact,
                role = m.role.toFirestoreValue(),
                isSignedIn = m.isSignedIn,
                invitationStatus = m.invitationStatus.toFirestoreValue(),
                securityKey = m.securityKey,
                invitedAt = m.invitedAt?.time,
                acceptedAt = m.acceptedAt?.time
            )
        }
        return SharedGroupEntity(
            groupId = groupId,
            groupName = groupName,
            ownerUid = ownerUid,
            allowedUidJson = gson.toJson(allowedUid),
            membersJson = gson.toJson(memberJsonList),
            groupType = groupType.toFirestoreValue(),
            syncStatus = syncStatus.toFirestoreValue(),
            createdAt = createdAt.time,
            updatedAt = updatedAt?.time
        )
    }

    private fun SharedGroupEntity.toModel(): SharedGroup {
        val allowedUidType = object : TypeToken<List<String>>() {}.type
        val allowedUidList: List<String> = gson.fromJson(allowedUidJson, allowedUidType) ?: emptyList()
        val memberJsonType = object : TypeToken<List<MemberJson>>() {}.type
        val memberJsonList: List<MemberJson> = gson.fromJson(membersJson, memberJsonType) ?: emptyList()
        return SharedGroup(
            groupId = groupId,
            groupName = groupName,
            ownerUid = ownerUid,
            allowedUid = allowedUidList,
            members = memberJsonList.map { m ->
                SharedGroupMember(
                    memberId = m.memberId,
                    name = m.name,
                    contact = m.contact,
                    role = SharedGroupRole.fromFirestoreValue(m.role),
                    isSignedIn = m.isSignedIn,
                    invitationStatus = InvitationStatus.fromFirestoreValue(m.invitationStatus),
                    securityKey = m.securityKey,
                    invitedAt = m.invitedAt?.let { Date(it) },
                    acceptedAt = m.acceptedAt?.let { Date(it) }
                )
            },
            groupType = GroupType.fromFirestoreValue(groupType),
            syncStatus = SyncStatus.fromFirestoreValue(syncStatus),
            createdAt = Date(createdAt),
            updatedAt = updatedAt?.let { Date(it) }
        )
    }
}
