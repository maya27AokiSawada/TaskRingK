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
import net.sumomo_planning.taskringk.data.remote.firestore.FirestoreSharedGroupDataSource
import net.sumomo_planning.taskringk.domain.model.GroupType
import net.sumomo_planning.taskringk.domain.model.SharedGroup
import net.sumomo_planning.taskringk.domain.model.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HybridSharedGroupRepositoryImplTest {

    private val firestoreDataSource = mockk<FirestoreSharedGroupDataSource>()
    private val sharedGroupDao = mockk<SharedGroupDao>(relaxed = true)
    private val sharedListDao = mockk<SharedListDao>(relaxed = true)
    private val deviceIdService = mockk<DeviceIdService>()
    private val networkMonitor = mockk<NetworkMonitor>()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val uid = "user-uid-1"
    private val groupId = "grp_001"

    private val fakeGroup = SharedGroup(
        groupId = groupId,
        groupName = "テストグループ",
        ownerUid = uid,
        allowedUid = listOf(uid),
        members = emptyList(),
        groupType = GroupType.SHOPPING,
        syncStatus = SyncStatus.SYNCED,
        createdAt = Instant.EPOCH,
        updatedAt = null,
    )

    private val fakeEntity = SharedGroupEntity(
        groupId = groupId,
        groupName = "テストグループ",
        ownerUid = uid,
        allowedUid = listOf(uid),
        membersJson = "[]",
        groupType = "shopping",
        syncStatus = "synced",
        createdAt = 0L,
        updatedAt = null,
    )

    private lateinit var repository: HybridSharedGroupRepositoryImpl

    @Before
    fun setUp() {
        // default: online
        every { networkMonitor.isOnlineFlow } returns flowOf(true)
        every { networkMonitor.isOnline } returns true

        repository = HybridSharedGroupRepositoryImpl(
            firestoreDataSource = firestoreDataSource,
            sharedGroupDao = sharedGroupDao,
            sharedListDao = sharedListDao,
            deviceIdService = deviceIdService,
            networkMonitor = networkMonitor,
            json = json,
        )
    }

    // ---- observeGroups ----

    @Test
    fun `observeGroups emits Firestore data and caches to Room`() = runTest {
        every { firestoreDataSource.observeGroups(uid) } returns flowOf(listOf(fakeGroup))

        repository.observeGroups(uid).test {
            val groups = awaitItem()
            assertEquals(1, groups.size)
            assertEquals(fakeGroup.groupId, groups[0].groupId)
            awaitComplete()
        }

        coVerify { sharedGroupDao.upsertAll(any()) }
    }

    @Test
    fun `observeGroups falls back to Room when Firestore fails`() = runTest {
        // Return a Flow that *emits* an error so the .catch operator can intercept it
        every { firestoreDataSource.observeGroups(uid) } returns flow { throw RuntimeException("network error") }
        every { sharedGroupDao.observeAll() } returns flowOf(listOf(fakeEntity))

        repository.observeGroups(uid).test {
            val groups = awaitItem()
            assertEquals(1, groups.size)
            assertEquals(groupId, groups[0].groupId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeGroups falls back to empty list from Room when both fail`() = runTest {
        every { firestoreDataSource.observeGroups(uid) } returns flow { throw RuntimeException("network error") }
        every { sharedGroupDao.observeAll() } returns flowOf(emptyList())

        repository.observeGroups(uid).test {
            val groups = awaitItem()
            assertTrue(groups.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ---- createGroup ----

    @Test
    fun `createGroup returns success and caches group to Room`() = runTest {
        every { deviceIdService.generateGroupId(any(), any()) } returns groupId
        every { deviceIdService.generateMemberId() } returns "member-1"
        coJustRun { firestoreDataSource.createGroup(any()) }

        val result = repository.createGroup(
            groupName = "新しいグループ",
            ownerUid = uid,
            ownerDisplayName = "テストユーザー",
            ownerEmail = "test@example.com",
        )

        // getOrThrow() exposes the underlying exception if result is failure
        assertEquals("新しいグループ", result.getOrThrow().groupName)
        coVerify { sharedGroupDao.upsert(any()) }
    }

    @Test
    fun `createGroup still caches locally when Firestore throws`() = runTest {
        every { deviceIdService.generateGroupId(any(), any()) } returns groupId
        every { deviceIdService.generateMemberId() } returns "member-1"
        coEvery { firestoreDataSource.createGroup(any()) } throws RuntimeException("offline")

        val result = repository.createGroup(
            groupName = "新しいグループ",
            ownerUid = uid,
            ownerDisplayName = "テストユーザー",
            ownerEmail = "test@example.com",
        )

        // Local write must succeed despite Firestore failure (isReturnDefaultValues=true stubs Log.w)
        assertEquals("新しいグループ", result.getOrThrow().groupName)
        coVerify { sharedGroupDao.upsert(any()) }
    }

    // ---- deleteGroup ----

    @Test
    fun `deleteGroup removes from Firestore and cleans up Room`() = runTest {
        coJustRun { firestoreDataSource.deleteGroup(groupId) }

        val result = repository.deleteGroup(groupId)

        result.getOrThrow()
        coVerifyOrder {
            firestoreDataSource.deleteGroup(groupId)
            sharedGroupDao.deleteById(groupId)
            sharedListDao.deleteByGroup(groupId)
        }
    }

    @Test
    fun `deleteGroup still cleans up Room when Firestore throws`() = runTest {
        coEvery { firestoreDataSource.deleteGroup(groupId) } throws RuntimeException("offline")

        val result = repository.deleteGroup(groupId)

        result.getOrThrow()  // exposes exception if failure
        coVerify { sharedGroupDao.deleteById(groupId) }
        coVerify { sharedListDao.deleteByGroup(groupId) }
    }

    // ---- leaveGroup ----

    @Test
    fun `leaveGroup removes uid from Firestore and cleans up Room`() = runTest {
        val memberId = "member-1"
        coJustRun { firestoreDataSource.leaveGroup(groupId, uid, memberId) }

        val result = repository.leaveGroup(groupId, uid, memberId)

        result.getOrThrow()
        coVerifyOrder {
            firestoreDataSource.leaveGroup(groupId, uid, memberId)
            sharedGroupDao.deleteById(groupId)
            sharedListDao.deleteByGroup(groupId)
        }
    }

    @Test
    fun `leaveGroup still cleans up Room when Firestore throws`() = runTest {
        val memberId = "member-1"
        coEvery { firestoreDataSource.leaveGroup(groupId, uid, memberId) } throws RuntimeException("offline")

        val result = repository.leaveGroup(groupId, uid, memberId)

        result.getOrThrow()  // exposes exception if failure
        coVerify { sharedGroupDao.deleteById(groupId) }
        coVerify { sharedListDao.deleteByGroup(groupId) }
    }

    // ---- Phase 5: network-aware tests ----

    @Test
    fun `observeGroups uses Room directly when offline`() = runTest {
        every { networkMonitor.isOnlineFlow } returns flowOf(false)
        every { sharedGroupDao.observeAll() } returns flowOf(listOf(fakeEntity))

        repository.observeGroups(uid).test {
            val groups = awaitItem()
            assertEquals(1, groups.size)
            // Firestore should NOT be called when offline
            coVerify(exactly = 0) { firestoreDataSource.observeGroups(any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeGroups switches to Firestore when going online`() = runTest {
        val networkFlow = kotlinx.coroutines.flow.MutableStateFlow(false)
        every { networkMonitor.isOnlineFlow } returns networkFlow
        every { sharedGroupDao.observeAll() } returns flowOf(listOf(fakeEntity))
        every { firestoreDataSource.observeGroups(uid) } returns flowOf(listOf(fakeGroup))

        repository.observeGroups(uid).test {
            // offline → Room
            val offlineGroups = awaitItem()
            assertEquals(groupId, offlineGroups[0].groupId)

            // go online → Firestore
            networkFlow.value = true
            val onlineGroups = awaitItem()
            assertEquals(groupId, onlineGroups[0].groupId)
            coVerify { firestoreDataSource.observeGroups(uid) }
            cancelAndIgnoreRemainingEvents()
        }
    }
}
