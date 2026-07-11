package net.sumomo_planning.taskringk.data.repository

import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import net.sumomo_planning.taskringk.domain.model.DrawingStroke
import net.sumomo_planning.taskringk.data.local.room.dao.WhiteboardDao
import net.sumomo_planning.taskringk.data.mapper.toDomain
import net.sumomo_planning.taskringk.data.mapper.toEntity
import net.sumomo_planning.taskringk.domain.model.Whiteboard
import net.sumomo_planning.taskringk.domain.repository.WhiteboardRepository

@Singleton
class LocalWhiteboardRepositoryImpl @Inject constructor(
    private val whiteboardDao: WhiteboardDao,
) : WhiteboardRepository {

    private val strokesState = MutableStateFlow<Map<String, List<DrawingStroke>>>(emptyMap())

    override fun observeByGroup(groupId: String): Flow<List<Whiteboard>> =
        whiteboardDao.observeByGroup(groupId)
            .map { entities -> entities.map { it.toDomain() } }

    override fun observeStrokes(groupId: String, whiteboardId: String): Flow<List<DrawingStroke>> =
        strokesState.map { all -> all[whiteboardId].orEmpty() }

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
        whiteboardDao.upsert(whiteboard.toEntity())
        whiteboard
    }

    override suspend fun deleteWhiteboard(whiteboardId: String): Result<Unit> = runCatching {
        whiteboardDao.deleteById(whiteboardId)
        strokesState.value = strokesState.value - whiteboardId
    }

    override suspend fun upsertStroke(
        groupId: String,
        whiteboardId: String,
        stroke: DrawingStroke,
    ): Result<Unit> = runCatching {
        val current = strokesState.value[whiteboardId].orEmpty()
        val updated = current.filterNot { it.strokeId == stroke.strokeId } + stroke
        strokesState.value = strokesState.value + (whiteboardId to updated.sortedBy { it.createdAt })
    }

    override suspend fun deleteStroke(
        groupId: String,
        whiteboardId: String,
        strokeId: String,
    ): Result<Unit> = runCatching {
        val current = strokesState.value[whiteboardId].orEmpty()
        strokesState.value = strokesState.value +
            (whiteboardId to current.filterNot { it.strokeId == strokeId })
    }

    override suspend fun clearStrokes(groupId: String, whiteboardId: String): Result<Unit> = runCatching {
        strokesState.value = strokesState.value + (whiteboardId to emptyList())
    }
}