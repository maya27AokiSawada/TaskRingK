package net.sumomo_planning.taskringk.domain.model

import java.time.Instant

data class Notification(
    val notificationId: String,
    val userId: String,
    val type: NotificationType,
    val groupId: String,
    val listId: String? = null,
    val message: String,
    val isRead: Boolean = false,
    val createdAt: Instant,
)

enum class NotificationType {
    LIST_CREATED,
    LIST_DELETED,
    LIST_RENAMED,
    MEMBER_JOINED,
    MEMBER_LEFT,
    GROUP_DELETED,
}
