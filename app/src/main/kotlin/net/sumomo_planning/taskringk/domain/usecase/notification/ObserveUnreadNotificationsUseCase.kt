package net.sumomo_planning.taskringk.domain.usecase.notification

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import net.sumomo_planning.taskringk.domain.model.Notification
import net.sumomo_planning.taskringk.domain.repository.NotificationRepository

class ObserveUnreadNotificationsUseCase @Inject constructor(
    private val repository: NotificationRepository,
) {
    operator fun invoke(userId: String): Flow<List<Notification>> =
        repository.observeUnreadNotifications(userId)
}