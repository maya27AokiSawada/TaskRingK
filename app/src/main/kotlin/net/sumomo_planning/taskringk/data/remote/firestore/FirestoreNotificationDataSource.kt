package net.sumomo_planning.taskringk.data.remote.firestore

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import net.sumomo_planning.taskringk.data.mapper.toDomain
import net.sumomo_planning.taskringk.data.mapper.toFirestoreMap
import net.sumomo_planning.taskringk.data.remote.dto.NotificationDto
import net.sumomo_planning.taskringk.domain.model.Notification

@Singleton
class FirestoreNotificationDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    private val collection get() = firestore.collection("Notifications")

    fun observeUnreadNotifications(userId: String): Flow<List<Notification>> = callbackFlow {
        val listener = collection
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val notifications = snapshot?.documents?.mapNotNull { doc ->
                    runCatching { doc.toObject(NotificationDto::class.java)?.toDomain() }
                        .onFailure { Log.w(TAG, "Failed to deserialize notification ${doc.id}", it) }
                        .getOrNull()
                } ?: emptyList()
                trySend(notifications)
            }
        awaitClose { listener.remove() }
    }

    suspend fun createNotifications(notifications: List<Notification>) {
        notifications.forEach { notification ->
            collection.document(notification.notificationId)
                .set(notification.toFirestoreMap())
                .await()
        }
    }

    suspend fun markAsRead(notificationId: String) {
        collection.document(notificationId)
            .update("isRead", true)
            .await()
    }

    suspend fun markAllAsRead(userId: String) {
        val snapshot = collection
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .get()
            .await()
        snapshot.documents.forEach { document ->
            document.reference.update("isRead", true).await()
        }
    }

    private companion object {
        const val TAG = "FirestoreNotificationDS"
    }
}