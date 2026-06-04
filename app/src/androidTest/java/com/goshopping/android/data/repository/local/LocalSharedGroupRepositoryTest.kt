package com.goshopping.android.data.repository.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.goshopping.android.data.model.InvitationStatus
import com.goshopping.android.data.model.SharedGroup
import com.goshopping.android.data.model.SharedGroupMember
import com.goshopping.android.data.model.SharedGroupRole
import com.goshopping.android.data.model.SyncStatus
import com.goshopping.android.data.repository.room.AppDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/**
 * LocalSharedGroupRepository のインスツルメントテスト（Room インメモリDB使用）。
 *
 * 依存: androidTestImplementation("androidx.test.ext:junit:1.x.x")
 *       androidTestImplementation("androidx.test:runner:1.x.x")
 *       androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.x.x")
 */
@RunWith(AndroidJUnit4::class)
class LocalSharedGroupRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var sut: LocalSharedGroupRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        sut = LocalSharedGroupRepository(db.sharedGroupDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ―― createGroup ――

    @Test
    fun `createGroup persists and retrieves the group`() = runTest {
        val member = makeOwnerMember("uid_owner")
        val created = sut.createGroup("group_1", "テストグループ", member)

        assertEquals("group_1", created.groupId)
        assertEquals("テストグループ", created.groupName)
        assertEquals("uid_owner", created.ownerUid)
        assertEquals(SyncStatus.LOCAL, created.syncStatus)
    }

    @Test
    fun `createGroup sets ownerUid and allowedUid from member`() = runTest {
        val member = makeOwnerMember("uid_owner")
        val created = sut.createGroup("group_1", "グループA", member)

        assertEquals(listOf("uid_owner"), created.allowedUid)
        assertEquals(1, created.members.size)
    }

    // ―― getAllGroups ――

    @Test
    fun `getAllGroups returns all created groups`() = runTest {
        sut.createGroup("group_1", "グループ1", makeOwnerMember("uid_1"))
        sut.createGroup("group_2", "グループ2", makeOwnerMember("uid_2"))

        val all = sut.getAllGroups()
        assertEquals(2, all.size)
        assertTrue(all.any { it.groupId == "group_1" })
        assertTrue(all.any { it.groupId == "group_2" })
    }

    @Test
    fun `getAllGroups returns empty list when no groups exist`() = runTest {
        assertTrue(sut.getAllGroups().isEmpty())
    }

    // ―― getGroupById ――

    @Test
    fun `getGroupById returns the correct group`() = runTest {
        sut.createGroup("group_1", "テストグループ", makeOwnerMember("uid_owner"))

        val found = sut.getGroupById("group_1")
        assertEquals("group_1", found.groupId)
    }

    @Test(expected = NoSuchElementException::class)
    fun `getGroupById throws when group does not exist`() = runTest {
        sut.getGroupById("nonexistent_group")
    }

    // ―― updateGroup ――

    @Test
    fun `updateGroup updates the group name`() = runTest {
        sut.createGroup("group_1", "旧名前", makeOwnerMember("uid_owner"))
        val original = sut.getGroupById("group_1")
        val updated = original.copy(groupName = "新名前")

        sut.updateGroup("group_1", updated)

        val fetched = sut.getGroupById("group_1")
        assertEquals("新名前", fetched.groupName)
    }

    // ―― deleteGroup ――

    @Test
    fun `deleteGroup removes the group from local DB`() = runTest {
        sut.createGroup("group_1", "テストグループ", makeOwnerMember("uid_owner"))
        sut.deleteGroup("group_1")

        assertTrue(sut.getAllGroups().isEmpty())
    }

    @Test
    fun `deleteGroup returns the deleted group`() = runTest {
        sut.createGroup("group_1", "テストグループ", makeOwnerMember("uid_owner"))

        val deleted = sut.deleteGroup("group_1")

        assertEquals("group_1", deleted.groupId)
    }

    // ―― addMember ――

    @Test
    fun `addMember appends a new member to the group`() = runTest {
        sut.createGroup("group_1", "テストグループ", makeOwnerMember("uid_owner"))
        val newMember = SharedGroupMember(
            memberId = "uid_member",
            name = "山田花子",
            contact = "hanako@example.com",
            role = SharedGroupRole.MEMBER,
            isSignedIn = true,
            invitationStatus = InvitationStatus.ACCEPTED
        )

        val updated = sut.addMember("group_1", newMember)

        assertEquals(2, updated.members.size)
        assertTrue(updated.members.any { it.memberId == "uid_member" })
    }

    // ―― removeMember ――

    @Test
    fun `removeMember removes only the specified member`() = runTest {
        val owner = makeOwnerMember("uid_owner")
        val member = SharedGroupMember(
            memberId = "uid_member",
            name = "山田花子",
            contact = "hanako@example.com",
            role = SharedGroupRole.MEMBER,
            isSignedIn = true,
            invitationStatus = InvitationStatus.ACCEPTED
        )
        sut.createGroup("group_1", "テストグループ", owner)
        sut.addMember("group_1", member)

        val updated = sut.removeMember("group_1", member)

        assertEquals(1, updated.members.size)
        assertFalse(updated.members.any { it.memberId == "uid_member" })
        assertTrue(updated.members.any { it.memberId == "uid_owner" })
    }

    // ―― saveGroups ――

    @Test
    fun `saveGroups upserts all provided groups`() = runTest {
        val groups = listOf(
            makeGroup("group_a", "グループA", "uid_a"),
            makeGroup("group_b", "グループB", "uid_b")
        )
        sut.saveGroups(groups)

        val all = sut.getAllGroups()
        assertEquals(2, all.size)
    }

    @Test
    fun `saveGroups overwrites existing group with same id`() = runTest {
        sut.createGroup("group_1", "旧名前", makeOwnerMember("uid_owner"))
        val updated = listOf(makeGroup("group_1", "新名前", "uid_owner"))

        sut.saveGroups(updated)

        val fetched = sut.getGroupById("group_1")
        assertEquals("新名前", fetched.groupName)
    }

    // ―― helpers ――

    private fun makeOwnerMember(uid: String) = SharedGroupMember(
        memberId = uid,
        name = "田中太郎",
        contact = "$uid@example.com",
        role = SharedGroupRole.OWNER,
        isSignedIn = true,
        invitationStatus = InvitationStatus.SELF
    )

    private fun makeGroup(groupId: String, groupName: String, ownerUid: String) = SharedGroup(
        groupId = groupId,
        groupName = groupName,
        ownerUid = ownerUid,
        allowedUid = listOf(ownerUid),
        members = emptyList(),
        syncStatus = SyncStatus.SYNCED,
        createdAt = Date()
    )
}
