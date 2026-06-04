package com.goshopping.android.data.repository

import com.goshopping.android.data.model.SharedGroup
import com.goshopping.android.data.model.SharedGroupMember

interface SharedGroupRepository {
    suspend fun createGroup(
        groupId: String,
        groupName: String,
        member: SharedGroupMember
    ): SharedGroup

    suspend fun getAllGroups(): List<SharedGroup>

    suspend fun getGroupById(groupId: String): SharedGroup

    suspend fun updateGroup(groupId: String, group: SharedGroup): SharedGroup

    suspend fun deleteGroup(groupId: String): SharedGroup

    suspend fun addMember(groupId: String, member: SharedGroupMember): SharedGroup

    suspend fun removeMember(groupId: String, member: SharedGroupMember): SharedGroup
}
