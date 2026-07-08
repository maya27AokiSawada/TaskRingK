package net.sumomo_planning.taskringk.domain.usecase.list

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import net.sumomo_planning.taskringk.domain.model.SharedList
import net.sumomo_planning.taskringk.domain.repository.SharedListRepository

class ObserveListsByGroupUseCase @Inject constructor(
    private val repository: SharedListRepository,
) {
    operator fun invoke(groupId: String): Flow<List<SharedList>> =
        repository.observeListsByGroup(groupId)
}
