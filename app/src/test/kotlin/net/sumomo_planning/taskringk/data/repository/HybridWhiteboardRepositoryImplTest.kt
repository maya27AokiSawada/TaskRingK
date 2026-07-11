package net.sumomo_planning.taskringk.data.repository

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.sumomo_planning.taskringk.core.network.NetworkMonitor
import net.sumomo_planning.taskringk.data.local.room.dao.WhiteboardDao
import net.sumomo_planning.taskringk.data.local.room.entity.WhiteboardEntity
import net.sumomo_planning.taskringk.data.remote.firestore.FirestoreWhiteboardDataSource
import net.sumomo_planning.taskringk.domain.model.DrawingPoint
import net.sumomo_planning.taskringk.domain.model.DrawingStroke
import net.sumomo_planning.taskringk.domain.model.Whiteboard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HybridWhiteboardRepositoryImplTest {

    private val firestoreDataSource = mockk<FirestoreWhiteboardDataSource>()
    private val whiteboardDao = mockk<WhiteboardDao>(relaxed = true)
    private val networkMonitor = mockk<NetworkMonitor>()

    private val groupId = "grp_001"
    private val boardId = "board_001"

    private val fakeBoard = Whiteboard(
        whiteboardId = boardId,
        groupId = groupId,
        ownerId = "uid_1",
        isPrivate = false,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    private val fakeEntity = WhiteboardEntity(
        whiteboardId = boardId,
        groupId = groupId,
        ownerId = "uid_1",
        isPrivate = false,
        createdAt = 0L,
        updatedAt = 0L,
    )

    private lateinit var repository: HybridWhiteboardRepositoryImpl

    @Before
    fun setUp() {
        every { networkMonitor.isOnlineFlow } returns flowOf(true)
        every { networkMonitor.isOnline } returns true
        every { whiteboardDao.observeById(boardId) } returns flowOf(fakeEntity)

        repository = HybridWhiteboardRepositoryImpl(
            firestoreDataSource = firestoreDataSource,
            whiteboardDao = whiteboardDao,
            networkMonitor = networkMonitor,
        )
    }

    @Test
    fun `observeByGroup emits Firestore data and caches to Room`() = runTest {
        every { firestoreDataSource.observeByGroup(groupId) } returns flowOf(listOf(fakeBoard))

        repository.observeByGroup(groupId).test {
            val boards = awaitItem()
            assertEquals(1, boards.size)
            assertEquals(boardId, boards[0].whiteboardId)
            coVerify { whiteboardDao.upsertAll(any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeByGroup falls back to Room when Firestore fails`() = runTest {
        every { firestoreDataSource.observeByGroup(groupId) } returns flow {
            throw RuntimeException("firestore error")
        }
        every { whiteboardDao.observeByGroup(groupId) } returns flowOf(listOf(fakeEntity))

        repository.observeByGroup(groupId).test {
            val boards = awaitItem()
            assertEquals(1, boards.size)
            assertEquals(boardId, boards[0].whiteboardId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeByGroup uses Room directly when offline`() = runTest {
        every { networkMonitor.isOnlineFlow } returns flowOf(false)
        every { whiteboardDao.observeByGroup(groupId) } returns flowOf(listOf(fakeEntity))

        repository.observeByGroup(groupId).test {
            val boards = awaitItem()
            assertEquals(1, boards.size)
            coVerify(exactly = 0) { firestoreDataSource.observeByGroup(any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `createWhiteboard writes to Firestore and Room`() = runTest {
        coJustRun { firestoreDataSource.createWhiteboard(any(), any()) }

        val result = repository.createWhiteboard(groupId = groupId, ownerId = "uid_1", isPrivate = false)

        assertTrue(result.isSuccess)
        val created = result.getOrThrow()
        assertEquals(groupId, created.groupId)
        coVerify { firestoreDataSource.createWhiteboard(eq(groupId), any()) }
        coVerify { whiteboardDao.upsert(any()) }
    }

    @Test
    fun `deleteWhiteboard removes from Firestore then Room`() = runTest {
        coJustRun { firestoreDataSource.deleteWhiteboard(groupId, boardId) }

        val result = repository.deleteWhiteboard(boardId)

        assertTrue(result.isSuccess)
        coVerifyOrder {
            firestoreDataSource.deleteWhiteboard(groupId, boardId)
            whiteboardDao.deleteById(boardId)
        }
    }

    @Test
    fun `observeStrokes delegates to Firestore stream`() = runTest {
        val stroke = DrawingStroke(
            strokeId = "s1",
            points = listOf(DrawingPoint(0f, 0f), DrawingPoint(2f, 2f)),
            colorValue = 0xFF111111.toInt(),
            strokeWidth = 4f,
            createdAt = Instant.EPOCH,
            authorId = "uid_1",
            authorName = "tester",
        )
        every { firestoreDataSource.observeStrokes(groupId, boardId) } returns flowOf(listOf(stroke))

        repository.observeStrokes(groupId, boardId).test {
            val strokes = awaitItem()
            assertEquals(1, strokes.size)
            assertEquals("s1", strokes[0].strokeId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `stroke mutation methods delegate to Firestore`() = runTest {
        val stroke = DrawingStroke(
            strokeId = "s2",
            points = listOf(DrawingPoint(1f, 1f), DrawingPoint(3f, 3f)),
            colorValue = 0xFF000000.toInt(),
            strokeWidth = 3f,
            createdAt = Instant.now(),
            authorId = "uid_1",
            authorName = "tester",
        )
        coJustRun { firestoreDataSource.upsertStroke(groupId, boardId, stroke) }
        coJustRun { firestoreDataSource.deleteStroke(groupId, boardId, stroke.strokeId) }
        coJustRun { firestoreDataSource.clearStrokes(groupId, boardId) }

        assertTrue(repository.upsertStroke(groupId, boardId, stroke).isSuccess)
        assertTrue(repository.deleteStroke(groupId, boardId, stroke.strokeId).isSuccess)
        assertTrue(repository.clearStrokes(groupId, boardId).isSuccess)

        coVerify { firestoreDataSource.upsertStroke(groupId, boardId, stroke) }
        coVerify { firestoreDataSource.deleteStroke(groupId, boardId, stroke.strokeId) }
        coVerify { firestoreDataSource.clearStrokes(groupId, boardId) }
    }
}
