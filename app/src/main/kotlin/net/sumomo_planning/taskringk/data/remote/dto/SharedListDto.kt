package net.sumomo_planning.taskringk.data.remote.dto

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Firestore DTO for the SharedList document at
 * /SharedGroups/{groupId}/sharedLists/{listId}.
 *
 * All fields have defaults so Firestore toObject() never throws on absent fields.
 */
@Keep
data class SharedListDto(
    @DocumentId val documentId: String = "",
    val listId: String = "",
    val listName: String = "",
    val ownerUid: String = "",
    val groupId: String = "",
    val groupName: String = "",
    val description: String = "",
    val listType: String = "shopping",
    val listKind: String = "shoppingList",
    val items: Map<String, SharedItemDto> = emptyMap(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
)

/**
 * Firestore DTO for an item embedded in the SharedList `items` map.
 */
@Keep
data class SharedItemDto(
    val itemId: String = "",
    val name: String = "",
    val quantity: Int = 1,
    val memberId: String = "",
    val registeredDate: Timestamp? = null,
    val purchaseDate: Timestamp? = null,
    val isPurchased: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Timestamp? = null,
    val shoppingInterval: Int = 0,
    val deadline: Timestamp? = null,
)
