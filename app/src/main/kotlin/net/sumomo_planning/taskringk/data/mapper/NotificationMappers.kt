package net.sumomo_planning.taskringk.data.mapper

import com.google.firebase.Timestamp
import java.time.Instant
import net.sumomo_planning.taskringk.data.remote.dto.NotificationDto
import net.sumomo_planning.taskringk.domain.model.Notification

fun NotificationDto.toDomain(): Notification = Notification(
    notificationId = notificationId.ifBlank { documentId },
    userId = userId,
    type = type.toNotificationType(),
    groupId = groupId,
    listId = listId,
    message = message,
    metadata = metadata,
    isRead = isRead,
    createdAt = createdAt?.toDate()?.toInstant() ?: Instant.now(),
)

fun Notification.toFirestoreMap(): Map<String, Any?> = mapOf(
    "notificationId" to notificationId,
    "userId" to userId,
    "type" to type.storageValue,
    "groupId" to groupId,
    "listId" to listId,
    "message" to message,
    "metadata" to metadata,
    "isRead" to isRead,
    "createdAt" to Timestamp(createdAt.epochSecond, createdAt.nano),
)