package net.sumomo_planning.taskringk.data.remote.dto

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

@Keep
data class WhiteboardDto(
    @DocumentId val documentId: String = "",
    val whiteboardId: String = "",
    val groupId: String = "",
    val ownerId: String? = null,
    val isPrivate: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
)

@Keep
data class DrawingStrokeDto(
    @DocumentId val documentId: String = "",
    val strokeId: String = "",
    val points: List<DrawingPointDto> = emptyList(),
    val colorValue: Int = 0,
    val strokeWidth: Float = 1f,
    val strokeCapStyle: String = "round",
    val createdAt: Timestamp? = null,
    val authorId: String = "",
    val authorName: String = "",
)

@Keep
data class DrawingPointDto(
    val x: Float = 0f,
    val y: Float = 0f,
)
