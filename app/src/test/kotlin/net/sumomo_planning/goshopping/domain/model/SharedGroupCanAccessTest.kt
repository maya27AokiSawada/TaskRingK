package net.sumomo_planning.goshopping.domain.model

import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedGroupCanAccessTest {

    @Test
    fun canAccess_allowsOwnerAndAllowedUidOnly() {
        val group = SharedGroup(
            groupId = "group-1",
            groupName = "Family",
            ownerUid = "owner",
            allowedUid = listOf("member"),
            members = emptyList(),
            createdAt = Instant.parse("2026-06-30T00:00:00Z"),
        )

        assertTrue(group.canAccess("owner"))
        assertTrue(group.canAccess("member"))
        assertFalse(group.canAccess("stranger"))
    }
}
