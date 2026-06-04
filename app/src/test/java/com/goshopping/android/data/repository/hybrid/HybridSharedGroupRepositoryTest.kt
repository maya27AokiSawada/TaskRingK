package com.goshopping.android.data.repository.hybrid

import com.goshopping.android.data.model.GroupType
import com.goshopping.android.data.model.InvitationStatus
import com.goshopping.android.data.model.SharedGroup
import com.goshopping.android.data.model.SharedGroupMember
import com.goshopping.android.data.model.SharedGroupRole
import com.goshopping.android.data.model.SyncStatus
import com.goshopping.android.data.repository.firestore.FirestoreSharedGroupRepository
import com.goshopping.android.data.repository.local.LocalSharedGroupRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Date

/**
 * HybridSharedGroupRepository のユニットテスト。
 *
 * 依存: testImplementation("io.mockk:mockk:1.13.x")
 *       testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.x.x")
 */
class HybridSharedGroupRepositoryTest {

    private lateinit var firestoreRepo: FirestoreSharedGroupRepository
    private lateinit var localRepo: LocalSharedGroupRepository
    private lateinit var sut: HybridSharedGroupRepository

    @Before
    fun setUp() {
        firestoreRepo = mockk()
        localRepo = mockk()
        sut = HybridSharedGroupRepository(firestoreRepo, localRepo)
    }

    // ―― createGroup ――

    @Test
    fun `createGroup calls both firestore and local repos`() = runTest {
        val member = makeOwnerMember()
        coEvery { firestoreRepo.createGroup(any(), any(), any()) } returns makeGroup()
        coEvery { localRepo.createGroup(any(), any(), any()) } returns makeGroup()

        sut.createGroup("group_1", "テストグループ", member)

        coVerify(exactly = 1) { firestoreRepo.createGroup("group_1", "テストグループ", member) }
        coVerify(exactly = 1) { localRepo.createGroup("group_1", "テストグループ", member) }
    }

    @Test
    fun `createGroup calls local repo even when firestore throws`() = runTest {
        val member = makeOwnerMember()
        coEvery { firestoreRepo.createGroup(any(), any(), any()) } throws RuntimeException("network error")
        coEvery { localRepo.createGroup(any(), any(), any()) } returns makeGroup()

        val result = sut.createGroup("group_1", "テストグループ", member)

        // ローカルへの書き込みは続行される（§6-2）
        coVerify(exactly = 1) { localRepo.createGroup(any(), any(), any()) }
        assertEquals("group_1", result.groupId)
    }

    // ―― getAllGroups ――

    @Test
    fun `getAllGroups returns firestore result and caches to local`() = runTest {
        val groups = listOf(makeGroup("group_1"), makeGroup("group_2"))
        coEvery { firestoreRepo.getAllGroups() } returns groups
        coEvery { localRepo.saveGroups(any()) } just runs

        val result = sut.getAllGroups()

        assertEquals(groups, result)
        coVerify(exactly = 1) { localRepo.saveGroups(groups) }
    }

    @Test
    fun `getAllGroups falls back to local when firestore throws`() = runTest {
        val localGroups = listOf(makeGroup("group_local"))
        coEvery { firestoreRepo.getAllGroups() } throws RuntimeException("offline")
        coEvery { localRepo.getAllGroups() } returns localGroups

        val result = sut.getAllGroups()

        assertEquals(localGroups, result)
        coVerify(exactly = 1) { localRepo.getAllGroups() }
    }

    // ―― getGroupById ――

    @Test
    fun `getGroupById returns firestore result and updates local cache`() = runTest {
        val group = makeGroup("group_1")
        coEvery { firestoreRepo.getGroupById("group_1") } returns group
        coEvery { localRepo.updateGroup("group_1", group) } returns group

        val result = sut.getGroupById("group_1")

        assertEquals(group, result)
        coVerify(exactly = 1) { localRepo.updateGroup("group_1", group) }
    }

    @Test
    fun `getGroupById falls back to local when firestore throws`() = runTest {
        val localGroup = makeGroup("group_1")
        coEvery { firestoreRepo.getGroupById("group_1") } throws RuntimeException("offline")
        coEvery { localRepo.getGroupById("group_1") } returns localGroup

        val result = sut.getGroupById("group_1")

        assertEquals(localGroup, result)
    }

    // ―― updateGroup ――

    @Test
    fun `updateGroup calls both repos`() = runTest {
        val group = makeGroup("group_1")
        coEvery { firestoreRepo.updateGroup(any(), any()) } returns group
        coEvery { localRepo.updateGroup(any(), any()) } returns group

        sut.updateGroup("group_1", group)

        coVerify(exactly = 1) { firestoreRepo.updateGroup("group_1", group) }
        coVerify(exactly = 1) { localRepo.updateGroup("group_1", group) }
    }

    @Test
    fun `updateGroup calls local repo even when firestore throws`() = runTest {
        val group = makeGroup("group_1")
        coEvery { firestoreRepo.updateGroup(any(), any()) } throws RuntimeException("network error")
        coEvery { localRepo.updateGroup(any(), any()) } returns group

        val result = sut.updateGroup("group_1", group)

        coVerify(exactly = 1) { localRepo.updateGroup("group_1", group) }
        assertEquals(group, result)
    }

    // ―― deleteGroup ――

    @Test
    fun `deleteGroup calls both repos`() = runTest {
        val group = makeGroup("group_1")
        coEvery { firestoreRepo.deleteGroup("group_1") } returns group
        coEvery { localRepo.deleteGroup("group_1") } returns group

        sut.deleteGroup("group_1")

        coVerify(exactly = 1) { firestoreRepo.deleteGroup("group_1") }
        coVerify(exactly = 1) { localRepo.deleteGroup("group_1") }
    }

    // ―― removeMember ――

    @Test
    fun `removeMember removes from local immediately even when firestore throws`() = runTest {
        // 仕様書 §12-9: ローカルから即座に削除する
        val member = makeOwnerMember()
        val group = makeGroup("group_1")
        coEvery { firestoreRepo.removeMember(any(), any()) } throws RuntimeException("network error")
        coEvery { localRepo.removeMember("group_1", member) } returns group

        sut.removeMember("group_1", member)

        coVerify(exactly = 1) { localRepo.removeMember("group_1", member) }
    }

    @Test
    fun `addMember calls both repos`() = runTest {
        val member = makeOwnerMember()
        val group = makeGroup("group_1")
        coEvery { firestoreRepo.addMember(any(), any()) } returns group
        coEvery { localRepo.addMember(any(), any()) } returns group

        sut.addMember("group_1", member)

        coVerify(exactly = 1) { firestoreRepo.addMember("group_1", member) }
        coVerify(exactly = 1) { localRepo.addMember("group_1", member) }
    }

    // ―― helpers ――

    private fun makeGroup(groupId: String = "group_1") = SharedGroup(
        groupId = groupId,
        groupName = "テストグループ",
        ownerUid = "uid_owner",
        allowedUid = listOf("uid_owner"),
        members = emptyList(),
        groupType = GroupType.SHOPPING,
        syncStatus = SyncStatus.SYNCED,
        createdAt = Date()
    )

    private fun makeOwnerMember() = SharedGroupMember(
        memberId = "uid_owner",
        name = "田中太郎",
        contact = "taro@example.com",
        role = SharedGroupRole.OWNER,
        isSignedIn = true,
        invitationStatus = InvitationStatus.SELF
    )
}
