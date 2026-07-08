package net.sumomo_planning.taskringk.domain.usecase.list

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import net.sumomo_planning.taskringk.domain.model.SharedList
import net.sumomo_planning.taskringk.domain.repository.SharedListRepository

class ObserveListUseCase @Inject constructor(
    private val repository: SharedListRepository,
) {
    operator fun invoke(groupId: String, listId: String): Flow<SharedList?> =
        repository.observeList(groupId, listId)
}
