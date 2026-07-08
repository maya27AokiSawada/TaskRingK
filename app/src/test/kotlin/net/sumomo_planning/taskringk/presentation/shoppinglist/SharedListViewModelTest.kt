package net.sumomo_planning.taskringk.presentation.shoppinglist

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
import net.sumomo_planning.taskringk.core.common.DeviceIdService
import net.sumomo_planning.taskringk.core.network.NetworkMonitor
import net.sumomo_planning.taskringk.domain.model.AuthUser
import net.sumomo_planning.taskringk.domain.model.GroupType
import net.sumomo_planning.taskringk.domain.model.ListKind
import net.sumomo_planning.taskringk.domain.model.ListType
import net.sumomo_planning.taskringk.domain.model.SharedGroup
import net.sumomo_planning.taskringk.domain.model.SharedItem
import net.sumomo_planning.taskringk.domain.model.SharedList
import net.sumomo_planning.taskringk.domain.model.SyncStatus
import net.sumomo_planning.taskringk.domain.usecase.auth.ObserveAuthStateUseCase
import net.sumomo_planning.taskringk.domain.usecase.group.ObserveGroupsUseCase
import net.sumomo_planning.taskringk.domain.usecase.list.AddItemUseCase
import net.sumomo_planning.taskringk.domain.usecase.list.CreateListUseCase
import net.sumomo_planning.taskringk.domain.usecase.list.DeleteListUseCase
import net.sumomo_planning.taskringk.domain.usecase.list.ObserveListUseCase
import net.sumomo_planning.taskringk.domain.usecase.list.ObserveListsByGroupUseCase
import net.sumomo_planning.taskringk.domain.usecase.list.RemoveItemUseCase
import net.sumomo_planning.taskringk.domain.usecase.list.UpdateItemUseCase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SharedListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val observeAuthStateUseCase = mockk<ObserveAuthStateUseCase>()
    private val observeGroupsUseCase = mockk<ObserveGroupsUseCase>()
    private val observeListsByGroupUseCase = mockk<ObserveListsByGroupUseCase>()
    private val observeListUseCase = mockk<ObserveListUseCase>()
    private val createListUseCase = mockk<CreateListUseCase>()
    private val deleteListUseCase = mockk<DeleteListUseCase>()
    private val addItemUseCase = mockk<AddItemUseCase>()
    private val updateItemUseCase = mockk<UpdateItemUseCase>()
    private val removeItemUseCase = mockk<RemoveItemUseCase>()
    private val deviceIdService = mockk<DeviceIdService>()
    private val networkMonitor = mockk<NetworkMonitor>()

    private val fakeUser = AuthUser(uid = "uid-1", email = "test@example.com", displayName = "テスト")
    private val groupId = "grp_001"
    private val listId = "lst_abc12345"

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

    private val fakeList = SharedList(
        listId = listId,
        listName = "週末の買い物",
        ownerUid = "uid-1",
        groupId = groupId,
        groupName = "テストグループ",
        description = "",
        listType = ListType.SHOPPING,
        listKind = ListKind.SHOPPING_LIST,
        items = emptyMap(),
        createdAt = Instant.EPOCH,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        // Default: online
        every { networkMonitor.isOnlineFlow } returns flowOf(true)
        every { networkMonitor.isOnline } returns true
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(): SharedListViewModel {
        return SharedListViewModel(
            observeAuthStateUseCase = observeAuthStateUseCase,
            observeGroupsUseCase = observeGroupsUseCase,
            observeListsByGroupUseCase = observeListsByGroupUseCase,
            observeListUseCase = observeListUseCase,
            createListUseCase = createListUseCase,
            deleteListUseCase = deleteListUseCase,
            addItemUseCase = addItemUseCase,
            updateItemUseCase = updateItemUseCase,
            removeItemUseCase = removeItemUseCase,
            deviceIdService = deviceIdService,
            networkMonitor = networkMonitor,
        )
    }

    @Test
    fun `initial state is empty with no user`() = runTest {
        every { observeAuthStateUseCase() } returns flowOf(null)

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertNull(state.currentUser)
            assertTrue(state.groups.isEmpty())
            assertTrue(state.lists.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `user sign-in starts group observation`() = runTest {
        every { observeAuthStateUseCase() } returns flowOf(fakeUser)
        every { observeGroupsUseCase(fakeUser.uid) } returns flowOf(listOf(fakeGroup))
        every { observeListsByGroupUseCase(groupId) } returns flowOf(listOf(fakeList))
        every { observeListUseCase(groupId, listId) } returns flowOf(fakeList)

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertEquals(fakeUser, state.currentUser)
            assertEquals(1, state.groups.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `createList success updates selected list`() = runTest {
        every { observeAuthStateUseCase() } returns flowOf(fakeUser)
        every { observeGroupsUseCase(fakeUser.uid) } returns flowOf(listOf(fakeGroup))
        every { observeListsByGroupUseCase(groupId) } returns flowOf(emptyList())
        coEvery { createListUseCase(groupId, any(), any(), any(), fakeUser.uid) } returns Result.success(fakeList)
        every { observeListUseCase(groupId, listId) } returns flowOf(fakeList)

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.createList("週末の買い物")
        testDispatcher.scheduler.advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertEquals(listId, state.selectedListId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `createList failure sets errorMessage`() = runTest {
        every { observeAuthStateUseCase() } returns flowOf(fakeUser)
        every { observeGroupsUseCase(fakeUser.uid) } returns flowOf(listOf(fakeGroup))
        every { observeListsByGroupUseCase(groupId) } returns flowOf(emptyList())
        coEvery {
            createListUseCase(any(), any(), any(), any(), any())
        } returns Result.failure(RuntimeException("作成失敗"))

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.createList("失敗するリスト")
        testDispatcher.scheduler.advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertNotNull(state.errorMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `createList does nothing when no user`() = runTest {
        every { observeAuthStateUseCase() } returns flowOf(null)

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.createList("無視されるリスト")
        testDispatcher.scheduler.advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertNull(state.selectedListId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `addItem does nothing when no user`() = runTest {
        every { observeAuthStateUseCase() } returns flowOf(null)

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.addItem("牛乳")
        testDispatcher.scheduler.advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertNull(state.errorMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `addItem failure sets errorMessage`() = runTest {
        every { observeAuthStateUseCase() } returns flowOf(fakeUser)
        every { observeGroupsUseCase(fakeUser.uid) } returns flowOf(listOf(fakeGroup))
        every { observeListsByGroupUseCase(groupId) } returns flowOf(listOf(fakeList))
        every { observeListUseCase(groupId, listId) } returns flowOf(fakeList)
        every { deviceIdService.generateItemId() } returns "item-001"
        coEvery { addItemUseCase(any(), any(), any()) } returns Result.failure(RuntimeException("追加失敗"))

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.addItem("牛乳")
        testDispatcher.scheduler.advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertNotNull(state.errorMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearError resets errorMessage`() = runTest {
        every { observeAuthStateUseCase() } returns flowOf(fakeUser)
        every { observeGroupsUseCase(fakeUser.uid) } returns flowOf(listOf(fakeGroup))
        every { observeListsByGroupUseCase(groupId) } returns flowOf(emptyList())
        coEvery {
            createListUseCase(any(), any(), any(), any(), any())
        } returns Result.failure(RuntimeException("エラー"))

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.createList("エラーリスト")
        testDispatcher.scheduler.advanceUntilIdle()
        vm.clearError()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertNull(state.errorMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sign-out clears all state`() = runTest {
        val authFlow = kotlinx.coroutines.flow.MutableStateFlow<AuthUser?>(fakeUser)
        every { observeAuthStateUseCase() } returns authFlow
        every { observeGroupsUseCase(fakeUser.uid) } returns flowOf(listOf(fakeGroup))
        every { observeListsByGroupUseCase(groupId) } returns flowOf(listOf(fakeList))
        every { observeListUseCase(groupId, listId) } returns flowOf(fakeList)

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        authFlow.value = null
        testDispatcher.scheduler.advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertNull(state.currentUser)
            assertTrue(state.groups.isEmpty())
            assertTrue(state.lists.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `togglePurchased does nothing when no user or list`() = runTest {
        every { observeAuthStateUseCase() } returns flowOf(null)

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val item = SharedItem(
            itemId = "item-001",
            name = "テスト",
            quantity = 1,
            memberId = "m1",
            registeredDate = Instant.EPOCH,
        )
        vm.togglePurchased(item)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertNull(state.errorMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
