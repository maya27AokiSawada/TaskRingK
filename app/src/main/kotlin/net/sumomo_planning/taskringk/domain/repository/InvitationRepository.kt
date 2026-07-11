package net.sumomo_planning.taskringk.domain.repository

import net.sumomo_planning.taskringk.domain.model.AcceptedInvitation
import net.sumomo_planning.taskringk.domain.model.Invitation
import net.sumomo_planning.taskringk.domain.model.SharedGroup

interface InvitationRepository {
    suspend fun createInvitation(
        group: SharedGroup,
        invitedBy: String,
        inviterName: String,
    ): Result<Invitation>

    suspend fun validateInvitation(groupId: String, token: String): Result<Invitation>

    suspend fun acceptInvitation(
        groupId: String,
        token: String,
        acceptorUid: String,
        acceptorEmail: String,
        acceptorName: String,
    ): Result<AcceptedInvitation>
}