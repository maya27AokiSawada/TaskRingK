package net.sumomo_planning.taskringk.domain.usecase.whiteboard

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import net.sumomo_planning.taskringk.domain.model.Whiteboard
import net.sumomo_planning.taskringk.domain.repository.WhiteboardRepository

class ObserveWhiteboardsByGroupUseCase @Inject constructor(
    private val repository: WhiteboardRepository,
) {
    operator fun invoke(groupId: String): Flow<List<Whiteboard>> = repository.observeByGroup(groupId)
}