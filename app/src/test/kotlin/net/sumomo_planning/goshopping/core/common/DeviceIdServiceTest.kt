package net.sumomo_planning.goshopping.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceIdServiceTest {

    private val service = DeviceIdService()

    @Test
    fun generateGroupId_usesDevicePrefixAndMillis() {
        assertEquals("a3f8c9d2_1782777600000", service.generateGroupId("a3f8c9d2", 1782777600000))
    }

    @Test
    fun generateListId_usesDevicePrefixAndUuid8() {
        val id = service.generateListId("a3f8c9d2")

        assertTrue(id.matches(Regex("a3f8c9d2_[0-9a-f]{8}")))
    }

    @Test
    fun generateInvitationToken_usesInvPrefix() {
        assertTrue(service.generateInvitationToken().startsWith("INV_"))
    }
}
