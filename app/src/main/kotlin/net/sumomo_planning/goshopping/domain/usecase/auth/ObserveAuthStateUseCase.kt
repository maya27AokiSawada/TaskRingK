package net.sumomo_planning.goshopping.domain.usecase.auth

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import net.sumomo_planning.goshopping.domain.model.AuthUser
import net.sumomo_planning.goshopping.domain.repository.AuthRepository

class ObserveAuthStateUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    operator fun invoke(): Flow<AuthUser?> = authRepository.observeAuthState()
}
