package com.goshopping.android.data.model

import com.google.firebase.Timestamp
import java.util.Date

data class Whiteboard(
    val whiteboardId: String = "",
    val groupId: String = "",
    /** null = グループ共有、非null = 個人用 */
    val ownerId: String? = null,
    val isPrivate: Boolean = false,
    val createdAt: Date = Date(),
    val updatedAt: Date? = null
) {
    val isGroupShared: Boolean get() = ownerId == null && !isPrivate

    fun toMap(): Map<String, Any?> = mapOf(
        "whiteboardId" to whiteboardId,
        "groupId" to groupId,
        "ownerId" to ownerId,
        "isPrivate" to isPrivate,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): Whiteboard = Whiteboard(
            whiteboardId = map["whiteboardId"] as? String ?: "",
            groupId = map["groupId"] as? String ?: "",
            ownerId = map["ownerId"] as? String,
            isPrivate = map["isPrivate"] as? Boolean ?: false,
            createdAt = (map["createdAt"] as? Timestamp)?.toDate() ?: Date(),
            updatedAt = (map["updatedAt"] as? Timestamp)?.toDate()
        )
    }
}

data class DrawingPoint(
    val x: Float = 0f,
    val y: Float = 0f
) {
    fun toMap(): Map<String, Any> = mapOf("x" to x.toDouble(), "y" to y.toDouble())

    companion object {
        fun fromMap(map: Map<String, Any?>): DrawingPoint = DrawingPoint(
            x = (map["x"] as? Double)?.toFloat() ?: 0f,
            y = (map["y"] as? Double)?.toFloat() ?: 0f
        )
    }
}

data class DrawingStroke(
    val strokeId: String = "",
    val points: List<DrawingPoint> = emptyList(),
    /** Color ARGB int値（例: -16777216 = 黒） */
    val colorValue: Int = -16777216,
    val strokeWidth: Float = 4f,
    val createdAt: Date = Date(),
    val authorId: String = "",
    val authorName: String = ""
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "strokeId" to strokeId,
        "points" to points.map { it.toMap() },
        "colorValue" to colorValue.toLong(),
        "strokeWidth" to strokeWidth.toDouble(),
        "createdAt" to createdAt,
        "authorId" to authorId,
        "authorName" to authorName
    )

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): DrawingStroke = DrawingStroke(
            strokeId = map["strokeId"] as? String ?: "",
            points = (map["points"] as? List<*>)?.mapNotNull { entry ->
                (entry as? Map<String, Any?>)?.let { DrawingPoint.fromMap(it) }
            } ?: emptyList(),
            colorValue = (map["colorValue"] as? Long)?.toInt() ?: -16777216,
            strokeWidth = (map["strokeWidth"] as? Double)?.toFloat() ?: 4f,
            createdAt = (map["createdAt"] as? Timestamp)?.toDate() ?: Date(),
            authorId = map["authorId"] as? String ?: "",
            authorName = map["authorName"] as? String ?: ""
        )
    }
}
