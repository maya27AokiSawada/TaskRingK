package net.sumomo_planning.taskringk.domain.usecase.whiteboard

import javax.inject.Inject
import net.sumomo_planning.taskringk.domain.model.Whiteboard
import net.sumomo_planning.taskringk.domain.repository.WhiteboardRepository

class CreateWhiteboardUseCase @Inject constructor(
    private val repository: WhiteboardRepository,
) {
    suspend operator fun invoke(
        groupId: String,
        ownerId: String?,
        isPrivate: Boolean,
    ): Result<Whiteboard> = repository.createWhiteboard(
        groupId = groupId,
        ownerId = ownerId,
        isPrivate = isPrivate,
    )
}