package net.sumomo_planning.taskringk.core.common

import java.time.Instant
import java.util.UUID
import net.sumomo_planning.taskringk.domain.model.Notification
import net.sumomo_planning.taskringk.domain.model.NotificationType

object NotificationFactory {
    fun create(
        userId: String,
        type: NotificationType,
        groupId: String,
        listId: String? = null,
        message: String,
        metadata: Map<String, String> = emptyMap(),
    ): Notification = Notification(
        notificationId = UUID.randomUUID().toString(),
        userId = userId,
        type = type,
        groupId = groupId,
        listId = listId,
        message = message,
        metadata = metadata,
        isRead = false,
        createdAt = Instant.now(),
    )
}