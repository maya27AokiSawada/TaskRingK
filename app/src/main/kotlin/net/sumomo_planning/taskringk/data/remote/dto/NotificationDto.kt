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
    // Flat fields for Flutter compatibility
    val invitationId: String? = null,
    val acceptorUid: String? = null,
    val acceptorName: String? = null,
    val acceptorEmail: String? = null,
    val isRead: Boolean = false,
    val createdAt: Timestamp? = null,
)