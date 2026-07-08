package net.sumomo_planning.taskringk.domain.model

import java.time.Instant

data class Whiteboard(
    val whiteboardId: String,
    val groupId: String,
    val ownerId: String? = null,
    val isPrivate: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant? = null,
)

data class DrawingStroke(
    val strokeId: String,
    val points: List<DrawingPoint>,
    val colorValue: Int,
    val strokeWidth: Float,
    val createdAt: Instant,
    val authorId: String,
    val authorName: String,
)

data class DrawingPoint(val x: Float, val y: Float)
