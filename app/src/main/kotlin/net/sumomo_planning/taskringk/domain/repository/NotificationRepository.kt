package net.sumomo_planning.taskringk.domain.repository

import kotlinx.coroutines.flow.Flow
import net.sumomo_planning.taskringk.domain.model.Notification

interface NotificationRepository {
    fun observeUnreadNotifications(userId: String): Flow<List<Notification>>

    suspend fun createNotifications(notifications: List<Notification>)

    suspend fun markAsRead(notificationId: String)

    suspend fun markAllAsRead(userId: String)
}