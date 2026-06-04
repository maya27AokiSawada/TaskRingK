package com.goshopping.android.data.model

import com.google.firebase.Timestamp
import java.util.Date

enum class ListType {
    SHOPPING, TODO;

    fun toFirestoreValue(): String = name.lowercase()

    companion object {
        fun fromFirestoreValue(value: String): ListType =
            if (value.lowercase() == "todo") TODO else SHOPPING
    }
}

data class SharedItem(
    val itemId: String = "",
    val name: String = "",
    val quantity: Int = 1,
    val memberId: String = "",
    val registeredDate: Date = Date(),
    val purchaseDate: Date? = null,
    val isPurchased: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Date? = null,
    val shoppingInterval: Int = 0,
    val deadline: Date? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "itemId" to itemId,
        "name" to name,
        "quantity" to quantity,
        "memberId" to memberId,
        "registeredDate" to registeredDate,
        "purchaseDate" to purchaseDate,
        "isPurchased" to isPurchased,
        "isDeleted" to isDeleted,
        "deletedAt" to deletedAt,
        "shoppingInterval" to shoppingInterval,
        "deadline" to deadline
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): SharedItem = SharedItem(
            itemId = map["itemId"] as? String ?: "",
            name = map["name"] as? String ?: "",
            quantity = (map["quantity"] as? Long)?.toInt() ?: 1,
            memberId = map["memberId"] as? String ?: "",
            registeredDate = (map["registeredDate"] as? Timestamp)?.toDate() ?: Date(),
            purchaseDate = (map["purchaseDate"] as? Timestamp)?.toDate(),
            isPurchased = map["isPurchased"] as? Boolean ?: false,
            isDeleted = map["isDeleted"] as? Boolean ?: false,
            deletedAt = (map["deletedAt"] as? Timestamp)?.toDate(),
            shoppingInterval = (map["shoppingInterval"] as? Long)?.toInt() ?: 0,
            deadline = (map["deadline"] as? Timestamp)?.toDate()
        )
    }
}

data class SharedList(
    val listId: String = "",
    val listName: String = "",
    val ownerUid: String = "",
    val groupId: String = "",
    val groupName: String = "",
    val description: String = "",
    val listType: ListType = ListType.SHOPPING,
    val items: Map<String, SharedItem> = emptyMap(),
    val createdAt: Date = Date(),
    val updatedAt: Date? = null
) {
    /** isDeleted=false のアイテムのみを登録日時順で返す（UI表示用） */
    val activeItems: List<SharedItem>
        get() = items.values.filter { !it.isDeleted }.sortedBy { it.registeredDate }

    fun toMap(): Map<String, Any?> = mapOf(
        "listId" to listId,
        "listName" to listName,
        "ownerUid" to ownerUid,
        "groupId" to groupId,
        "groupName" to groupName,
        "description" to description,
        "listType" to listType.toFirestoreValue(),
        "items" to items.mapValues { (_, item) -> item.toMap() },
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): SharedList {
            val rawItems = map["items"] as? Map<String, Any?> ?: emptyMap()
            val parsedItems = rawItems.entries.associate { (key, value) ->
                key to SharedItem.fromMap(value as? Map<String, Any?> ?: emptyMap())
            }
            return SharedList(
                listId = map["listId"] as? String ?: "",
                listName = map["listName"] as? String ?: "",
                ownerUid = map["ownerUid"] as? String ?: "",
                groupId = map["groupId"] as? String ?: "",
                groupName = map["groupName"] as? String ?: "",
                description = map["description"] as? String ?: "",
                listType = ListType.fromFirestoreValue(map["listType"] as? String ?: "shopping"),
                items = parsedItems,
                createdAt = (map["createdAt"] as? Timestamp)?.toDate() ?: Date(),
                updatedAt = (map["updatedAt"] as? Timestamp)?.toDate()
            )
        }
    }
}
