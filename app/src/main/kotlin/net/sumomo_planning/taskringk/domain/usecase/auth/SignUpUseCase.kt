package net.sumomo_planning.taskringk.domain.usecase.auth

import javax.inject.Inject
import net.sumomo_planning.taskringk.domain.model.AuthUser
import net.sumomo_planning.taskringk.domain.repository.AuthRepository

class SignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        displayName: String,
    ): Result<AuthUser> = authRepository.signUp(email, password, displayName)
}
