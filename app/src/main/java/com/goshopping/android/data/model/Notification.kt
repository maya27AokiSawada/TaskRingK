package com.goshopping.android.data.model

import com.google.firebase.Timestamp
import java.util.Date

enum class NotificationType {
    LIST_CREATED, LIST_DELETED, LIST_RENAMED,
    MEMBER_JOINED, MEMBER_LEFT, GROUP_DELETED;

    fun toFirestoreValue(): String = when (this) {
        LIST_CREATED -> "listCreated"
        LIST_DELETED -> "listDeleted"
        LIST_RENAMED -> "listRenamed"
        MEMBER_JOINED -> "memberJoined"
        MEMBER_LEFT -> "memberLeft"
        GROUP_DELETED -> "groupDeleted"
    }

    companion object {
        fun fromFirestoreValue(value: String): NotificationType = when (value) {
            "listCreated" -> LIST_CREATED
            "listDeleted" -> LIST_DELETED
            "listRenamed" -> LIST_RENAMED
            "memberJoined" -> MEMBER_JOINED
            "memberLeft" -> MEMBER_LEFT
            "groupDeleted" -> GROUP_DELETED
            else -> LIST_CREATED
        }
    }
}

data class AppNotification(
    val notificationId: String = "",
    val userId: String = "",
    val type: NotificationType = NotificationType.LIST_CREATED,
    val groupId: String = "",
    val listId: String? = null,
    val message: String = "",
    val isRead: Boolean = false,
    val createdAt: Date = Date()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "type" to type.toFirestoreValue(),
        "groupId" to groupId,
        "listId" to listId,
        "message" to message,
        "isRead" to isRead,
        "createdAt" to createdAt
    )

    companion object {
        fun fromMap(map: Map<String, Any?>, id: String = ""): AppNotification = AppNotification(
            notificationId = id,
            userId = map["userId"] as? String ?: "",
            type = NotificationType.fromFirestoreValue(map["type"] as? String ?: "listCreated"),
            groupId = map["groupId"] as? String ?: "",
            listId = map["listId"] as? String,
            message = map["message"] as? String ?: "",
            isRead = map["isRead"] as? Boolean ?: false,
            createdAt = (map["createdAt"] as? Timestamp)?.toDate() ?: Date()
        )
    }
}
