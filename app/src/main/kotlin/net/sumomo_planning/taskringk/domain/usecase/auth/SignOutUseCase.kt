package net.sumomo_planning.taskringk.domain.usecase.auth

import javax.inject.Inject
import net.sumomo_planning.taskringk.domain.repository.AuthRepository

class SignOutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): Result<Unit> = authRepository.signOut()
}
