package net.sumomo_planning.goshopping.data.mapper

import java.time.Instant
import kotlinx.serialization.json.Json
import net.sumomo_planning.goshopping.domain.model.ListType
import net.sumomo_planning.goshopping.domain.model.SharedItem
import net.sumomo_planning.goshopping.domain.model.SharedList
import org.junit.Assert.assertEquals
import org.junit.Test

class SharedListMappersTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun sharedList_roundTripsThroughEntity() {
        val item = SharedItem(
            itemId = "item-1",
            name = "Milk",
            quantity = 2,
            memberId = "member-1",
            registeredDate = Instant.parse("2026-06-30T08:00:00Z"),
            purchaseDate = Instant.parse("2026-06-30T09:00:00Z"),
            isPurchased = true,
            shoppingInterval = 7,
            deadline = Instant.parse("2026-07-01T00:00:00Z"),
        )
        val list = SharedList(
            listId = "list-1",
            listName = "Weekly",
            ownerUid = "owner",
            groupId = "group-1",
            groupName = "Family",
            description = "desc",
            listType = ListType.TODO,
            items = mapOf(item.itemId to item),
            createdAt = Instant.parse("2026-06-30T00:00:00Z"),
            updatedAt = Instant.parse("2026-06-30T10:00:00Z"),
        )

        val restored = list.toEntity(json).toDomain(json)

        assertEquals(list, restored)
    }
}
