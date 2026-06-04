package com.goshopping.android.data.repository.hybrid

import com.goshopping.android.data.model.ListType
import com.goshopping.android.data.model.SharedItem
import com.goshopping.android.data.model.SharedList
import com.goshopping.android.data.repository.firestore.FirestoreSharedListRepository
import com.goshopping.android.data.repository.local.LocalSharedListRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.Date

/**
 * HybridSharedListRepository のユニットテスト。
 *
 * 依存: testImplementation("io.mockk:mockk:1.13.x")
 *       testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.x.x")
 */
class HybridSharedListRepositoryTest {

    private lateinit var firestoreRepo: FirestoreSharedListRepository
    private lateinit var localRepo: LocalSharedListRepository
    private lateinit var sut: HybridSharedListRepository

    @Before
    fun setUp() {
        firestoreRepo = mockk()
        localRepo = mockk()
        sut = HybridSharedListRepository(firestoreRepo, localRepo)
    }

    // ―― createSharedList ――

    @Test
    fun `createSharedList calls both firestore and local repos`() = runTest {
        coEvery { firestoreRepo.createSharedList(any(), any(), any(), any(), any()) } returns makeList()
        coEvery { localRepo.createSharedList(any(), any(), any(), any(), any()) } returns makeList()

        sut.createSharedList("uid_owner", "group_1", "週末の買い物")

        coVerify(exactly = 1) {
            firestoreRepo.createSharedList("uid_owner", "group_1", "週末の買い物", null, null)
        }
        coVerify(exactly = 1) {
            localRepo.createSharedList("uid_owner", "group_1", "週末の買い物", null, null)
        }
    }

    @Test
    fun `createSharedList calls local repo even when firestore throws`() = runTest {
        val localList = makeList("list_local")
        coEvery { firestoreRepo.createSharedList(any(), any(), any(), any(), any()) } throws RuntimeException("network error")
        coEvery { localRepo.createSharedList(any(), any(), any(), any(), any()) } returns localList

        val result = sut.createSharedList("uid_owner", "group_1", "週末の買い物")

        coVerify(exactly = 1) { localRepo.createSharedList(any(), any(), any(), any(), any()) }
        assertEquals("list_local", result.listId)
    }

    // ―― getSharedListsByGroup ――

    @Test
    fun `getSharedListsByGroup returns firestore result and caches to local`() = runTest {
        val lists = listOf(makeList("list_1"), makeList("list_2"))
        coEvery { firestoreRepo.getSharedListsByGroup("group_1") } returns lists
        coEvery { localRepo.saveLists(any()) } just runs

        val result = sut.getSharedListsByGroup("group_1")

        assertEquals(lists, result)
        coVerify(exactly = 1) { localRepo.saveLists(lists) }
    }

    @Test
    fun `getSharedListsByGroup falls back to local when firestore throws`() = runTest {
        val localLists = listOf(makeList("list_local"))
        coEvery { firestoreRepo.getSharedListsByGroup("group_1") } throws RuntimeException("offline")
        coEvery { localRepo.getSharedListsByGroup("group_1") } returns localLists

        val result = sut.getSharedListsByGroup("group_1")

        assertEquals(localLists, result)
    }

    // ―― getSharedListById ――

    @Test
    fun `getSharedListById returns firestore result and saves to local`() = runTest {
        val list = makeList("list_1")
        coEvery { firestoreRepo.getSharedListById("group_1", "list_1") } returns list
        coEvery { localRepo.saveLists(listOf(list)) } just runs

        val result = sut.getSharedListById("group_1", "list_1")

        assertEquals(list, result)
        coVerify(exactly = 1) { localRepo.saveLists(listOf(list)) }
    }

    @Test
    fun `getSharedListById returns null from local when firestore returns null`() = runTest {
        coEvery { firestoreRepo.getSharedListById("group_1", "list_missing") } returns null
        coEvery { localRepo.getSharedListById("group_1", "list_missing") } returns null

        val result = sut.getSharedListById("group_1", "list_missing")

        assertNull(result)
        // null でも localRepo.saveLists は呼ばれない
        coVerify(exactly = 0) { localRepo.saveLists(any()) }
    }

    // ―― addSingleItem ――

