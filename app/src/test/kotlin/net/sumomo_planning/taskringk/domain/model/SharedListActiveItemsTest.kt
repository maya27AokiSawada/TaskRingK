package net.sumomo_planning.taskringk.domain.model

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class SharedListActiveItemsTest {

    @Test
    fun activeItems_excludesDeletedItemsAndSortsByRegisteredDate() {
        val later = item("later", Instant.parse("2026-06-30T10:00:00Z"))
        val deleted = item("deleted", Instant.parse("2026-06-30T09:00:00Z"), isDeleted = true)
        val earlier = item("earlier", Instant.parse("2026-06-30T08:00:00Z"))
        val list = SharedList(
            listId = "list-1",
            listName = "Weekly",
            ownerUid = "owner",
            groupId = "group-1",
            groupName = "Family",
            items = mapOf(
                later.itemId to later,
                deleted.itemId to deleted,
                earlier.itemId to earlier,
            ),
            createdAt = Instant.parse("2026-06-30T00:00:00Z"),
        )

        assertEquals(listOf("earlier", "later"), list.activeItems.map { it.itemId })
    }

    private fun item(
        id: String,
        registeredDate: Instant,
        isDeleted: Boolean = false,
    ) = SharedItem(
        itemId = id,
        name = id,
        memberId = "member-1",
        registeredDate = registeredDate,
        isDeleted = isDeleted,
    )
}
