package net.sumomo_planning.taskringk.data.repository

import android.util.Log
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import net.sumomo_planning.taskringk.core.network.NetworkMonitor
import net.sumomo_planning.taskringk.data.local.room.dao.WhiteboardDao
import net.sumomo_planning.taskringk.data.mapper.toDomain
import net.sumomo_planning.taskringk.data.mapper.toEntity
import net.sumomo_planning.taskringk.data.remote.firestore.FirestoreWhiteboardDataSource
import net.sumomo_planning.taskringk.domain.model.DrawingStroke
import net.sumomo_planning.taskringk.domain.model.Whiteboard
import net.sumomo_planning.taskringk.domain.repository.WhiteboardRepository

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class HybridWhiteboardRepositoryImpl @Inject constructor(
    private val firestoreDataSource: FirestoreWhiteboardDataSource,
    private val whiteboardDao: WhiteboardDao,
    private val networkMonitor: NetworkMonitor,
) : WhiteboardRepository {

    override fun observeByGroup(groupId: String): Flow<List<Whiteboard>> =
        networkMonitor.isOnlineFlow.flatMapLatest { isOnline ->
            if (isOnline) {
                firestoreDataSource.observeByGroup(groupId)
                    .onEach { whiteboards ->
                        whiteboardDao.upsertAll(whiteboards.map { it.toEntity() })
                    }
                    .catch { error ->
                        Log.w(TAG, "Firestore observeByGroup error - falling back to Room", error)
                        emitAll(
                            whiteboardDao.observeByGroup(groupId)
                                .map { entities -> entities.map { it.toDomain() } }
                        )
                    }
            } else {
                whiteboardDao.observeByGroup(groupId)
                    .map { entities -> entities.map { it.toDomain() } }
            }
        }

    override fun observeStrokes(groupId: String, whiteboardId: String): Flow<List<DrawingStroke>> =
        firestoreDataSource.observeStrokes(groupId, whiteboardId)

    override suspend fun createWhiteboard(
        groupId: String,
        ownerId: String?,
        isPrivate: Boolean,
    ): Result<Whiteboard> = runCatching {
        val now = Instant.now()
        val whiteboard = Whiteboard(
            whiteboardId = UUID.randomUUID().toString(),
            groupId = groupId,
            ownerId = ownerId,
            isPrivate = isPrivate,
            createdAt = now,
            updatedAt = now,
        )

        runCatching { firestoreDataSource.createWhiteboard(groupId, whiteboard) }
            .onFailure { Log.w(TAG, "Firestore createWhiteboard failed (non-fatal)", it) }

        whiteboardDao.upsert(whiteboard.toEntity())
        whiteboard
    }

    override suspend fun deleteWhiteboard(whiteboardId: String): Result<Unit> = runCatching {
        val board = whiteboardDao.observeById(whiteboardId).firstOrNull()?.toDomain()
        if (board != null) {
            runCatching { firestoreDataSource.deleteWhiteboard(board.groupId, whiteboardId) }
                .onFailure { Log.w(TAG, "Firestore deleteWhiteboard failed (non-fatal)", it) }
        }
        whiteboardDao.deleteById(whiteboardId)
    }

    override suspend fun upsertStroke(
        groupId: String,
        whiteboardId: String,
        stroke: DrawingStroke,
    ): Result<Unit> = runCatching {
        firestoreDataSource.upsertStroke(groupId, whiteboardId, stroke)
    }

    override suspend fun deleteStroke(
        groupId: String,
        whiteboardId: String,
        strokeId: String,
    ): Result<Unit> = runCatching {
        firestoreDataSource.deleteStroke(groupId, whiteboardId, strokeId)
    }

    override suspend fun clearStrokes(groupId: String, whiteboardId: String): Result<Unit> = runCatching {
        firestoreDataSource.clearStrokes(groupId, whiteboardId)
    }

    private companion object {
        const val TAG = "HybridWhiteboardRepo"
    }
}
