package net.sumomo_planning.taskringk.data.local.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shared_lists",
    indices = [Index("groupId")],
)
data class SharedListEntity(
    @PrimaryKey val listId: String,
    val listName: String,
    val ownerUid: String,
    val groupId: String,
    val groupName: String,
    val description: String,
    val listType: String,
    val listKind: String,
    val itemsJson: String,
    val createdAt: Long,
    val updatedAt: Long?,
)
