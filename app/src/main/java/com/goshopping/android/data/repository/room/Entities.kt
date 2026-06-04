package com.goshopping.android.data.repository.room

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Room エンティティ: SharedGroup */
@Entity(tableName = "shared_groups")
data class SharedGroupEntity(
    @PrimaryKey val groupId: String,
    val groupName: String,
    val ownerUid: String,
    /** List<String> を JSON 文字列にシリアライズ */
    val allowedUidJson: String,
    /** List<MemberJson> を JSON 文字列にシリアライズ */
    val membersJson: String,
    val groupType: String,
    val syncStatus: String,
    val createdAt: Long,
    val updatedAt: Long?
)

/** Room エンティティ: SharedList（items は SharedItemEntity に別管理） */
@Entity(tableName = "shared_lists")
data class SharedListEntity(
    @PrimaryKey val listId: String,
    val listName: String,
    val ownerUid: String,
    val groupId: String,
    val groupName: String,
    val description: String,
    val listType: String,
    val createdAt: Long,
    val updatedAt: Long?
)

/** Room エンティティ: SharedItem */
@Entity(
    tableName = "shared_items",
    primaryKeys = ["itemId", "listId"]
)
data class SharedItemEntity(
    val itemId: String,
    val listId: String,
    val groupId: String,
    val name: String,
    val quantity: Int,
    val memberId: String,
    val registeredDate: Long,
    val purchaseDate: Long?,
    val isPurchased: Boolean,
    val isDeleted: Boolean,
    val deletedAt: Long?,
    val shoppingInterval: Int,
    val deadline: Long?
)
