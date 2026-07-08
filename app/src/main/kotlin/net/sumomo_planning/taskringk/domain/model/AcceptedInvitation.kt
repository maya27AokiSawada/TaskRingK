package net.sumomo_planning.taskringk.domain.model

import java.time.Instant

data class AcceptedInvitation(
    val acceptorUid: String,
    val acceptorEmail: String,
    val acceptorName: String,
    val groupId: String,
    val listId: String? = null,
    val role: SharedGroupRole,
    val acceptedAt: Instant,
    val processedAt: Instant? = null,
)
