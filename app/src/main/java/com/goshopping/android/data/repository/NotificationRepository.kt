package com.goshopping.android.data.repository

import com.goshopping.android.data.model.AppNotification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    suspend fun sendNotification(notification: AppNotification)

    suspend fun markAsRead(notificationId: String)

    /** 自分宛の未読通知をリアルタイムで監視する Flow を返す */
    fun watchNotifications(userId: String): Flow<List<AppNotification>>
}
