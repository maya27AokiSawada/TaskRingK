package net.sumomo_planning.taskringk.domain.usecase.whiteboard

import javax.inject.Inject
import net.sumomo_planning.taskringk.domain.model.DrawingStroke
import net.sumomo_planning.taskringk.domain.repository.WhiteboardRepository

class UpsertWhiteboardStrokeUseCase @Inject constructor(
    private val repository: WhiteboardRepository,
) {
    suspend operator fun invoke(
        groupId: String,
        whiteboardId: String,
        stroke: DrawingStroke,
    ): Result<Unit> = repository.upsertStroke(groupId, whiteboardId, stroke)
}
