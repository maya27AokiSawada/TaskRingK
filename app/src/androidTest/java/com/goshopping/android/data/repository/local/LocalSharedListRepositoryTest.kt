package com.goshopping.android.data.repository.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.goshopping.android.data.model.ListType
import com.goshopping.android.data.model.SharedItem
import com.goshopping.android.data.model.SharedList
import com.goshopping.android.data.repository.room.AppDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/**
 * LocalSharedListRepository のインスツルメントテスト（Room インメモリDB使用）。
 *
 * 依存: androidTestImplementation("androidx.test.ext:junit:1.x.x")
 *       androidTestImplementation("androidx.test:runner:1.x.x")
 *       androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.x.x")
 */
@RunWith(AndroidJUnit4::class)
class LocalSharedListRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var sut: LocalSharedListRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        sut = LocalSharedListRepository(db.sharedListDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ―― createSharedList ――

    @Test
    fun `createSharedList persists and retrieves the list`() = runTest {
        val created = sut.createSharedList(
            ownerUid = "uid_owner",
            groupId = "group_1",
            listName = "週末の買い物"
        )

        assertNotNull(created.listId)
        assertEquals("週末の買い物", created.listName)
        assertEquals("uid_owner", created.ownerUid)
        assertEquals("group_1", created.groupId)
    }

    @Test
    fun `createSharedList with customListId uses the provided id`() = runTest {
        val created = sut.createSharedList(
            ownerUid = "uid_owner",
            groupId = "group_1",
            listName = "カスタムリスト",
            customListId = "custom_list_id"
        )

        assertEquals("custom_list_id", created.listId)
    }

    // ―― getSharedListById ――

    @Test
    fun `getSharedListById returns the correct list`() = runTest {
        val created = sut.createSharedList("uid_owner", "group_1", "リストA", customListId = "list_1")

        val found = sut.getSharedListById("group_1", "list_1")

        assertNotNull(found)
        assertEquals("list_1", found!!.listId)
    }

    @Test
    fun `getSharedListById returns null for non-existent list`() = runTest {
        val result = sut.getSharedListById("group_1", "nonexistent_list")
        assertNull(result)
    }

    // ―― getSharedListsByGroup ――

    @Test
    fun `getSharedListsByGroup returns all lists for the group`() = runTest {
        sut.createSharedList("uid_owner", "group_1", "リストA")
        sut.createSharedList("uid_owner", "group_1", "リストB")
        sut.createSharedList("uid_owner", "group_2", "別グループのリスト")

        val lists = sut.getSharedListsByGroup("group_1")

        assertEquals(2, lists.size)
        assertTrue(lists.all { it.groupId == "group_1" })
    }

    // ―― addSingleItem ――

    @Test
    fun `addSingleItem persists the item into the list`() = runTest {
        sut.createSharedList("uid_owner", "group_1", "テストリスト", customListId = "list_1")
        val item = makeItem("item_1")

        sut.addSingleItem("group_1", "list_1", item)

        val list = sut.getSharedListById("group_1", "list_1")
        assertNotNull(list)
        assertNotNull(list!!.items["item_1"])
        assertEquals("牛乳", list.items["item_1"]!!.name)
    }

    @Test
    fun `addSingleItem does not replace other existing items`() = runTest {
        sut.createSharedList("uid_owner", "group_1", "テストリスト", customListId = "list_1")
        sut.addSingleItem("group_1", "list_1", makeItem("item_1"))
        sut.addSingleItem("group_1", "list_1", makeItem("item_2"))

        val list = sut.getSharedListById("group_1", "list_1")!!
        assertEquals(2, list.items.size)
        assertNotNull(list.items["item_1"])
        assertNotNull(list.items["item_2"])
    }

    // ―― updateSingleItem ――

    @Test
    fun `updateSingleItem overwrites the existing item`() = runTest {
        sut.createSharedList("uid_owner", "group_1", "テストリスト", customListId = "list_1")
        sut.addSingleItem("group_1", "list_1", makeItem("item_1", quantity = 1))
        val updated = makeItem("item_1", quantity = 5)

        sut.updateSingleItem("group_1", "list_1", updated)

        val list = sut.getSharedListById("group_1", "list_1")!!
        assertEquals(5, list.items["item_1"]!!.quantity)
    }

    // ―― removeSingleItem (論理削除) ――

    @Test
    fun `removeSingleItem sets isDeleted to true and does not physically remove`() = runTest {
        // 仕様書 §12-2: 論理削除（isDeleted=true）
        sut.createSharedList("uid_owner", "group_1", "テストリスト", customListId = "list_1")
        sut.addSingleItem("group_1", "list_1", makeItem("item_1"))

        sut.removeSingleItem("group_1", "list_1", "item_1")

        val list = sut.getSharedListById("group_1", "list_1")!!
        val item = list.items["item_1"]
        assertNotNull(item)
        assertTrue(item!!.isDeleted)
        assertNotNull(item.deletedAt)
    }

    @Test
    fun `removeSingleItem item does not appear in activeItems`() = runTest {
        sut.createSharedList("uid_owner", "group_1", "テストリスト", customListId = "list_1")
        sut.addSingleItem("group_1", "list_1", makeItem("item_1"))
        sut.addSingleItem("group_1", "list_1", makeItem("item_2"))

        sut.removeSingleItem("group_1", "list_1", "item_1")

        val list = sut.getSharedListById("group_1", "list_1")!!
        assertEquals(1, list.activeItems.size)
        assertEquals("item_2", list.activeItems.first().itemId)
    }

    // ―― deleteSharedList ――

    @Test
    fun `deleteSharedList removes the list and its items`() = runTest {
        sut.createSharedList("uid_owner", "group_1", "削除リスト", customListId = "list_1")
        sut.addSingleItem("group_1", "list_1", makeItem("item_1"))

        sut.deleteSharedList("group_1", "list_1")

        assertNull(sut.getSharedListById("group_1", "list_1"))
    }

    // ―― deleteGroupData ――

    @Test
    fun `deleteGroupData removes all lists and items for the group`() = runTest {
        sut.createSharedList("uid_owner", "group_1", "リストA", customListId = "list_a")
        sut.createSharedList("uid_owner", "group_1", "リストB", customListId = "list_b")
        sut.addSingleItem("group_1", "list_a", makeItem("item_1"))
        // 別グループのリストは削除されないこと
        sut.createSharedList("uid_owner", "group_2", "別グループ", customListId = "list_other")

        sut.deleteGroupData("group_1")

        assertTrue(sut.getSharedListsByGroup("group_1").isEmpty())
        assertEquals(1, sut.getSharedListsByGroup("group_2").size)
    }

    // ―― saveLists ――

    @Test
    fun `saveLists upserts lists and their items`() = runTest {
        val item = makeItem("item_1")
        val list = makeListWithItems("list_1", "group_1", mapOf(item.itemId to item))

        sut.saveLists(listOf(list))

        val fetched = sut.getSharedListById("group_1", "list_1")
        assertNotNull(fetched)
        assertNotNull(fetched!!.items["item_1"])
    }

    // ―― helpers ――

    private fun makeItem(id: String, quantity: Int = 2) = SharedItem(
        itemId = id,
        name = "牛乳",
        quantity = quantity,
        memberId = "member_1",
        registeredDate = Date()
    )

    private fun makeListWithItems(
        listId: String,
        groupId: String,
        items: Map<String, SharedItem> = emptyMap()
    ) = SharedList(
        listId = listId,
        listName = "テストリスト",
        ownerUid = "uid_owner",
        groupId = groupId,
        groupName = "テストグループ",
        listType = ListType.SHOPPING,
        items = items,
        createdAt = Date()
    )
}
