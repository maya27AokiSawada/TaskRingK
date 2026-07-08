package net.sumomo_planning.taskringk.data.local.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "whiteboards",
    indices = [Index("groupId")],
)
data class WhiteboardEntity(
    @PrimaryKey val whiteboardId: String,
    val groupId: String,
    val ownerId: String?,
    val isPrivate: Boolean,
    val createdAt: Long,
    val updatedAt: Long?,
)
