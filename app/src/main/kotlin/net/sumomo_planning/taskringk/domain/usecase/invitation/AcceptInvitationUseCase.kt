package net.sumomo_planning.taskringk.domain.usecase.invitation

import javax.inject.Inject
import net.sumomo_planning.taskringk.domain.model.AcceptedInvitation
import net.sumomo_planning.taskringk.domain.repository.InvitationRepository

class AcceptInvitationUseCase @Inject constructor(
    private val repository: InvitationRepository,
) {
    suspend operator fun invoke(
        groupId: String,
        token: String,
        acceptorUid: String,
        acceptorEmail: String,
        acceptorName: String,
    ): Result<AcceptedInvitation> = repository.acceptInvitation(
        groupId = groupId,
        token = token,
        acceptorUid = acceptorUid,
        acceptorEmail = acceptorEmail,
        acceptorName = acceptorName,
    )
}