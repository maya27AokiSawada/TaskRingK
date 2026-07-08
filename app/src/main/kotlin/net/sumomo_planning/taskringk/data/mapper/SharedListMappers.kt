package net.sumomo_planning.taskringk.data.mapper

import java.time.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.sumomo_planning.taskringk.data.local.dto.LocalSharedItemDto
import net.sumomo_planning.taskringk.data.local.room.entity.SharedListEntity
import net.sumomo_planning.taskringk.domain.model.SharedItem
import net.sumomo_planning.taskringk.domain.model.SharedList

fun SharedListEntity.toDomain(json: Json): SharedList = SharedList(
    listId = listId,
    listName = listName,
    ownerUid = ownerUid,
    groupId = groupId,
    groupName = groupName,
    description = description,
    listType = listType.toListType(),
    listKind = listKind.toListKind(),
    items = json.decodeFromString<Map<String, LocalSharedItemDto>>(itemsJson).mapValues { it.value.toDomain() },
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = updatedAt?.let(Instant::ofEpochMilli),
)

fun SharedList.toEntity(json: Json): SharedListEntity = SharedListEntity(
    listId = listId,
    listName = listName,
    ownerUid = ownerUid,
    groupId = groupId,
    groupName = groupName,
    description = description,
    listType = listType.storageValue,
    listKind = listKind.storageValue,
    itemsJson = json.encodeToString(items.mapValues { it.value.toLocalDto() }),
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt?.toEpochMilli(),
)

private fun LocalSharedItemDto.toDomain(): SharedItem = SharedItem(
    itemId = itemId,
    name = name,
    quantity = quantity,
    memberId = memberId,
    registeredDate = Instant.ofEpochMilli(registeredDate),
    purchaseDate = purchaseDate?.let(Instant::ofEpochMilli),
    isPurchased = isPurchased,
    isDeleted = isDeleted,
    deletedAt = deletedAt?.let(Instant::ofEpochMilli),
    shoppingInterval = shoppingInterval,
    deadline = deadline?.let(Instant::ofEpochMilli),
)

private fun SharedItem.toLocalDto(): LocalSharedItemDto = LocalSharedItemDto(
    itemId = itemId,
    name = name,
    quantity = quantity,
    memberId = memberId,
    registeredDate = registeredDate.toEpochMilli(),
    purchaseDate = purchaseDate?.toEpochMilli(),
    isPurchased = isPurchased,
    isDeleted = isDeleted,
    deletedAt = deletedAt?.toEpochMilli(),
    shoppingInterval = shoppingInterval,
    deadline = deadline?.toEpochMilli(),
)
