package net.sumomo_planning.taskringk.data.remote.firestore

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await
import net.sumomo_planning.taskringk.data.mapper.toDomain
import net.sumomo_planning.taskringk.data.mapper.toFirestoreMap
import net.sumomo_planning.taskringk.data.mapper.toMemberMap
import net.sumomo_planning.taskringk.data.remote.dto.InvitationDto
import net.sumomo_planning.taskringk.domain.model.AcceptedInvitation
import net.sumomo_planning.taskringk.domain.model.Invitation
import net.sumomo_planning.taskringk.domain.model.SharedGroup
import net.sumomo_planning.taskringk.domain.model.SharedGroupMember
import net.sumomo_planning.taskringk.domain.model.SharedGroupRole

@Singleton
class FirestoreInvitationDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    private val groupsCollection get() = firestore.collection("SharedGroups")

    suspend fun createInvitation(
        group: SharedGroup,
        invitedBy: String,
        inviterName: String,
        token: String,
        securityKey: String? = null,
    ): Invitation {
        val invitation = Invitation(
            token = token,
            groupId = group.groupId,
            groupName = group.groupName,
            invitedBy = invitedBy,
            inviterName = inviterName,
            createdAt = java.time.Instant.now(),
            expiresAt = java.time.Instant.now().plusSeconds(24 * 60 * 60),
            securityKey = securityKey,
        )

        invitationDocument(group.groupId, token)
            .set(invitation.toFirestoreMap())
            .await()

        return invitation
    }

    suspend fun validateInvitation(groupId: String, token: String): Invitation {
        val snapshot = invitationDocument(groupId, token).get().await()
        val dto = snapshot.toObject(InvitationDto::class.java)
            ?: throw IllegalStateException("Invitation not found")
        val invitation = dto.toDomain()
        if (!invitation.isValidAt(java.time.Instant.now())) {
            throw IllegalStateException("Invitation is expired or has reached the usage limit")
        }
        return invitation
    }

    suspend fun acceptInvitation(
        invitation: Invitation,
        acceptorUid: String,
        acceptorEmail: String,
        acceptorName: String,
    ): AcceptedInvitation {
        val groupRef = groupsCollection.document(invitation.groupId)

        val now = java.time.Instant.now()
        val member = SharedGroupMember(
            memberId = acceptorUid,
            name = acceptorName,
            contact = acceptorEmail,
            role = SharedGroupRole.MEMBER,
            isSignedIn = true,
            invitedAt = invitation.createdAt,
            acceptedAt = now,
            securityKey = invitation.securityKey,
        )

        groupRef.update(
            mapOf(
                "allowedUid" to FieldValue.arrayUnion(acceptorUid),
                "members" to FieldValue.arrayUnion(member.toMemberMap()),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        ).await()

        invitationDocument(invitation.groupId, invitation.token)
            .update(
                mapOf(
                    "currentUses" to FieldValue.increment(1),
                    "usedBy" to FieldValue.arrayUnion(acceptorUid),
                )
            )
            .await()

        return AcceptedInvitation(
            acceptorUid = acceptorUid,
            acceptorEmail = acceptorEmail,
            acceptorName = acceptorName,
            groupId = invitation.groupId,
            role = SharedGroupRole.MEMBER,
            acceptedAt = now,
            processedAt = now,
        )
    }

    private fun invitationDocument(groupId: String, token: String) =
        groupsCollection.document(groupId).collection("invitations").document(token)
}