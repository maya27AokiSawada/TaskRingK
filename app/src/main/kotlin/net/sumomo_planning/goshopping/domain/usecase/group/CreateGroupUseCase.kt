package net.sumomo_planning.goshopping.domain.usecase.group

import javax.inject.Inject
import net.sumomo_planning.goshopping.domain.model.SharedGroup
import net.sumomo_planning.goshopping.domain.repository.SharedGroupRepository

class CreateGroupUseCase @Inject constructor(
    private val repository: SharedGroupRepository,
) {
    suspend operator fun invoke(
        groupName: String,
        ownerUid: String,
        ownerDisplayName: String,
        ownerEmail: String,
    ): Result<SharedGroup> = repository.createGroup(groupName, ownerUid, ownerDisplayName, ownerEmail)
}
