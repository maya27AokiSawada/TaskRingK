package net.sumomo_planning.taskringk.data.mapper

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import java.time.Instant
import net.sumomo_planning.taskringk.data.remote.dto.DrawingPointDto
import net.sumomo_planning.taskringk.data.remote.dto.DrawingStrokeDto
import net.sumomo_planning.taskringk.data.remote.dto.WhiteboardDto
import net.sumomo_planning.taskringk.domain.model.DrawingPoint
import net.sumomo_planning.taskringk.domain.model.DrawingStroke
import net.sumomo_planning.taskringk.domain.model.StrokeCapStyle
import net.sumomo_planning.taskringk.domain.model.Whiteboard

fun WhiteboardDto.toDomain(): Whiteboard = Whiteboard(
    whiteboardId = whiteboardId.ifBlank { documentId },
    groupId = groupId,
    ownerId = ownerId,
    isPrivate = isPrivate,
    createdAt = createdAt?.toDate()?.toInstant() ?: Instant.now(),
    updatedAt = updatedAt?.toDate()?.toInstant(),
)

fun Whiteboard.toFirestoreMap(): Map<String, Any?> = mapOf(
    "whiteboardId" to whiteboardId,
    "groupId" to groupId,
    "ownerId" to ownerId,
    "isPrivate" to isPrivate,
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp(),
)

fun DrawingStrokeDto.toDomain(): DrawingStroke = DrawingStroke(
    strokeId = strokeId.ifBlank { documentId },
    points = points.map { it.toDomain() },
    colorValue = colorValue,
    strokeWidth = strokeWidth,
    strokeCapStyle = strokeCapStyle.toStrokeCapStyle(),
    createdAt = createdAt?.toDate()?.toInstant() ?: Instant.now(),
    authorId = authorId,
    authorName = authorName,
)

fun DrawingPointDto.toDomain(): DrawingPoint = DrawingPoint(x = x, y = y)

fun DrawingStroke.toFirestoreMap(): Map<String, Any?> = mapOf(
    "strokeId" to strokeId,
    "points" to points.map { it.toFirestoreMap() },
    "colorValue" to colorValue,
    "strokeWidth" to strokeWidth,
    "strokeCapStyle" to strokeCapStyle.storageValue,
    "createdAt" to Timestamp(createdAt.epochSecond, createdAt.nano),
    "authorId" to authorId,
    "authorName" to authorName,
)

fun DrawingPoint.toFirestoreMap(): Map<String, Any?> = mapOf(
    "x" to x,
    "y" to y,
)

private val StrokeCapStyle.storageValue: String
    get() = when (this) {
        StrokeCapStyle.ROUND -> "round"
        StrokeCapStyle.BUTT -> "butt"
    }

private fun String.toStrokeCapStyle(): StrokeCapStyle = when (lowercase()) {
    "butt" -> StrokeCapStyle.BUTT
    else -> StrokeCapStyle.ROUND
}
