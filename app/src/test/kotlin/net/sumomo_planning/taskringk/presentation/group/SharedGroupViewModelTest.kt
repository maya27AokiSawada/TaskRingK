package net.sumomo_planning.taskringk.presentation.group

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.sumomo_planning.taskringk.domain.model.AuthUser
import net.sumomo_planning.taskringk.domain.model.GroupType
import net.sumomo_planning.taskringk.domain.model.SharedGroup
import net.sumomo_planning.taskringk.domain.model.SyncStatus
import net.sumomo_planning.taskringk.domain.usecase.auth.ObserveAuthStateUseCase
import net.sumomo_planning.taskringk.domain.usecase.group.CreateGroupUseCase
import net.sumomo_planning.taskringk.domain.usecase.group.DeleteGroupUseCase
import net.sumomo_planning.taskringk.domain.usecase.group.LeaveGroupUseCase
import net.sumomo_planning.taskringk.domain.usecase.group.ObserveGroupsUseCase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SharedGroupViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val observeAuthStateUseCase = mockk<ObserveAuthStateUseCase>()
    private val observeGroupsUseCase = mockk<ObserveGroupsUseCase>()
    private val createGroupUseCase = mockk<CreateGroupUseCase>()
    private val deleteGroupUseCase = mockk<DeleteGroupUseCase>()
    private val leaveGroupUseCase = mockk<LeaveGroupUseCase>()

    private val fakeUser = AuthUser(uid = "uid-1", email = "test@example.com", displayName = "テスト")
    private val groupId = "grp_001"

    private val fakeGroup = SharedGroup(
        groupId = groupId,
        groupName = "テストグループ",
        ownerUid = "uid-1",
        allowedUid = listOf("uid-1"),
        members = emptyList(),
        groupType = GroupType.SHOPPING,
        syncStatus = SyncStatus.SYNCED,
        createdAt = Instant.EPOCH,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        // Default: no user signed in
        every { observeAuthStateUseCase() } returns flowOf(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = SharedGroupViewModel(
        observeAuthStateUseCase = observeAuthStateUseCase,
        observeGroupsUseCase = observeGroupsUseCase,
        createGroupUseCase = createGroupUseCase,
        deleteGroupUseCase = deleteGroupUseCase,
        leaveGroupUseCase = leaveGroupUseCase,
    )

    @Test
    fun `initial state has no user and empty groups`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        val state = vm.uiState.value
        assertNull(state.currentUser)
        assertTrue(state.groups.isEmpty())
        assertFalse(state.isLoading)
    }

    @Test
    fun `observeAuthState populates currentUser and starts observing groups`() = runTest {
        every { observeAuthStateUseCase() } returns flowOf(fakeUser)
        every { observeGroupsUseCase(fakeUser.uid) } returns flowOf(listOf(fakeGroup))

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertEquals(fakeUser, state.currentUser)
            assertEquals(1, state.groups.size)
            assertEquals(groupId, state.groups[0].groupId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `createGroup succeeds clears isLoading`() = runTest {
        every { observeAuthStateUseCase() } returns flowOf(fakeUser)
        every { observeGroupsUseCase(fakeUser.uid) } returns flowOf(emptyList())
        coEvery {
            createGroupUseCase(any(), any(), any(), any())
        } returns Result.success(fakeGroup)

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.createGroup("テストグループ")
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `createGroup failure sets errorMessage`() = runTest {
        every { observeAuthStateUseCase() } returns flowOf(fakeUser)
        every { observeGroupsUseCase(fakeUser.uid) } returns flowOf(emptyList())
        coEvery {
            createGroupUseCase(any(), any(), any(), any())
        } returns Result.failure(RuntimeException("作成失敗"))

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.createGroup("テストグループ")
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertEquals("作成失敗", vm.uiState.value.errorMessage)
    }

    @Test
    fun `createGroup does nothing when no current user`() = runTest {
        // No user signed in
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.createGroup("テストグループ")
        testDispatcher.scheduler.advanceUntilIdle()

        // createGroupUseCase should never be called
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `deleteGroup succeeds clears isLoading`() = runTest {
        every { observeAuthStateUseCase() } returns flowOf(fakeUser)
        every { observeGroupsUseCase(fakeUser.uid) } returns flowOf(emptyList())
        coEvery { deleteGroupUseCase(groupId) } returns Result.success(Unit)

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.deleteGroup(groupId)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `deleteGroup failure sets errorMessage`() = runTest {
        every { observeAuthStateUseCase() } returns flowOf(fakeUser)
        every { observeGroupsUseCase(fakeUser.uid) } returns flowOf(emptyList())
        coEvery { deleteGroupUseCase(groupId) } returns Result.failure(RuntimeException("削除失敗"))

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.deleteGroup(groupId)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertNotNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `leaveGroup succeeds clears isLoading`() = runTest {
        every { observeAuthStateUseCase() } returns flowOf(fakeUser)
        every { observeGroupsUseCase(fakeUser.uid) } returns flowOf(emptyList())
        coEvery { leaveGroupUseCase(groupId, fakeUser.uid, "member-1") } returns Result.success(Unit)

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.leaveGroup(groupId, "member-1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `leaveGroup does nothing when no current user`() = runTest {
        // No user signed in
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.leaveGroup(groupId, "member-1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `clearError resets errorMessage to null`() = runTest {
        every { observeAuthStateUseCase() } returns flowOf(fakeUser)
        every { observeGroupsUseCase(fakeUser.uid) } returns flowOf(emptyList())
        coEvery {
            createGroupUseCase(any(), any(), any(), any())
        } returns Result.failure(RuntimeException("エラー"))

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.createGroup("グループ")
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(vm.uiState.value.errorMessage)

        vm.clearError()
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `sign out clears groups list`() = runTest {
        // Start signed in, then sign out
        val authFlow = kotlinx.coroutines.flow.MutableStateFlow<AuthUser?>(fakeUser)
        every { observeAuthStateUseCase() } returns authFlow
        every { observeGroupsUseCase(fakeUser.uid) } returns flowOf(listOf(fakeGroup))

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, vm.uiState.value.groups.size)

        // Simulate sign out
        authFlow.value = null
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.groups.isEmpty())
        assertNull(vm.uiState.value.currentUser)
    }
}
