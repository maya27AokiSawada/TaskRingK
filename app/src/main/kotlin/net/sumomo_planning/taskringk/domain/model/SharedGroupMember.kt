package net.sumomo_planning.taskringk.domain.model

import java.time.Instant

data class SharedGroupMember(
    val memberId: String,
    val name: String,
    val contact: String,
    val role: SharedGroupRole,
    val isSignedIn: Boolean = false,
    val invitationStatus: InvitationStatus = InvitationStatus.PENDING,
    val securityKey: String? = null,
    val invitedAt: Instant? = null,
    val acceptedAt: Instant? = null,
)

enum class SharedGroupRole { OWNER, MEMBER, MANAGER, PARTNER }

enum class InvitationStatus { SELF, PENDING, ACCEPTED, DELETED }
