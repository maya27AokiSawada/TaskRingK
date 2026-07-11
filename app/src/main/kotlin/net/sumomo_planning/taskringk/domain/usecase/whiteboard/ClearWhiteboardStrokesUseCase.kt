package net.sumomo_planning.taskringk.domain.usecase.whiteboard

import javax.inject.Inject
import net.sumomo_planning.taskringk.domain.repository.WhiteboardRepository

class ClearWhiteboardStrokesUseCase @Inject constructor(
    private val repository: WhiteboardRepository,
) {
    suspend operator fun invoke(groupId: String, whiteboardId: String): Result<Unit> =
        repository.clearStrokes(groupId, whiteboardId)
}
