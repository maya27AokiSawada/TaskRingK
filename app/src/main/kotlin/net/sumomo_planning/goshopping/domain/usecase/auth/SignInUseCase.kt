package net.sumomo_planning.goshopping.domain.usecase.auth

import javax.inject.Inject
import net.sumomo_planning.goshopping.domain.model.AuthUser
import net.sumomo_planning.goshopping.domain.repository.AuthRepository

class SignInUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String, password: String): Result<AuthUser> =
        authRepository.signIn(email, password)
}
