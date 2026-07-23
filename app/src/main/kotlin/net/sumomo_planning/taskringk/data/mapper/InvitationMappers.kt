package net.sumomo_planning.taskringk.data.mapper

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import java.time.Instant
import net.sumomo_planning.taskringk.data.remote.dto.InvitationDto
import net.sumomo_planning.taskringk.domain.model.AcceptedInvitation
import net.sumomo_planning.taskringk.domain.model.Invitation

fun InvitationDto.toDomain(): Invitation = Invitation(
    token = token,
    groupId = groupId,
    groupName = groupName,
    invitedBy = invitedBy,
    inviterName = inviterName,
    createdAt = createdAt?.toDate()?.toInstant() ?: Instant.now(),
    expiresAt = expiresAt?.toDate()?.toInstant() ?: Instant.now(),
    maxUses = maxUses,
    currentUses = currentUses,
    usedBy = usedBy,
    securityKey = securityKey,
)

fun Invitation.toFirestoreMap(): Map<String, Any?> = mapOf(
    "token" to token,
    "invitationId" to token,
    "groupId" to groupId,
    "sharedGroupId" to groupId,
    "groupName" to groupName,
    "invitedBy" to invitedBy,
    "inviterUid" to invitedBy,
    "inviterName" to inviterName,
    "inviterDisplayName" to inviterName,
    "createdAt" to FieldValue.serverTimestamp(),
    "expiresAt" to Timestamp(expiresAt.epochSecond, expiresAt.nano),
    "maxUses" to maxUses,
    "currentUses" to currentUses,
    "usedBy" to usedBy,
    "securityKey" to securityKey,
    "invitationType" to "individual",
    "inviteRole" to "member",
    "status" to "pending",
    "type" to "secure_qr_invitation",
    "version" to "3.1",
)

fun AcceptedInvitation.toFirestoreMap(): Map<String, Any?> = mapOf(
    "acceptorUid" to acceptorUid,
    "acceptorEmail" to acceptorEmail,
    "acceptorName" to acceptorName,
    "groupId" to groupId,
    "sharedGroupId" to groupId,
    "invitationId" to invitationId,
    "token" to invitationId,
    "listId" to listId,
    "role" to role.name,
    "acceptedAt" to Timestamp(acceptedAt.epochSecond, acceptedAt.nano),
    "processedAt" to processedAt?.let { Timestamp(it.epochSecond, it.nano) },
)