    @Test
    fun `addSingleItem calls both repos with same item`() = runTest {
        val item = makeItem("item_1")
        coEvery { firestoreRepo.addSingleItem(any(), any(), any()) } just runs
        coEvery { localRepo.addSingleItem(any(), any(), any()) } just runs

        sut.addSingleItem("group_1", "list_1", item)

        coVerify(exactly = 1) { firestoreRepo.addSingleItem("group_1", "list_1", item) }
        coVerify(exactly = 1) { localRepo.addSingleItem("group_1", "list_1", item) }
    }

    @Test
    fun `addSingleItem calls local repo even when firestore throws`() = runTest {
        val item = makeItem("item_1")
        coEvery { firestoreRepo.addSingleItem(any(), any(), any()) } throws RuntimeException("network error")
        coEvery { localRepo.addSingleItem(any(), any(), any()) } just runs

        sut.addSingleItem("group_1", "list_1", item)

        coVerify(exactly = 1) { localRepo.addSingleItem("group_1", "list_1", item) }
    }

    // ―― updateSingleItem ――

    @Test
    fun `updateSingleItem calls both repos`() = runTest {
        val item = makeItem("item_1")
        coEvery { firestoreRepo.updateSingleItem(any(), any(), any()) } just runs
        coEvery { localRepo.updateSingleItem(any(), any(), any()) } just runs

        sut.updateSingleItem("group_1", "list_1", item)

        coVerify(exactly = 1) { firestoreRepo.updateSingleItem("group_1", "list_1", item) }
        coVerify(exactly = 1) { localRepo.updateSingleItem("group_1", "list_1", item) }
    }

    // ―― removeSingleItem (論理削除) ――

    @Test
    fun `removeSingleItem performs logical delete on both repos`() = runTest {
        // 仕様書 §12-2: 論理削除（isDeleted=true）
        coEvery { firestoreRepo.removeSingleItem(any(), any(), any()) } just runs
        coEvery { localRepo.removeSingleItem(any(), any(), any()) } just runs

        sut.removeSingleItem("group_1", "list_1", "item_1")

        coVerify(exactly = 1) { firestoreRepo.removeSingleItem("group_1", "list_1", "item_1") }
        coVerify(exactly = 1) { localRepo.removeSingleItem("group_1", "list_1", "item_1") }
    }

    @Test
    fun `removeSingleItem calls local repo even when firestore throws`() = runTest {
        coEvery { firestoreRepo.removeSingleItem(any(), any(), any()) } throws RuntimeException("network error")
        coEvery { localRepo.removeSingleItem(any(), any(), any()) } just runs

        sut.removeSingleItem("group_1", "list_1", "item_1")

        coVerify(exactly = 1) { localRepo.removeSingleItem("group_1", "list_1", "item_1") }
    }

    // ―― watchSharedList ――

    @Test
    fun `watchSharedList returns flow from firestore repo`() = runTest {
        val list = makeList("list_1")
        coEvery { firestoreRepo.watchSharedList("group_1", "list_1") } returns flowOf(list)

        val results = sut.watchSharedList("group_1", "list_1").toList()

        assertEquals(1, results.size)
        assertEquals(list, results.first())
    }

    // ―― deleteSharedList ――

    @Test
    fun `deleteSharedList calls both repos`() = runTest {
        coEvery { firestoreRepo.deleteSharedList(any(), any()) } just runs
        coEvery { localRepo.deleteSharedList(any(), any()) } just runs

        sut.deleteSharedList("group_1", "list_1")

        coVerify(exactly = 1) { firestoreRepo.deleteSharedList("group_1", "list_1") }
        coVerify(exactly = 1) { localRepo.deleteSharedList("group_1", "list_1") }
    }

    // ―― helpers ――

    private fun makeList(listId: String = "list_1") = SharedList(
        listId = listId,
        listName = "週末の買い物",
        ownerUid = "uid_owner",
        groupId = "group_1",
        groupName = "テストグループ",
        listType = ListType.SHOPPING,
        items = emptyMap(),
        createdAt = Date()
    )

    private fun makeItem(itemId: String) = SharedItem(
        itemId = itemId,
        name = "牛乳",
        quantity = 2,
        memberId = "member_1",
        registeredDate = Date()
    )
}
