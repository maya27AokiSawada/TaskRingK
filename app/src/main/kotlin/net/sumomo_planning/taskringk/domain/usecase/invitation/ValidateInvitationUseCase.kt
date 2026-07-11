package net.sumomo_planning.taskringk.domain.usecase.invitation

import javax.inject.Inject
import net.sumomo_planning.taskringk.domain.model.Invitation
import net.sumomo_planning.taskringk.domain.repository.InvitationRepository

class ValidateInvitationUseCase @Inject constructor(
    private val repository: InvitationRepository,
) {
    suspend operator fun invoke(groupId: String, token: String): Result<Invitation> =
        repository.validateInvitation(groupId, token)
}