package com.goshopping.android.data.repository.firestore

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.goshopping.android.data.model.ListType
import com.goshopping.android.data.model.SharedItem
import com.goshopping.android.data.model.SharedList
import com.goshopping.android.data.repository.SharedListRepository
import com.goshopping.android.data.service.DeviceIdService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirestoreSharedListRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : SharedListRepository {

    private fun listsCollection(groupId: String) =
        firestore.collection("SharedGroups").document(groupId).collection("sharedLists")

    override suspend fun createSharedList(
        ownerUid: String,
        groupId: String,
        listName: String,
        description: String?,
        customListId: String?
    ): SharedList {
        val listId = customListId ?: DeviceIdService.generateListId(
            DeviceIdService.getDevicePrefix()
        )
        val list = SharedList(
            listId = listId,
            listName = listName,
            ownerUid = ownerUid,
            groupId = groupId,
            groupName = "",
            description = description ?: "",
            listType = ListType.SHOPPING,
            items = emptyMap(),
            createdAt = Date()
        )
        val data = list.toMap().toMutableMap<String, Any?>()
        data["createdAt"] = FieldValue.serverTimestamp()
        listsCollection(groupId).document(listId).set(data).await()
        return list
    }

    override suspend fun getSharedListById(groupId: String, listId: String): SharedList? {
        val doc = listsCollection(groupId).document(listId).get().await()
        return doc.data?.let { SharedList.fromMap(it) }
    }

    override suspend fun getSharedListsByGroup(groupId: String): List<SharedList> {
        val snapshot = listsCollection(groupId).get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.data?.let { SharedList.fromMap(it) }
        }
    }

    override suspend fun updateSharedList(list: SharedList) {
        val updates = list.toMap().toMutableMap<String, Any?>()
        updates["updatedAt"] = FieldValue.serverTimestamp()
        // items は差分更新で管理するため全件置換しない
        updates.remove("items")
        listsCollection(list.groupId).document(list.listId).update(updates).await()
    }

    override suspend fun deleteSharedList(groupId: String, listId: String) {
        listsCollection(groupId).document(listId).delete().await()
    }

    override suspend fun addSingleItem(groupId: String, listId: String, item: SharedItem) {
        listsCollection(groupId).document(listId).update(
            mapOf(
                "items.${item.itemId}" to item.toMap(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    override suspend fun updateSingleItem(groupId: String, listId: String, item: SharedItem) {
        listsCollection(groupId).document(listId).update(
            mapOf(
                "items.${item.itemId}" to item.toMap(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    override suspend fun removeSingleItem(groupId: String, listId: String, itemId: String) {
        val deletedAt = Date()
        listsCollection(groupId).document(listId).update(
            mapOf(
                "items.$itemId.isDeleted" to true,
                "items.$itemId.deletedAt" to deletedAt,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    override fun watchSharedList(groupId: String, listId: String): Flow<SharedList?> =
        callbackFlow {
            val docRef = listsCollection(groupId).document(listId)
            val listener = docRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.data?.let { SharedList.fromMap(it) }
                trySend(list)
            }
            awaitClose { listener.remove() }
        }
}
