package com.goshopping.android.data.repository.firestore

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.goshopping.android.data.model.SharedGroup
import com.goshopping.android.data.model.SharedGroupMember
import com.goshopping.android.data.model.SyncStatus
import com.goshopping.android.data.repository.SharedGroupRepository
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirestoreSharedGroupRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : SharedGroupRepository {

    private val groups get() = firestore.collection("SharedGroups")

    override suspend fun createGroup(
        groupId: String,
        groupName: String,
        member: SharedGroupMember
    ): SharedGroup {
        val ownerUid = requireCurrentUid()
        val group = SharedGroup(
            groupId = groupId,
            groupName = groupName,
            ownerUid = ownerUid,
            allowedUid = listOf(ownerUid),
            members = listOf(member),
            syncStatus = SyncStatus.SYNCED,
            createdAt = Date()
        )
        val data = group.toMap().toMutableMap<String, Any?>()
        data["createdAt"] = FieldValue.serverTimestamp()
        groups.document(groupId).set(data).await()
        return group
    }

    override suspend fun getAllGroups(): List<SharedGroup> {
        val currentUid = requireCurrentUid()
        val snapshot = groups
            .whereArrayContains("allowedUid", currentUid)
            .get()
            .await()
        return snapshot.documents.mapNotNull { doc ->
            doc.data?.let { SharedGroup.fromMap(it) }
        }
    }

    override suspend fun getGroupById(groupId: String): SharedGroup {
        val doc = groups.document(groupId).get().await()
        return doc.data?.let { SharedGroup.fromMap(it) }
            ?: throw NoSuchElementException("Group $groupId not found")
    }

    override suspend fun updateGroup(groupId: String, group: SharedGroup): SharedGroup {
        val updates = group.toMap().toMutableMap<String, Any?>()
        updates["updatedAt"] = FieldValue.serverTimestamp()
        groups.document(groupId).update(updates).await()
        return group
    }

    override suspend fun deleteGroup(groupId: String): SharedGroup {
        val group = getGroupById(groupId)
        groups.document(groupId).delete().await()
        return group
    }

    override suspend fun addMember(groupId: String, member: SharedGroupMember): SharedGroup {
        val uid = requireCurrentUid()
        groups.document(groupId).update(
            mapOf(
                "members" to FieldValue.arrayUnion(member.toMap()),
                "allowedUid" to FieldValue.arrayUnion(uid),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        return getGroupById(groupId)
    }

    override suspend fun removeMember(groupId: String, member: SharedGroupMember): SharedGroup {
        val uid = requireCurrentUid()
        groups.document(groupId).update(
            mapOf(
                "members" to FieldValue.arrayRemove(member.toMap()),
                "allowedUid" to FieldValue.arrayRemove(uid),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        return getGroupById(groupId)
    }

    private fun requireCurrentUid(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User is not authenticated")
}
