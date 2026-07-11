package net.sumomo_planning.taskringk.domain.usecase.notification

import javax.inject.Inject
import net.sumomo_planning.taskringk.domain.repository.NotificationRepository

class MarkNotificationAsReadUseCase @Inject constructor(
    private val repository: NotificationRepository,
) {
    suspend operator fun invoke(notificationId: String) {
        repository.markAsRead(notificationId)
    }
}