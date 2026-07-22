package net.sumomo_planning.taskringk.data.remote.dto

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

@Keep
data class NotificationDto(
    @DocumentId val documentId: String = "",
    val notificationId: String = "",
    val userId: String = "",
    val type: String = "listCreated",
    val groupId: String = "",
    val listId: String? = null,
    val message: String = "",
    val metadata: Map<String, String> = emptyMap(),
    val isRead: Boolean = false,
    val createdAt: Timestamp? = null,
)