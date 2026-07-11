package net.sumomo_planning.taskringk.data.repository

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import net.sumomo_planning.taskringk.core.common.DeviceIdService
import net.sumomo_planning.taskringk.core.network.NetworkMonitor
import net.sumomo_planning.taskringk.data.local.room.dao.SharedGroupDao
import net.sumomo_planning.taskringk.data.local.room.dao.SharedListDao
import net.sumomo_planning.taskringk.data.local.room.entity.SharedGroupEntity
import net.sumomo_planning.taskringk.data.local.room.entity.SharedListEntity
import net.sumomo_planning.taskringk.data.remote.firestore.FirestoreSharedListDataSource
import net.sumomo_planning.taskringk.domain.model.ListKind
import net.sumomo_planning.taskringk.domain.model.ListType
import net.sumomo_planning.taskringk.domain.model.SharedItem
import net.sumomo_planning.taskringk.domain.model.SharedList
import net.sumomo_planning.taskringk.domain.repository.NotificationRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HybridSharedListRepositoryImplTest {

    private val firestoreDataSource = mockk<FirestoreSharedListDataSource>()
    private val sharedListDao = mockk<SharedListDao>(relaxed = true)
    private val sharedGroupDao = mockk<SharedGroupDao>(relaxed = true)
    private val deviceIdService = mockk<DeviceIdService>()
    private val networkMonitor = mockk<NetworkMonitor>()
    private val notificationRepository = mockk<NotificationRepository>(relaxed = true)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val groupId = "grp_001"
    private val listId = "lst_abc12345"
    private val ownerUid = "user-uid-1"

    private val fakeList = SharedList(
        listId = listId,
        listName = "週末の買い物",
        ownerUid = ownerUid,
        groupId = groupId,
        groupName = "家族",
        description = "",
        listType = ListType.SHOPPING,
        listKind = ListKind.SHOPPING_LIST,
        items = emptyMap(),
        createdAt = Instant.EPOCH,
        updatedAt = null,
    )

    private val fakeEntity = SharedListEntity(
        listId = listId,
        listName = "週末の買い物",
        ownerUid = ownerUid,
        groupId = groupId,
        groupName = "家族",
        description = "",
        listType = "shopping",
        listKind = "shoppingList",
        itemsJson = "{}",
        createdAt = 0L,
        updatedAt = null,
    )

    private val fakeGroupEntity = SharedGroupEntity(
        groupId = groupId,
        groupName = "家族",
        ownerUid = ownerUid,
        allowedUid = listOf(ownerUid),
        membersJson = "[]",
        groupType = "shopping",
        syncStatus = "synced",
        createdAt = 0L,
        updatedAt = null,
    )

    private lateinit var repository: HybridSharedListRepositoryImpl

    @Before
    fun setUp() {
        // default: online
        every { networkMonitor.isOnlineFlow } returns flowOf(true)
        every { networkMonitor.isOnline } returns true
        every { sharedGroupDao.observeById(groupId) } returns flowOf(fakeGroupEntity)
        every { sharedListDao.observeById(listId) } returns flowOf(fakeEntity)

        repository = HybridSharedListRepositoryImpl(
            firestoreDataSource = firestoreDataSource,
            sharedListDao = sharedListDao,
            sharedGroupDao = sharedGroupDao,
            deviceIdService = deviceIdService,
            networkMonitor = networkMonitor,
            notificationRepository = notificationRepository,
            json = json,
        )
    }

    // ---- observeListsByGroup ----

    @Test
    fun `observeListsByGroup emits Firestore data and caches to Room`() = runTest {
        every { firestoreDataSource.observeByGroup(groupId) } returns flowOf(listOf(fakeList))

        repository.observeListsByGroup(groupId).test {
            val lists = awaitItem()
            assertEquals(1, lists.size)
            assertEquals(listId, lists[0].listId)
            coVerify { sharedListDao.upsertAll(any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeListsByGroup falls back to Room on Firestore error`() = runTest {
        every { firestoreDataSource.observeByGroup(groupId) } returns flow {
            throw RuntimeException("Firestore unavailable")
        }
        every { sharedListDao.observeByGroup(groupId) } returns flowOf(listOf(fakeEntity))

        repository.observeListsByGroup(groupId).test {
            val lists = awaitItem()
            assertEquals(1, lists.size)
            assertEquals(listId, lists[0].listId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ---- createList ----

    @Test
    fun `createList writes to Firestore and Room`() = runTest {
        val newListId = "new_00112233"
        every { deviceIdService.generateListId(any()) } returns newListId
        coJustRun { firestoreDataSource.createList(any(), any()) }

        val result = repository.createList(
            groupId = groupId,
            groupName = "家族",
            listName = "新しいリスト",
            description = "",
            ownerUid = ownerUid,
        )

        assertTrue(result.isSuccess)
        assertEquals(newListId, result.getOrThrow().listId)
        coVerify { firestoreDataSource.createList(eq(groupId), any()) }
        coVerify { sharedListDao.upsert(any()) }
    }

    @Test
    fun `createList continues to Room when Firestore fails`() = runTest {
        val newListId = "new_00112233"
        every { deviceIdService.generateListId(any()) } returns newListId
        coEvery { firestoreDataSource.createList(any(), any()) } throws RuntimeException("net error")

        val result = repository.createList(
            groupId = groupId,
            groupName = "家族",
            listName = "新しいリスト",
            description = "",
            ownerUid = ownerUid,
        )

        assertTrue(result.isSuccess)
        coVerify { sharedListDao.upsert(any()) }
    }

    // ---- deleteList ----

    @Test
    fun `deleteList removes from Firestore then Room in order`() = runTest {
        coJustRun { firestoreDataSource.deleteList(any(), any()) }

        val result = repository.deleteList(groupId, listId)

        assertTrue(result.isSuccess)
        coVerifyOrder {
            firestoreDataSource.deleteList(groupId, listId)
            sharedListDao.deleteById(listId)
        }
    }

    @Test
    fun `deleteList removes from Room even when Firestore fails`() = runTest {
        coEvery { firestoreDataSource.deleteList(any(), any()) } throws RuntimeException("net error")

        val result = repository.deleteList(groupId, listId)

        assertTrue(result.isSuccess)
        coVerify { sharedListDao.deleteById(listId) }
    }

    // ---- addOrUpdateItem ----

    @Test
    fun `addOrUpdateItem performs partial Firestore update and updates Room cache`() = runTest {
        val item = SharedItem(
            itemId = "item-001",
            name = "牛乳",
            quantity = 2,
            memberId = ownerUid,
            registeredDate = Instant.EPOCH,
        )
        coJustRun { firestoreDataSource.addOrUpdateItem(any(), any(), any()) }
        every { sharedListDao.observeById(listId) } returns flowOf(fakeEntity)

        val result = repository.addOrUpdateItem(groupId, listId, item)

        assertTrue(result.isSuccess)
        coVerify { firestoreDataSource.addOrUpdateItem(groupId, listId, item) }
        coVerify { sharedListDao.upsert(any()) }
    }

    @Test
    fun `addOrUpdateItem continues to update Room when Firestore fails`() = runTest {
        val item = SharedItem(
            itemId = "item-001",
            name = "牛乳",
            quantity = 1,
            memberId = ownerUid,
            registeredDate = Instant.EPOCH,
        )
        coEvery { firestoreDataSource.addOrUpdateItem(any(), any(), any()) } throws RuntimeException("net error")
        every { sharedListDao.observeById(listId) } returns flowOf(fakeEntity)

        val result = repository.addOrUpdateItem(groupId, listId, item)

        assertTrue(result.isSuccess)
        coVerify { sharedListDao.upsert(any()) }
    }

    // ---- removeItem ----

    @Test
    fun `removeItem performs logical delete in Firestore and Room`() = runTest {
        val itemId = "item-001"
        val existingItem = SharedItem(
            itemId = itemId,
            name = "牛乳",
            quantity = 1,
            memberId = ownerUid,
            registeredDate = Instant.EPOCH,
        )
        val entityWithItem = fakeEntity.copy(
            itemsJson = json.encodeToString(
                kotlinx.serialization.serializer(),
                mapOf(itemId to net.sumomo_planning.taskringk.data.local.dto.LocalSharedItemDto(
                    itemId = itemId,
                    name = "牛乳",
                    quantity = 1,
                    memberId = ownerUid,
                    registeredDate = 0L,
                    purchaseDate = null,
                    isPurchased = false,
                    isDeleted = false,
                    deletedAt = null,
                    shoppingInterval = 0,
                    deadline = null,
                ))
            )
        )
        coJustRun { firestoreDataSource.removeItem(any(), any(), any()) }
        every { sharedListDao.observeById(listId) } returns flowOf(entityWithItem)

        val result = repository.removeItem(groupId, listId, itemId)

        assertTrue(result.isSuccess)
        coVerify { firestoreDataSource.removeItem(groupId, listId, itemId) }
        coVerify { sharedListDao.upsert(match { entity ->
            json.decodeFromString<Map<String, net.sumomo_planning.taskringk.data.local.dto.LocalSharedItemDto>>(entity.itemsJson)[itemId]?.isDeleted == true
        }) }
    }

    @Test
    fun `removeItem continues Room update when Firestore fails`() = runTest {
        val itemId = "item-001"
        val entityWithItem = fakeEntity.copy(
            itemsJson = json.encodeToString(
                kotlinx.serialization.serializer(),
                mapOf(itemId to net.sumomo_planning.taskringk.data.local.dto.LocalSharedItemDto(
                    itemId = itemId, name = "X", quantity = 1, memberId = ownerUid,
                    registeredDate = 0L, purchaseDate = null, isPurchased = false,
                    isDeleted = false, deletedAt = null, shoppingInterval = 0, deadline = null,
                ))
            )
        )
        coEvery { firestoreDataSource.removeItem(any(), any(), any()) } throws RuntimeException("net error")
        every { sharedListDao.observeById(listId) } returns flowOf(entityWithItem)

        val result = repository.removeItem(groupId, listId, itemId)

        assertTrue(result.isSuccess)
        coVerify { sharedListDao.upsert(any()) }
    }

    // ---- Phase 5: network-aware tests ----

    @Test
    fun `observeListsByGroup uses Room directly when offline`() = runTest {
        every { networkMonitor.isOnlineFlow } returns flowOf(false)
        every { sharedListDao.observeByGroup(groupId) } returns flowOf(listOf(fakeEntity))

        repository.observeListsByGroup(groupId).test {
            val lists = awaitItem()
            assertEquals(1, lists.size)
            assertEquals(listId, lists[0].listId)
            // Firestore should NOT be called when offline
            coVerify(exactly = 0) { firestoreDataSource.observeByGroup(any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeListsByGroup switches to Firestore when going online`() = runTest {
        val networkFlow = kotlinx.coroutines.flow.MutableStateFlow(false)
        every { networkMonitor.isOnlineFlow } returns networkFlow
        every { sharedListDao.observeByGroup(groupId) } returns flowOf(listOf(fakeEntity))
        every { firestoreDataSource.observeByGroup(groupId) } returns flowOf(listOf(fakeList))

        repository.observeListsByGroup(groupId).test {
            // initially offline → Room
            val offlineLists = awaitItem()
            assertEquals(listId, offlineLists[0].listId)

            // go online → Firestore
            networkFlow.value = true
            val onlineLists = awaitItem()
            assertEquals(listId, onlineLists[0].listId)
            coVerify { firestoreDataSource.observeByGroup(groupId) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeList emits Firestore data and caches to Room`() = runTest {
        every { firestoreDataSource.observeList(groupId, listId) } returns flowOf(fakeList)

        repository.observeList(groupId, listId).test {
            val list = awaitItem()
            assertEquals(listId, list?.listId)
            coVerify { sharedListDao.upsert(any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeList falls back to Room when Firestore errors`() = runTest {
        every { firestoreDataSource.observeList(groupId, listId) } returns flow {
            throw RuntimeException("Firestore unavailable")
        }
        every { sharedListDao.observeById(listId) } returns flowOf(fakeEntity)

        repository.observeList(groupId, listId).test {
            val list = awaitItem()
            assertEquals(listId, list?.listId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeList uses Room directly when offline`() = runTest {
        every { networkMonitor.isOnlineFlow } returns flowOf(false)
        every { sharedListDao.observeById(listId) } returns flowOf(fakeEntity)

        repository.observeList(groupId, listId).test {
            val list = awaitItem()
            assertEquals(listId, list?.listId)
            coVerify(exactly = 0) { firestoreDataSource.observeList(any(), any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeList switches source when connectivity changes`() = runTest {
        val networkFlow = kotlinx.coroutines.flow.MutableStateFlow(false)
        every { networkMonitor.isOnlineFlow } returns networkFlow
        every { sharedListDao.observeById(listId) } returns flowOf(fakeEntity)
        every { firestoreDataSource.observeList(groupId, listId) } returns flowOf(fakeList)

        repository.observeList(groupId, listId).test {
            val offlineList = awaitItem()
            assertEquals(listId, offlineList?.listId)

            networkFlow.value = true
            val onlineList = awaitItem()
            assertEquals(listId, onlineList?.listId)
            coVerify { firestoreDataSource.observeList(groupId, listId) }
            cancelAndIgnoreRemainingEvents()
        }
    }
}
