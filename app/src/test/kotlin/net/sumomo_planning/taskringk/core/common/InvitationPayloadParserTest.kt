package net.sumomo_planning.taskringk.core.common

import java.time.Instant
import net.sumomo_planning.taskringk.domain.model.Invitation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InvitationPayloadParserTest {

    @Test
    fun `parse handles flutter v3_1 json payload`() {
        val raw = """
            {
              "invitationId": "INV_123",
              "sharedGroupId": "GROUP_1",
              "securityKey": "SEC_ABC",
              "type": "secure_qr_invitation",
              "version": "3.1"
            }
        """.trimIndent()

        val payload = InvitationPayloadParser.parse(raw)

        assertEquals("INV_123", payload.invitationId)
        assertEquals("GROUP_1", payload.groupId)
        assertEquals("SEC_ABC", payload.securityKey)
        assertEquals("secure_qr_invitation", payload.type)
        assertEquals("3.1", payload.version)
    }

    @Test
    fun `parse handles https deep link payload`() {
        val raw = "https://legacy-prod-firebase-project-id.web.app/invite?invitationId=INV_999&key=SEC_999&groupId=GROUP_9"

        val payload = InvitationPayloadParser.parse(raw)

        assertEquals("INV_999", payload.invitationId)
        assertEquals("GROUP_9", payload.groupId)
        assertEquals("SEC_999", payload.securityKey)
    }

    @Test
    fun `parse handles taskringk custom scheme payload`() {
        val raw = "taskringk://invite?token=INV_777&groupId=GROUP_7&key=SEC_777"

        val payload = InvitationPayloadParser.parse(raw)

        assertEquals("INV_777", payload.invitationId)
        assertEquals("GROUP_7", payload.groupId)
        assertEquals("SEC_777", payload.securityKey)
    }

    @Test
    fun `toInvitationPayloadJson emits flutter compatible keys`() {
        val invitation = Invitation(
            token = "INV_ABC",
            groupId = "GROUP_ABC",
            groupName = "demo",
            invitedBy = "owner",
            inviterName = "Owner",
            createdAt = Instant.now(),
            expiresAt = Instant.now().plusSeconds(3600),
            securityKey = "SEC_ABC",
        )

        val payload = InvitationPayloadParser.parse(invitation.toInvitationPayloadJson())

        assertEquals("INV_ABC", payload.invitationId)
        assertEquals("GROUP_ABC", payload.groupId)
        assertEquals("SEC_ABC", payload.securityKey)
    }

    @Test
    fun `parse throws when required fields are missing`() {
        val raw = "{}"

        val result = runCatching { InvitationPayloadParser.parse(raw) }

        assertTrue(result.isFailure)
    }

    @Test
    fun `parse throws when securityKey is blank`() {
        val raw = """
            {
              "invitationId": "INV_123",
              "sharedGroupId": "GROUP_1",
              "securityKey": ""
            }
        """.trimIndent()

        val result = runCatching { InvitationPayloadParser.parse(raw) }

        assertTrue(result.isFailure)
    }
}