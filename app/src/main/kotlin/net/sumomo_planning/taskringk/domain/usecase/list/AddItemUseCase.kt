package net.sumomo_planning.taskringk.domain.usecase.list

import javax.inject.Inject
import net.sumomo_planning.taskringk.domain.model.SharedItem
import net.sumomo_planning.taskringk.domain.repository.SharedListRepository

class AddItemUseCase @Inject constructor(
    private val repository: SharedListRepository,
) {
    suspend operator fun invoke(
        groupId: String,
        listId: String,
        item: SharedItem,
    ): Result<Unit> = repository.addOrUpdateItem(groupId, listId, item)
}
