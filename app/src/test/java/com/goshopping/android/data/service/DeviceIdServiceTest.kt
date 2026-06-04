package com.goshopping.android.data.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeviceIdServiceTest {

    @Before
    fun setUp() {
        DeviceIdService.setDevicePrefix("a3f8c9d2")
    }

    // ―― generateGroupId ――

    @Test
    fun `generateGroupId has format prefix_timestamp`() {
        val id = DeviceIdService.generateGroupId("a3f8c9d2")
        val parts = id.split("_")
        assertEquals(2, parts.size)
        assertEquals("a3f8c9d2", parts[0])
        // タイムスタンプ部分は数値
        assertTrue("timestamp part should be numeric: ${parts[1]}",
            parts[1].toLongOrNull() != null)
    }

    @Test
    fun `generateGroupId timestamp is close to current time`() {
        val before = System.currentTimeMillis()
        val id = DeviceIdService.generateGroupId("a3f8c9d2")
        val after = System.currentTimeMillis()
        val timestamp = id.substringAfter("_").toLong()
        assertTrue(timestamp in before..after)
    }

    @Test
    fun `generateGroupId uses stored devicePrefix when not specified`() {
        val id = DeviceIdService.generateGroupId()
        assertTrue(id.startsWith("a3f8c9d2_"))
    }

    // ―― generateListId ――

    @Test
    fun `generateListId has format prefix_uuid8`() {
        val id = DeviceIdService.generateListId("a3f8c9d2")
        val parts = id.split("_")
        assertEquals(2, parts.size)
        assertEquals("a3f8c9d2", parts[0])
        assertEquals(8, parts[1].length)
    }

    @Test
    fun `generateListId uuid8 part is alphanumeric`() {
        val id = DeviceIdService.generateListId("a3f8c9d2")
        val uuid8 = id.substringAfter("_")
        assertTrue("uuid8 should be alphanumeric: $uuid8",
            uuid8.all { it.isLetterOrDigit() })
    }

    @Test
    fun `generateListId produces unique ids`() {
        val ids = (1..100).map { DeviceIdService.generateListId("a3f8c9d2") }.toSet()
        assertEquals(100, ids.size)
    }

    // ―― generateInvitationToken ――

    @Test
    fun `generateInvitationToken starts with INV_ prefix`() {
        val token = DeviceIdService.generateInvitationToken()
        assertTrue("token should start with INV_: $token", token.startsWith("INV_"))
    }

    @Test
    fun `generateInvitationToken uuid part matches UUID v4 format`() {
        val token = DeviceIdService.generateInvitationToken()
        val uuid = token.removePrefix("INV_")
        // UUID v4: xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx (36文字)
        assertEquals(36, uuid.length)
        val uuidRegex = Regex(
            "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"
        )
        assertTrue("uuid should match v4 format: $uuid", uuidRegex.matches(uuid))
    }

    @Test
    fun `generateInvitationToken produces unique tokens`() {
        val tokens = (1..50).map { DeviceIdService.generateInvitationToken() }.toSet()
        assertEquals(50, tokens.size)
    }

    // ―― setDevicePrefix / getDevicePrefix ――

    @Test
    fun `setDevicePrefix updates the stored prefix`() {
        DeviceIdService.setDevicePrefix("deadbeef")
        assertEquals("deadbeef", DeviceIdService.getDevicePrefix())
        // teardown: reset to test prefix
        DeviceIdService.setDevicePrefix("a3f8c9d2")
    }
}
