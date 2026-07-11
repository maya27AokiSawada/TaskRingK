package net.sumomo_planning.taskringk.data.remote.dto

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

@Keep
data class InvitationDto(
    @DocumentId val documentId: String = "",
    val token: String = "",
    val groupId: String = "",
    val groupName: String = "",
    val invitedBy: String = "",
    val inviterName: String = "",
    val createdAt: Timestamp? = null,
    val expiresAt: Timestamp? = null,
    val maxUses: Int = 5,
    val currentUses: Int = 0,
    val usedBy: List<String> = emptyList(),
    val securityKey: String? = null,
)