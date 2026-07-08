package net.sumomo_planning.taskringk.domain.usecase.list

import javax.inject.Inject
import net.sumomo_planning.taskringk.domain.model.SharedList
import net.sumomo_planning.taskringk.domain.repository.SharedListRepository

class CreateListUseCase @Inject constructor(
    private val repository: SharedListRepository,
) {
    suspend operator fun invoke(
        groupId: String,
        groupName: String,
        listName: String,
        description: String = "",
        ownerUid: String,
    ): Result<SharedList> = repository.createList(
        groupId = groupId,
        groupName = groupName,
        listName = listName,
        description = description,
        ownerUid = ownerUid,
    )
}
