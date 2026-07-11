package net.sumomo_planning.taskringk.domain.repository

import kotlinx.coroutines.flow.Flow
import net.sumomo_planning.taskringk.domain.model.DrawingStroke
import net.sumomo_planning.taskringk.domain.model.Whiteboard

interface WhiteboardRepository {
    fun observeByGroup(groupId: String): Flow<List<Whiteboard>>

    fun observeStrokes(groupId: String, whiteboardId: String): Flow<List<DrawingStroke>>

    suspend fun createWhiteboard(
        groupId: String,
        ownerId: String?,
        isPrivate: Boolean,
    ): Result<Whiteboard>

    suspend fun deleteWhiteboard(whiteboardId: String): Result<Unit>

    suspend fun upsertStroke(
        groupId: String,
        whiteboardId: String,
        stroke: DrawingStroke,
    ): Result<Unit>

    suspend fun deleteStroke(
        groupId: String,
        whiteboardId: String,
        strokeId: String,
    ): Result<Unit>

    suspend fun clearStrokes(groupId: String, whiteboardId: String): Result<Unit>
}