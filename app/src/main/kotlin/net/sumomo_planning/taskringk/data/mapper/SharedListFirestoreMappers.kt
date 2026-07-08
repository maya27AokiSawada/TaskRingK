package net.sumomo_planning.taskringk.data.mapper

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import java.time.Instant
import net.sumomo_planning.taskringk.data.remote.dto.SharedItemDto
import net.sumomo_planning.taskringk.data.remote.dto.SharedListDto
import net.sumomo_planning.taskringk.domain.model.SharedItem
import net.sumomo_planning.taskringk.domain.model.SharedList

// ---- Firestore DTO → Domain ----

fun SharedListDto.toDomain(): SharedList = SharedList(
    listId = listId.ifBlank { documentId },
    listName = listName,
    ownerUid = ownerUid,
    groupId = groupId,
    groupName = groupName,
    description = description,
    listType = listType.toListType(),
    listKind = listKind.toListKind(),
    items = items.mapValues { it.value.toDomain() },
    createdAt = createdAt?.toDate()?.toInstant() ?: Instant.now(),
    updatedAt = updatedAt?.toDate()?.toInstant(),
)

fun SharedItemDto.toDomain(): SharedItem = SharedItem(
    itemId = itemId,
    name = name,
    quantity = quantity,
    memberId = memberId,
    registeredDate = registeredDate?.toDate()?.toInstant() ?: Instant.now(),
    purchaseDate = purchaseDate?.toDate()?.toInstant(),
    isPurchased = isPurchased,
    isDeleted = isDeleted,
    deletedAt = deletedAt?.toDate()?.toInstant(),
    shoppingInterval = shoppingInterval,
    deadline = deadline?.toDate()?.toInstant(),
)

// ---- Domain → Firestore map (used for set() writes) ----

/**
 * Converts a [SharedList] to a Firestore-compatible map for initial creation.
 * Uses [FieldValue.serverTimestamp] for timestamps.
 */
fun SharedList.toFirestoreMap(): Map<String, Any?> = mapOf(
    "listId" to listId,
    "listName" to listName,
    "ownerUid" to ownerUid,
    "groupId" to groupId,
    "groupName" to groupName,
    "description" to description,
    "listType" to listType.storageValue,
    "listKind" to listKind.storageValue,
    "items" to items.mapValues { it.value.toItemMap() },
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp(),
)

/**
 * Converts a [SharedItem] to a Firestore-compatible map.
 * Used for partial update: `items.{itemId}` (porting_spec §12-1).
 */
fun SharedItem.toItemMap(): Map<String, Any?> = mapOf(
    "itemId" to itemId,
    "name" to name,
    "quantity" to quantity,
    "memberId" to memberId,
    "registeredDate" to Timestamp(registeredDate.epochSecond, registeredDate.nano),
    "purchaseDate" to purchaseDate?.let { Timestamp(it.epochSecond, it.nano) },
    "isPurchased" to isPurchased,
    "isDeleted" to isDeleted,
    "deletedAt" to deletedAt?.let { Timestamp(it.epochSecond, it.nano) },
    "shoppingInterval" to shoppingInterval,
    "deadline" to deadline?.let { Timestamp(it.epochSecond, it.nano) },
)
