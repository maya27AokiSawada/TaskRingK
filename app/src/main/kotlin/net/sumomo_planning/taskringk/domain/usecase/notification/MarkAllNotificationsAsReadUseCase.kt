package net.sumomo_planning.taskringk.domain.usecase.notification

import javax.inject.Inject
import net.sumomo_planning.taskringk.domain.repository.NotificationRepository

class MarkAllNotificationsAsReadUseCase @Inject constructor(
    private val repository: NotificationRepository,
) {
    suspend operator fun invoke(userId: String) {
        repository.markAllAsRead(userId)
    }
}