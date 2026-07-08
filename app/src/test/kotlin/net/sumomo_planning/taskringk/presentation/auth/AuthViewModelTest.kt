package net.sumomo_planning.taskringk.presentation.auth

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.sumomo_planning.taskringk.domain.model.AuthUser
import net.sumomo_planning.taskringk.domain.usecase.auth.ObserveAuthStateUseCase
import net.sumomo_planning.taskringk.domain.usecase.auth.SendPasswordResetEmailUseCase
import net.sumomo_planning.taskringk.domain.usecase.auth.SignInUseCase
import net.sumomo_planning.taskringk.domain.usecase.auth.SignOutUseCase
import net.sumomo_planning.taskringk.domain.usecase.auth.SignUpUseCase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val signUpUseCase = mockk<SignUpUseCase>()
    private val signInUseCase = mockk<SignInUseCase>()
    private val signOutUseCase = mockk<SignOutUseCase>()
    private val sendPasswordResetEmailUseCase = mockk<SendPasswordResetEmailUseCase>()
    private val observeAuthStateUseCase = mockk<ObserveAuthStateUseCase>()

    private val fakeUser = AuthUser(uid = "uid-1", email = "test@example.com", displayName = "Test")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { observeAuthStateUseCase() } returns flowOf(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = AuthViewModel(
        signUpUseCase = signUpUseCase,
        signInUseCase = signInUseCase,
        signOutUseCase = signOutUseCase,
        sendPasswordResetEmailUseCase = sendPasswordResetEmailUseCase,
        observeAuthStateUseCase = observeAuthStateUseCase,
    )

    @Test
    fun `initial state has no user and no errors`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        val state = vm.uiState.value
        assertNull(state.currentUser)
        assertNull(state.errorMessage)
    }

    @Test
    fun `signIn success clears loading and error`() = runTest {
        coEvery { signInUseCase(any(), any()) } returns Result.success(fakeUser)
        val vm = createViewModel()

        vm.signIn("test@example.com", "password123")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertNull(state.errorMessage)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `signIn failure sets errorMessage`() = runTest {
        coEvery { signInUseCase(any(), any()) } returns
            Result.failure(Exception("パスワードが間違っています"))
        val vm = createViewModel()

        vm.signIn("test@example.com", "wrong")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("パスワードが間違っています", vm.uiState.value.errorMessage)
    }

    @Test
    fun `signUp failure sets errorMessage`() = runTest {
        coEvery { signUpUseCase(any(), any(), any()) } returns
            Result.failure(Exception("このメールアドレスはすでに使用されています"))
        val vm = createViewModel()

        vm.signUp("dup@example.com", "password123", "Dup")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("このメールアドレスはすでに使用されています", vm.uiState.value.errorMessage)
    }

    @Test
    fun `sendPasswordResetEmail success sets successMessage`() = runTest {
        coEvery { sendPasswordResetEmailUseCase(any()) } returns Result.success(Unit)
        val vm = createViewModel()

        vm.sendPasswordResetEmail("test@example.com")
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(vm.uiState.value.successMessage)
    }

    @Test
    fun `observeAuthState updates currentUser in uiState`() = runTest {
        every { observeAuthStateUseCase() } returns flowOf(fakeUser)
        val vm = createViewModel()

        vm.uiState.test {
            val initial = awaitItem()  // may still be null at first emission
            testDispatcher.scheduler.advanceUntilIdle()
            // After the flow is collected, currentUser should reflect fakeUser
            cancelAndConsumeRemainingEvents()
        }
        assertEquals(fakeUser, vm.uiState.value.currentUser)
    }

    @Test
    fun `clearMessages resets errorMessage and successMessage`() = runTest {
        coEvery { signInUseCase(any(), any()) } returns
            Result.failure(Exception("エラー"))
        val vm = createViewModel()

        vm.signIn("x@x.com", "bad")
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(vm.uiState.value.errorMessage)

        vm.clearMessages()
        assertNull(vm.uiState.value.errorMessage)
    }
}
