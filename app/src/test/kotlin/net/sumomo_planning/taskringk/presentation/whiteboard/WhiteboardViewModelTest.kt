package net.sumomo_planning.taskringk.presentation.whiteboard

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.sumomo_planning.taskringk.domain.model.AuthUser
import net.sumomo_planning.taskringk.domain.model.DrawingPoint
import net.sumomo_planning.taskringk.domain.model.DrawingStroke
import net.sumomo_planning.taskringk.domain.model.SharedGroup
import net.sumomo_planning.taskringk.domain.model.StrokeCapStyle
import net.sumomo_planning.taskringk.domain.model.Whiteboard
import net.sumomo_planning.taskringk.domain.usecase.auth.ObserveAuthStateUseCase
import net.sumomo_planning.taskringk.domain.usecase.group.ObserveGroupsUseCase
import net.sumomo_planning.taskringk.domain.usecase.whiteboard.ClearWhiteboardStrokesUseCase
import net.sumomo_planning.taskringk.domain.usecase.whiteboard.CreateWhiteboardUseCase
import net.sumomo_planning.taskringk.domain.usecase.whiteboard.DeleteWhiteboardStrokeUseCase
import net.sumomo_planning.taskringk.domain.usecase.whiteboard.DeleteWhiteboardUseCase
import net.sumomo_planning.taskringk.domain.usecase.whiteboard.ObserveWhiteboardStrokesUseCase
import net.sumomo_planning.taskringk.domain.usecase.whiteboard.ObserveWhiteboardsByGroupUseCase
import net.sumomo_planning.taskringk.domain.usecase.whiteboard.UpsertWhiteboardStrokeUseCase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WhiteboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val observeAuthStateUseCase = mockk<ObserveAuthStateUseCase>()
    private val observeGroupsUseCase = mockk<ObserveGroupsUseCase>()
    private val observeWhiteboardsByGroupUseCase = mockk<ObserveWhiteboardsByGroupUseCase>()
    private val createWhiteboardUseCase = mockk<CreateWhiteboardUseCase>()
    private val deleteWhiteboardUseCase = mockk<DeleteWhiteboardUseCase>()
    private val observeWhiteboardStrokesUseCase = mockk<ObserveWhiteboardStrokesUseCase>()
    private val upsertWhiteboardStrokeUseCase = mockk<UpsertWhiteboardStrokeUseCase>()
    private val deleteWhiteboardStrokeUseCase = mockk<DeleteWhiteboardStrokeUseCase>()
    private val clearWhiteboardStrokesUseCase = mockk<ClearWhiteboardStrokesUseCase>()

    private val fakeUser = AuthUser(uid = "uid-1", email = "test@example.com", displayName = "tester")
    private val groupId = "grp-1"
    private val boardId = "board-1"

    private val fakeGroup = SharedGroup(
        groupId = groupId,
        groupName = "group",
        ownerUid = fakeUser.uid,
        allowedUid = listOf(fakeUser.uid),
        members = emptyList(),
        createdAt = Instant.EPOCH,
    )

    private val fakeBoard = Whiteboard(
        whiteboardId = boardId,
        groupId = groupId,
        ownerId = fakeUser.uid,
        isPrivate = false,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { observeAuthStateUseCase() } returns flowOf(fakeUser)
        every { observeGroupsUseCase(fakeUser.uid) } returns flowOf(listOf(fakeGroup))
        every { observeWhiteboardsByGroupUseCase(groupId) } returns flowOf(listOf(fakeBoard))
        every { observeWhiteboardStrokesUseCase(groupId, boardId) } returns flowOf(emptyList())

        coEvery { createWhiteboardUseCase(any(), any(), any()) } returns Result.success(fakeBoard)
        coEvery { deleteWhiteboardUseCase(any()) } returns Result.success(Unit)
        coEvery { upsertWhiteboardStrokeUseCase(any(), any(), any()) } returns Result.success(Unit)
        coEvery { deleteWhiteboardStrokeUseCase(any(), any(), any()) } returns Result.success(Unit)
        coEvery { clearWhiteboardStrokesUseCase(any(), any()) } returns Result.success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = WhiteboardViewModel(
        observeAuthStateUseCase = observeAuthStateUseCase,
        observeGroupsUseCase = observeGroupsUseCase,
        observeWhiteboardsByGroupUseCase = observeWhiteboardsByGroupUseCase,
        createWhiteboardUseCase = createWhiteboardUseCase,
        deleteWhiteboardUseCase = deleteWhiteboardUseCase,
        observeWhiteboardStrokesUseCase = observeWhiteboardStrokesUseCase,
        upsertWhiteboardStrokeUseCase = upsertWhiteboardStrokeUseCase,
        deleteWhiteboardStrokeUseCase = deleteWhiteboardStrokeUseCase,
        clearWhiteboardStrokesUseCase = clearWhiteboardStrokesUseCase,
    )

    @Test
    fun `observe stroke flow updates committedStrokes`() = runTest {
        val remoteStrokes = MutableStateFlow<List<DrawingStroke>>(emptyList())
        every { observeWhiteboardStrokesUseCase(groupId, boardId) } returns remoteStrokes

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(groupId, vm.uiState.value.selectedGroupId)
        assertEquals(boardId, vm.uiState.value.selectedWhiteboardId)
        assertTrue(vm.uiState.value.committedStrokes.isEmpty())

        val stroke = DrawingStroke(
            strokeId = "s-1",
            points = listOf(DrawingPoint(0f, 0f), DrawingPoint(10f, 10f)),
            colorValue = 0xFF111111.toInt(),
            strokeWidth = 6f,
            createdAt = Instant.now(),
            authorId = fakeUser.uid,
            authorName = "tester",
        )
        remoteStrokes.value = listOf(stroke)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, vm.uiState.value.committedStrokes.size)
        assertEquals("s-1", vm.uiState.value.committedStrokes.first().strokeId)
    }

    @Test
    fun `endStroke persists active stroke via upsert usecase`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.selectBrushColor(0xFF1976D2.toInt())
        vm.updateStrokeWidth(9f)
        vm.selectStrokeCapStyle(StrokeCapStyle.BUTT)
        vm.startStroke(androidx.compose.ui.geometry.Offset(1f, 1f))
        vm.appendStrokePoint(androidx.compose.ui.geometry.Offset(5f, 6f))
        vm.endStroke()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            upsertWhiteboardStrokeUseCase(
                groupId,
                boardId,
                match { stroke ->
                    stroke.authorId == fakeUser.uid &&
                        stroke.points.size == 2 &&
                        stroke.colorValue == 0xFF1976D2.toInt() &&
                        stroke.strokeWidth == 9f &&
                        stroke.strokeCapStyle == StrokeCapStyle.BUTT
                },
            )
        }
        assertNull(vm.uiState.value.activeStroke)
    }

    @Test
    fun `eraser mode writes white stroke color`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.selectBrushColor(0xFFD32F2F.toInt())
        vm.toggleEraser()
        vm.startStroke(androidx.compose.ui.geometry.Offset(2f, 2f))
        vm.appendStrokePoint(androidx.compose.ui.geometry.Offset(8f, 8f))
        vm.endStroke()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            upsertWhiteboardStrokeUseCase(
                groupId,
                boardId,
                match { stroke -> stroke.colorValue == 0xFFFFFFFF.toInt() },
            )
        }
    }

    @Test
    fun `undo pushes redo stack and deletes latest stroke remotely`() = runTest {
        val s1 = DrawingStroke(
            strokeId = "s1",
            points = listOf(DrawingPoint(0f, 0f), DrawingPoint(1f, 1f)),
            colorValue = 0,
            strokeWidth = 2f,
            createdAt = Instant.EPOCH,
            authorId = fakeUser.uid,
            authorName = "tester",
        )
        val s2 = DrawingStroke(
            strokeId = "s2",
            points = listOf(DrawingPoint(1f, 1f), DrawingPoint(2f, 2f)),
            colorValue = 0,
            strokeWidth = 2f,
            createdAt = Instant.ofEpochSecond(1),
            authorId = fakeUser.uid,
            authorName = "tester",
        )
        every { observeWhiteboardStrokesUseCase(groupId, boardId) } returns flowOf(listOf(s1, s2))

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.undo()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { deleteWhiteboardStrokeUseCase(groupId, boardId, "s2") }
        assertEquals(1, vm.uiState.value.redoStack.size)
        assertEquals("s2", vm.uiState.value.redoStack.first().strokeId)
    }

    @Test
    fun `redo re-upserts first redo stroke`() = runTest {
        val s1 = DrawingStroke(
            strokeId = "s1",
            points = listOf(DrawingPoint(0f, 0f), DrawingPoint(1f, 1f)),
            colorValue = 0,
            strokeWidth = 2f,
            createdAt = Instant.EPOCH,
            authorId = fakeUser.uid,
            authorName = "tester",
        )
        val s2 = s1.copy(strokeId = "s2", createdAt = Instant.ofEpochSecond(1))
        every { observeWhiteboardStrokesUseCase(groupId, boardId) } returns flowOf(listOf(s1, s2))

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.undo()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.redo()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            upsertWhiteboardStrokeUseCase(
                groupId,
                boardId,
                match { it.strokeId == "s2" },
            )
        }
        assertTrue(vm.uiState.value.redoStack.isEmpty())
    }

    @Test
    fun `clearBoard clears local state and calls remote clear usecase`() = runTest {
        val stroke = DrawingStroke(
            strokeId = "s1",
            points = listOf(DrawingPoint(0f, 0f), DrawingPoint(1f, 1f)),
            colorValue = 0,
            strokeWidth = 2f,
            createdAt = Instant.EPOCH,
            authorId = fakeUser.uid,
            authorName = "tester",
        )
        every { observeWhiteboardStrokesUseCase(groupId, boardId) } returns flowOf(listOf(stroke))

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.clearBoard()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { clearWhiteboardStrokesUseCase(groupId, boardId) }
        assertTrue(vm.uiState.value.committedStrokes.isEmpty())
        assertTrue(vm.uiState.value.redoStack.isEmpty())
    }
}
