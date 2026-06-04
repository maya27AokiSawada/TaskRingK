package com.goshopping.android.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class InvitationModelTest {

    @Test
    fun `isValid returns true for a fresh invitation that has not been used`() {
        val invitation = makeInvitation(
            currentUses = 0,
            maxUses = 5,
            expiresAt = futureDate(),
            usedBy = emptyList()
        )
        assertTrue(invitation.isValid("uid_new_user"))
    }

    @Test
    fun `isValid returns false when invitation is expired`() {
        val invitation = makeInvitation(
            expiresAt = pastDate()
        )
        assertFalse(invitation.isValid("uid_new_user"))
    }

    @Test
    fun `isValid returns false when maxUses is reached`() {
        val invitation = makeInvitation(
            currentUses = 5,
            maxUses = 5,
            expiresAt = futureDate()
        )
        assertFalse(invitation.isValid("uid_new_user"))
    }

    @Test
    fun `isValid returns false when currentUid already used the invitation`() {
        val invitation = makeInvitation(
            currentUses = 1,
            maxUses = 5,
            expiresAt = futureDate(),
            usedBy = listOf("uid_existing_user")
        )
        assertFalse(invitation.isValid("uid_existing_user"))
    }

    @Test
    fun `isValid returns true when different user uses an already-used invitation`() {
        val invitation = makeInvitation(
            currentUses = 1,
            maxUses = 5,
            expiresAt = futureDate(),
            usedBy = listOf("uid_other_user")
        )
        assertTrue(invitation.isValid("uid_new_user"))
    }

    @Test
    fun `isValid returns false when both expired and maxUses reached`() {
        val invitation = makeInvitation(
            currentUses = 5,
            maxUses = 5,
            expiresAt = pastDate()
        )
        assertFalse(invitation.isValid("uid_new_user"))
    }

    // ―― helpers ――

    private fun makeInvitation(
        currentUses: Int = 0,
        maxUses: Int = 5,
        expiresAt: Date = futureDate(),
        usedBy: List<String> = emptyList()
    ) = Invitation(
        token = "INV_test-token",
        groupId = "group_1",
        groupName = "テストグループ",
        invitedBy = "uid_owner",
        inviterName = "田中太郎",
        createdAt = Date(),
        expiresAt = expiresAt,
        maxUses = maxUses,
        currentUses = currentUses,
        usedBy = usedBy
    )

    private fun futureDate() = Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000L)
    private fun pastDate() = Date(System.currentTimeMillis() - 1000L)
}
