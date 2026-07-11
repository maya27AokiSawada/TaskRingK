package net.sumomo_planning.taskringk.domain.usecase.whiteboard

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import net.sumomo_planning.taskringk.domain.model.DrawingStroke
import net.sumomo_planning.taskringk.domain.repository.WhiteboardRepository

class ObserveWhiteboardStrokesUseCase @Inject constructor(
    private val repository: WhiteboardRepository,
) {
    operator fun invoke(groupId: String, whiteboardId: String): Flow<List<DrawingStroke>> =
        repository.observeStrokes(groupId, whiteboardId)
}
