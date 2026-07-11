package net.sumomo_planning.taskringk.domain.usecase.whiteboard

import javax.inject.Inject
import net.sumomo_planning.taskringk.domain.repository.WhiteboardRepository

class DeleteWhiteboardUseCase @Inject constructor(
    private val repository: WhiteboardRepository,
) {
    suspend operator fun invoke(whiteboardId: String): Result<Unit> =
        repository.deleteWhiteboard(whiteboardId)
}