package com.goshopping.android.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class SharedListModelTest {

    // ―― activeItems ――

    @Test
    fun `activeItems returns only non-deleted items`() {
        val list = makeListWithItems(
            active = listOf("item_a", "item_b"),
            deleted = listOf("item_c")
        )
        val active = list.activeItems
        assertEquals(2, active.size)
        assertTrue(active.none { it.isDeleted })
    }

    @Test
    fun `activeItems returns empty when all items are deleted`() {
        val list = makeListWithItems(active = emptyList(), deleted = listOf("item_x"))
        assertTrue(list.activeItems.isEmpty())
    }

    @Test
    fun `activeItems is sorted by registeredDate ascending`() {
        val now = System.currentTimeMillis()
        val item1 = makeItem("item_1", registeredDate = Date(now - 2000))
        val item2 = makeItem("item_2", registeredDate = Date(now - 1000))
        val item3 = makeItem("item_3", registeredDate = Date(now))
        val list = SharedList(
            listId = "list_1",
            items = mapOf(
                item3.itemId to item3,
                item1.itemId to item1,
                item2.itemId to item2
            )
        )
        val active = list.activeItems
        assertEquals("item_1", active[0].itemId)
        assertEquals("item_2", active[1].itemId)
        assertEquals("item_3", active[2].itemId)
    }

    // ―― ListType enum ――

    @Test
    fun `ListType fromFirestoreValue round-trip`() {
        assertEquals(ListType.SHOPPING, ListType.fromFirestoreValue("shopping"))
        assertEquals(ListType.TODO, ListType.fromFirestoreValue("todo"))
        assertEquals(ListType.SHOPPING, ListType.fromFirestoreValue("unknown"))
    }

    // ―― SharedItem toMap / fromMap ――

    @Test
    fun `SharedItem toMap contains all required fields`() {
        val item = makeItem("item_1")
        val map = item.toMap()
        assertEquals("item_1", map["itemId"])
        assertEquals("牛乳", map["name"])
        assertEquals(2, map["quantity"])
        assertFalse(map["isDeleted"] as Boolean)
    }

    @Test
    fun `SharedItem fromMap uses defaults for missing keys`() {
        val item = SharedItem.fromMap(emptyMap())
        assertEquals("", item.itemId)
        assertEquals(1, item.quantity)
        assertFalse(item.isPurchased)
        assertFalse(item.isDeleted)
        assertEquals(0, item.shoppingInterval)
    }

    @Test
    fun `SharedItem fromMap parses Long quantity correctly`() {
        val map = mapOf<String, Any?>("quantity" to 5L)
        val item = SharedItem.fromMap(map)
        assertEquals(5, item.quantity)
    }

    // ―― helpers ――

    private fun makeItem(
        id: String,
        isDeleted: Boolean = false,
        registeredDate: Date = Date()
    ) = SharedItem(
        itemId = id,
        name = "牛乳",
        quantity = 2,
        memberId = "member_1",
        registeredDate = registeredDate,
        isDeleted = isDeleted
    )

    private fun makeListWithItems(
        active: List<String>,
        deleted: List<String>
    ): SharedList {
        val items = mutableMapOf<String, SharedItem>()
        active.forEach { id -> items[id] = makeItem(id, isDeleted = false) }
        deleted.forEach { id -> items[id] = makeItem(id, isDeleted = true) }
        return SharedList(listId = "list_1", items = items)
    }
}
