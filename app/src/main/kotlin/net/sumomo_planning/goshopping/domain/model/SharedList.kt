package net.sumomo_planning.goshopping.domain.model

import java.time.Instant

data class SharedList(
    val listId: String,
    val listName: String,
    val ownerUid: String,
    val groupId: String,
    val groupName: String,
    val description: String = "",
    val listType: ListType = ListType.SHOPPING,
    val items: Map<String, SharedItem> = emptyMap(),
    val createdAt: Instant,
    val updatedAt: Instant? = null,
) {
    val activeItems: List<SharedItem>
        get() = items.values
            .filter { !it.isDeleted }
            .sortedBy { it.registeredDate }
}

enum class ListType { SHOPPING, TODO }
