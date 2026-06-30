package net.sumomo_planning.goshopping.data.mapper

import java.time.Instant
import net.sumomo_planning.goshopping.data.local.room.entity.WhiteboardEntity
import net.sumomo_planning.goshopping.domain.model.Whiteboard

fun WhiteboardEntity.toDomain(): Whiteboard = Whiteboard(
    whiteboardId = whiteboardId,
    groupId = groupId,
    ownerId = ownerId,
    isPrivate = isPrivate,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = updatedAt?.let(Instant::ofEpochMilli),
)

fun Whiteboard.toEntity(): WhiteboardEntity = WhiteboardEntity(
    whiteboardId = whiteboardId,
    groupId = groupId,
    ownerId = ownerId,
    isPrivate = isPrivate,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt?.toEpochMilli(),
)
