package net.sumomo_planning.taskringk.data.repository

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import net.sumomo_planning.taskringk.data.remote.firestore.FirestoreNotificationDataSource
import net.sumomo_planning.taskringk.domain.model.Notification
import net.sumomo_planning.taskringk.domain.repository.NotificationRepository

@Singleton
class HybridNotificationRepositoryImpl @Inject constructor(
    private val firestoreNotificationDataSource: FirestoreNotificationDataSource,
) : NotificationRepository {
    override fun observeUnreadNotifications(userId: String): Flow<List<Notification>> =
        firestoreNotificationDataSource.observeUnreadNotifications(userId)
            .catch { error ->
                Log.w(TAG, "Firestore notification stream failed", error)
                emit(emptyList())
            }

    override suspend fun createNotifications(notifications: List<Notification>) {
        runCatching { firestoreNotificationDataSource.createNotifications(notifications) }
            .onFailure { Log.w(TAG, "Firestore createNotifications failed", it) }
    }

    override suspend fun markAsRead(notificationId: String) {
        runCatching { firestoreNotificationDataSource.markAsRead(notificationId) }
            .onFailure { Log.w(TAG, "Firestore markAsRead failed", it) }
    }

    override suspend fun markAllAsRead(userId: String) {
        runCatching { firestoreNotificationDataSource.markAllAsRead(userId) }
            .onFailure { Log.w(TAG, "Firestore markAllAsRead failed", it) }
    }

    private companion object {
        const val TAG = "HybridNotificationRepo"
    }
}