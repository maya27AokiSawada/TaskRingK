package com.goshopping.android.data.repository.firestore

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.goshopping.android.data.model.AppNotification
import com.goshopping.android.data.repository.NotificationRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreNotificationRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : NotificationRepository {

    private val notifications get() = firestore.collection("notifications")

    override suspend fun sendNotification(notification: AppNotification) {
        val id = notification.notificationId.ifEmpty { UUID.randomUUID().toString() }
        val data = notification.toMap().toMutableMap<String, Any?>()
        data["createdAt"] = FieldValue.serverTimestamp()
        notifications.document(id).set(data).await()
    }

    override suspend fun markAsRead(notificationId: String) {
        notifications.document(notificationId)
            .update("isRead", true)
            .await()
    }

    override fun watchNotifications(userId: String): Flow<List<AppNotification>> =
        callbackFlow {
            val listener = notifications
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val list = snapshot?.documents?.mapNotNull { doc ->
                        doc.data?.let { AppNotification.fromMap(it, doc.id) }
                    } ?: emptyList()
                    trySend(list)
                }
            awaitClose { listener.remove() }
        }
}
