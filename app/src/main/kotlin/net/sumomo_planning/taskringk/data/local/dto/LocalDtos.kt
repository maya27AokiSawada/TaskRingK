package net.sumomo_planning.taskringk.data.local.dto

import kotlinx.serialization.Serializable

@Serializable
data class LocalSharedGroupMemberDto(
    val memberId: String,
    val name: String,
    val contact: String,
    val role: String,
    val isSignedIn: Boolean,
    val invitationStatus: String,
    val securityKey: String?,
    val invitedAt: Long?,
    val acceptedAt: Long?,
)

@Serializable
data class LocalSharedItemDto(
    val itemId: String,
    val name: String,
    val quantity: Int,
    val memberId: String,
    val registeredDate: Long,
    val purchaseDate: Long?,
    val isPurchased: Boolean,
    val isDeleted: Boolean,
    val deletedAt: Long?,
    val shoppingInterval: Int,
    val deadline: Long?,
)
