package net.sumomo_planning.taskringk.domain.model

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Data class representing the payload encoded within the QR code.
 * Based on the v3.1 specification.
 */
@Serializable
data class QrInvitationData(
    val invitationId: String,
    val inviterUid: String,
    val inviterEmail: String? = null,
    val inviterDisplayName: String? = null,
    val sharedGroupId: String,
    val groupName: String? = null,
    val groupOwnerUid: String? = null,
    val invitationType: String = "individual",
    val securityKey: String,
    val type: String = "secure_qr_invitation",
    val version: String = "3.1"
)

/**
 * Data class representing the invitation document stored in Firestore.
 * This contains all the details of the invitation and its state.
 */
@Serializable
data class QrInvitationDocument(
    val invitationId: String,
    val inviterUid: String,
    val inviterEmail: String? = null,
    val inviterDisplayName: String? = null,
    val sharedGroupId: String,
    val groupName: String? = null,
    val groupOwnerUid: String? = null,
    val invitationType: String = "individual",
    val inviteRole: String = "member",
    val message: String? = null,
    val securityKey: String,
    val invitationToken: String,
    val type: String = "secure_qr_invitation",
    val version: String = "3.1",
    val status: String = "pending", // pending | accepted | used | expired | revoked
    val maxUses: Int = 5,
    val currentUses: Int = 0,
    val usedBy: List<String> = emptyList(),
    val createdAt: Long = Instant.now().toEpochMilli(),
    val expiresAt: Long = Instant.now().plusSeconds(24 * 3600).toEpochMilli(),
    val acceptedAt: Long? = null,
    val lastUsedAt: Long? = null,
    val acceptorUid: String? = null
)
