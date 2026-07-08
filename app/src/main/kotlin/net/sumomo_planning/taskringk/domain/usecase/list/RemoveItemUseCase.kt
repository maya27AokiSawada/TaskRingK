package net.sumomo_planning.taskringk.domain.usecase.list

import javax.inject.Inject
import net.sumomo_planning.taskringk.domain.repository.SharedListRepository

class RemoveItemUseCase @Inject constructor(
    private val repository: SharedListRepository,
) {
    suspend operator fun invoke(
        groupId: String,
        listId: String,
        itemId: String,
    ): Result<Unit> = repository.removeItem(groupId, listId, itemId)
}
