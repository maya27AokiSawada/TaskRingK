package com.goshopping.android.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class SharedGroupModelTest {

    // ―― canAccess ――

    @Test
    fun `canAccess returns true when uid is ownerUid`() {
        val group = makeGroup(ownerUid = "uid_owner", allowedUid = listOf("uid_owner"))
        assertTrue(group.canAccess("uid_owner"))
    }

    @Test
    fun `canAccess returns true when uid is in allowedUid`() {
        val group = makeGroup(ownerUid = "uid_owner", allowedUid = listOf("uid_owner", "uid_member"))
        assertTrue(group.canAccess("uid_member"))
    }

    @Test
    fun `canAccess returns false for unknown uid`() {
        val group = makeGroup(ownerUid = "uid_owner", allowedUid = listOf("uid_owner"))
        assertFalse(group.canAccess("uid_stranger"))
    }

    // ―― GroupType enum ――

    @Test
    fun `GroupType toFirestoreValue returns lowercase`() {
        assertEquals("shopping", GroupType.SHOPPING.toFirestoreValue())
        assertEquals("todo", GroupType.TODO.toFirestoreValue())
    }

    @Test
    fun `GroupType fromFirestoreValue round-trip`() {
        assertEquals(GroupType.SHOPPING, GroupType.fromFirestoreValue("shopping"))
        assertEquals(GroupType.TODO, GroupType.fromFirestoreValue("todo"))
        assertEquals(GroupType.SHOPPING, GroupType.fromFirestoreValue("unknown"))
    }

    // ―― SyncStatus enum ――

    @Test
    fun `SyncStatus fromFirestoreValue round-trip`() {
        assertEquals(SyncStatus.SYNCED, SyncStatus.fromFirestoreValue("synced"))
        assertEquals(SyncStatus.PENDING, SyncStatus.fromFirestoreValue("pending"))
        assertEquals(SyncStatus.LOCAL, SyncStatus.fromFirestoreValue("local"))
        assertEquals(SyncStatus.SYNCED, SyncStatus.fromFirestoreValue("unknown"))
    }

    // ―― SharedGroupRole enum ――

    @Test
    fun `SharedGroupRole fromFirestoreValue round-trip`() {
        assertEquals(SharedGroupRole.OWNER, SharedGroupRole.fromFirestoreValue("owner"))
        assertEquals(SharedGroupRole.MANAGER, SharedGroupRole.fromFirestoreValue("manager"))
        assertEquals(SharedGroupRole.PARTNER, SharedGroupRole.fromFirestoreValue("partner"))
        assertEquals(SharedGroupRole.MEMBER, SharedGroupRole.fromFirestoreValue("member"))
        assertEquals(SharedGroupRole.MEMBER, SharedGroupRole.fromFirestoreValue("unknown"))
    }

    // ―― InvitationStatus enum ――

    @Test
    fun `InvitationStatus fromFirestoreValue round-trip`() {
        assertEquals(InvitationStatus.SELF, InvitationStatus.fromFirestoreValue("self"))
        assertEquals(InvitationStatus.PENDING, InvitationStatus.fromFirestoreValue("pending"))
        assertEquals(InvitationStatus.ACCEPTED, InvitationStatus.fromFirestoreValue("accepted"))
        assertEquals(InvitationStatus.DELETED, InvitationStatus.fromFirestoreValue("deleted"))
    }

    // ―― toMap / fromMap ――

    @Test
    fun `SharedGroupMember toMap contains all required fields`() {
        val member = makeOwnerMember()
        val map = member.toMap()
        assertEquals("member_1", map["memberId"])
        assertEquals("田中太郎", map["name"])
        assertEquals("owner", map["role"])
        assertEquals("self", map["invitationStatus"])
    }

    @Test
    fun `SharedGroupMember fromMap defaults when keys are missing`() {
        val member = SharedGroupMember.fromMap(emptyMap())
        assertEquals("", member.memberId)
        assertEquals(SharedGroupRole.MEMBER, member.role)
        assertEquals(InvitationStatus.SELF, member.invitationStatus)
        assertFalse(member.isSignedIn)
    }

    // ―― helpers ――

    private fun makeGroup(
        ownerUid: String = "uid_owner",
        allowedUid: List<String> = listOf("uid_owner")
    ) = SharedGroup(
        groupId = "group_1",
        groupName = "テストグループ",
        ownerUid = ownerUid,
        allowedUid = allowedUid,
        members = emptyList(),
        createdAt = Date()
    )

    private fun makeOwnerMember() = SharedGroupMember(
        memberId = "member_1",
        name = "田中太郎",
        contact = "taro@example.com",
        role = SharedGroupRole.OWNER,
        isSignedIn = true,
        invitationStatus = InvitationStatus.SELF
    )
}
