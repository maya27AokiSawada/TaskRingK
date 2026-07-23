package net.sumomo_planning.taskringk.data.remote.firestore

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import net.sumomo_planning.taskringk.data.mapper.toDomain
import net.sumomo_planning.taskringk.data.mapper.toFirestoreMap
import net.sumomo_planning.taskringk.data.mapper.toMemberMap
import net.sumomo_planning.taskringk.data.mapper.toUpdateMap
import net.sumomo_planning.taskringk.data.remote.dto.InvitationDto
import net.sumomo_planning.taskringk.data.remote.dto.SharedGroupDto
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
    private val invitationRetryDelaysMs = listOf(250L, 350L, 500L, 700L, 1000L, 1400L, 1800L)

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
        val snapshot = readInvitationSnapshot(groupId, token)
        val dto = snapshot.toObject(InvitationDto::class.java)
            ?: throw IllegalStateException("Invitation not found")
        val invitation = dto.toDomain()
        if (!invitation.isValidAt(java.time.Instant.now())) {
            throw IllegalStateException("Invitation is expired or has reached the usage limit")
        }
        return invitation
    }

    suspend fun createInvitationAcceptedNotification(
        invitation: Invitation,
        acceptorUid: String,
        acceptorEmail: String,
        acceptorName: String,
    ): AcceptedInvitation {
        val now = java.time.Instant.now()
        return AcceptedInvitation(
            acceptorUid = acceptorUid,
            acceptorEmail = acceptorEmail,
            acceptorName = acceptorName,
            groupId = invitation.groupId,
            invitationId = invitation.token,
            role = SharedGroupRole.MEMBER,
            acceptedAt = now,
            processedAt = null,
        )
    }

    suspend fun processAcceptedInvitation(
        invitation: Invitation,
        acceptorUid: String,
        acceptorEmail: String,
        acceptorName: String,
    ): AcceptedInvitation {
        val groupRef = groupsCollection.document(invitation.groupId)
        val groupSnapshot = groupRef.get().await()
        val groupDto = groupSnapshot.toObject(SharedGroupDto::class.java)
            ?: throw IllegalStateException("Group ${invitation.groupId} not found")

        if (groupDto.allowedUid.contains(acceptorUid) || groupDto.members.any { it.memberId == acceptorUid }) {
            val now = java.time.Instant.now()
            return AcceptedInvitation(
                acceptorUid = acceptorUid,
                acceptorEmail = acceptorEmail,
                acceptorName = acceptorName,
                groupId = invitation.groupId,
                invitationId = invitation.token,
                role = SharedGroupRole.MEMBER,
                acceptedAt = now,
                processedAt = now,
            )
        }

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

        val updatedMembers = groupDto.members.map { it.toUpdateMap() } + member.toMemberMap()

        groupRef.update(
            mapOf(
                "allowedUid" to FieldValue.arrayUnion(acceptorUid),
                "members" to updatedMembers,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        ).await()

        if (!invitation.usedBy.contains(acceptorUid)) {
            invitationDocument(invitation.groupId, invitation.token)
                .update(
                    mapOf(
                        "currentUses" to FieldValue.increment(1),
                        "usedBy" to FieldValue.arrayUnion(acceptorUid),
                        "status" to "accepted",
                        "lastUsedAt" to FieldValue.serverTimestamp(),
                    )
                )
                .await()
        }

        return AcceptedInvitation(
            acceptorUid = acceptorUid,
            acceptorEmail = acceptorEmail,
            acceptorName = acceptorName,
            groupId = invitation.groupId,
            invitationId = invitation.token,
            role = SharedGroupRole.MEMBER,
            acceptedAt = now,
            processedAt = now,
        )
    }

    suspend fun getAllowedUids(groupId: String): List<String> {
        val groupDto = groupsCollection.document(groupId)
            .get()
            .await()
            .toObject(SharedGroupDto::class.java)
            ?: return emptyList()
        return groupDto.allowedUid
    }

    private suspend fun readInvitationSnapshot(
        groupId: String,
        token: String,
    ): com.google.firebase.firestore.DocumentSnapshot {
        val maxAttempts = invitationRetryDelaysMs.size + 1
        var lastError: Throwable? = null

        for (attempt in 1..maxAttempts) {
            try {
                val snapshot = invitationDocument(groupId, token).get().await()
                if (snapshot.exists()) {
                    return snapshot
                }
                lastError = IllegalStateException("Invitation not found")
            } catch (error: FirebaseFirestoreException) {
                val shouldRetry = error.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
                    error.code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ||
                    error.code == FirebaseFirestoreException.Code.ABORTED ||
                    error.code == FirebaseFirestoreException.Code.INTERNAL
                if (!shouldRetry) {
                    throw error
                }
                lastError = error
            }

            if (attempt < maxAttempts) {
                delay(invitationRetryDelaysMs[attempt - 1])
            }
        }

        throw lastError ?: IllegalStateException("Invitation not found")
    }

    private fun invitationDocument(groupId: String, token: String) =
        groupsCollection.document(groupId).collection("invitations").document(token)
}