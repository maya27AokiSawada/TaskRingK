package net.sumomo_planning.taskringk.data.remote.dto

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Firestore DTO for the SharedGroup document at /SharedGroups/{groupId}.
 *
 * All fields have defaults so Firestore deserialization never throws on absent fields.
 */
@Keep
data class SharedGroupDto(
    @DocumentId val documentId: String = "",
    val groupId: String = "",
    val groupName: String = "",
    val ownerUid: String = "",
    val allowedUid: List<String> = emptyList(),
    val members: List<SharedGroupMemberDto> = emptyList(),
    val groupType: String = "shopping",
    val syncStatus: String = "synced",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
)

/**
 * Firestore DTO for a member embedded in the SharedGroup document.
 */
@Keep
data class SharedGroupMemberDto(
    val memberId: String = "",
    val name: String = "",
    val contact: String = "",
    val role: String = "member",
    val isSignedIn: Boolean = false,
    val invitationStatus: String = "pending",
    val securityKey: String? = null,
    val invitedAt: Timestamp? = null,
    val acceptedAt: Timestamp? = null,
)
