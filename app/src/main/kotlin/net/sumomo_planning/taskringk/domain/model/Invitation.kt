package net.sumomo_planning.taskringk.domain.model

import java.time.Instant

data class Invitation(
    val token: String,
    val groupId: String,
    val groupName: String,
    val invitedBy: String,
    val inviterName: String,
    val createdAt: Instant,
    val expiresAt: Instant,
    val maxUses: Int = 5,
    val currentUses: Int = 0,
    val usedBy: List<String> = emptyList(),
    val securityKey: String? = null,
) {
    fun isValidAt(now: Instant): Boolean = now.isBefore(expiresAt) && currentUses < maxUses
}
