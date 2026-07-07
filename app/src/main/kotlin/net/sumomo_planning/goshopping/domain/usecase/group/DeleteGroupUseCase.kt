package net.sumomo_planning.goshopping.domain.usecase.group

import javax.inject.Inject
import net.sumomo_planning.goshopping.domain.repository.SharedGroupRepository

class DeleteGroupUseCase @Inject constructor(
    private val repository: SharedGroupRepository,
) {
    suspend operator fun invoke(groupId: String): Result<Unit> = repository.deleteGroup(groupId)
}
