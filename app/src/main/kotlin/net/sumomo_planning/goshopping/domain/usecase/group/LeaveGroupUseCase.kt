package net.sumomo_planning.goshopping.domain.usecase.group

import javax.inject.Inject
import net.sumomo_planning.goshopping.domain.repository.SharedGroupRepository

class LeaveGroupUseCase @Inject constructor(
    private val repository: SharedGroupRepository,
) {
    suspend operator fun invoke(
        groupId: String,
        uid: String,
        memberId: String,
    ): Result<Unit> = repository.leaveGroup(groupId, uid, memberId)
}
