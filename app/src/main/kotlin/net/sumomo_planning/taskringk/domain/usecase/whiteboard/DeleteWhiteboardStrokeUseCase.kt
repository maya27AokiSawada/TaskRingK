package net.sumomo_planning.taskringk.domain.usecase.whiteboard

import javax.inject.Inject
import net.sumomo_planning.taskringk.domain.repository.WhiteboardRepository

class DeleteWhiteboardStrokeUseCase @Inject constructor(
    private val repository: WhiteboardRepository,
) {
    suspend operator fun invoke(
        groupId: String,
        whiteboardId: String,
        strokeId: String,
    ): Result<Unit> = repository.deleteStroke(groupId, whiteboardId, strokeId)
}
