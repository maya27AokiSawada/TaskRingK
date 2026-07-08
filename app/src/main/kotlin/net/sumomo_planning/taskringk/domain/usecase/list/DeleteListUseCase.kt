package net.sumomo_planning.taskringk.domain.usecase.list

import javax.inject.Inject
import net.sumomo_planning.taskringk.domain.repository.SharedListRepository

class DeleteListUseCase @Inject constructor(
    private val repository: SharedListRepository,
) {
    suspend operator fun invoke(groupId: String, listId: String): Result<Unit> =
        repository.deleteList(groupId, listId)
}
