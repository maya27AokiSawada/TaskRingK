package net.sumomo_planning.taskringk.domain.usecase.invitation

import javax.inject.Inject
import net.sumomo_planning.taskringk.domain.model.Notification
import net.sumomo_planning.taskringk.domain.repository.InvitationRepository

class ProcessInvitationAcceptedNotificationUseCase @Inject constructor(
    private val repository: InvitationRepository,
) {
    suspend operator fun invoke(notification: Notification): Result<Unit> =
        repository.processInvitationAcceptedNotification(notification)
}