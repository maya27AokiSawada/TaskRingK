package net.sumomo_planning.taskringk.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shared_groups")
data class SharedGroupEntity(
    @PrimaryKey val groupId: String,
    val groupName: String,
    val ownerUid: String,
    val allowedUid: List<String>,
    val membersJson: String,
    val groupType: String,
    val syncStatus: String,
    val createdAt: Long,
    val updatedAt: Long?,
)
