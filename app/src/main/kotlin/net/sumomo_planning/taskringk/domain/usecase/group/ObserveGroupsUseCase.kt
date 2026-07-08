package net.sumomo_planning.taskringk.domain.usecase.group

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import net.sumomo_planning.taskringk.domain.model.SharedGroup
import net.sumomo_planning.taskringk.domain.repository.SharedGroupRepository

class ObserveGroupsUseCase @Inject constructor(
    private val repository: SharedGroupRepository,
) {
    operator fun invoke(uid: String): Flow<List<SharedGroup>> = repository.observeGroups(uid)
}
