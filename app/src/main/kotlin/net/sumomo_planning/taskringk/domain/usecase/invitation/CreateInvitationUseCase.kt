package net.sumomo_planning.taskringk.domain.usecase.invitation

import javax.inject.Inject
import net.sumomo_planning.taskringk.domain.model.Invitation
import net.sumomo_planning.taskringk.domain.model.SharedGroup
import net.sumomo_planning.taskringk.domain.repository.InvitationRepository

class CreateInvitationUseCase @Inject constructor(
    private val repository: InvitationRepository,
) {
    suspend operator fun invoke(
        group: SharedGroup,
        invitedBy: String,
        inviterName: String,
    ): Result<Invitation> = repository.createInvitation(group, invitedBy, inviterName)
}