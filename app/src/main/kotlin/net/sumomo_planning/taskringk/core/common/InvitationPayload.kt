package net.sumomo_planning.taskringk.core.common

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import net.sumomo_planning.taskringk.domain.model.Invitation

data class InvitationPayload(
    val invitationId: String,
    val groupId: String,
    val securityKey: String? = null,
    val type: String = "secure_qr_invitation",
    val version: String = "3.1",
)

object InvitationPayloadParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parse(raw: String): InvitationPayload {
        val normalized = raw.trim()
        if (normalized.isEmpty()) {
            throw IllegalArgumentException("QR data is empty")
        }

        if (normalized.startsWith("{")) {
            return parseJsonPayload(normalized)
        }

        return parseUriPayload(normalized)
    }

    private fun parseJsonPayload(rawJson: String): InvitationPayload {
        val root = json.parseToJsonElement(rawJson) as? JsonObject
            ?: throw IllegalArgumentException("Invalid QR JSON")

        val invitationId = firstString(root, "invitationId", "token")
            ?: throw IllegalArgumentException("invitationId is missing")
        val groupId = firstString(root, "sharedGroupId", "SharedGroupId", "groupId")
            ?: throw IllegalArgumentException("groupId is missing")
        val securityKey = firstString(root, "securityKey", "key")
            ?: throw IllegalArgumentException("securityKey is missing")

        return InvitationPayload(
            invitationId = invitationId,
            groupId = groupId,
            securityKey = securityKey,
            type = firstString(root, "type") ?: "secure_qr_invitation",
            version = firstString(root, "version") ?: "3.1",
        )
    }

    private fun parseUriPayload(raw: String): InvitationPayload {
        val uri = runCatching { URI(raw) }.getOrNull()
            ?: throw IllegalArgumentException("Invalid QR URI")

        val params = parseQuery(uri.rawQuery)
        val invitationId = params["invitationId"] ?: params["token"]
            ?: throw IllegalArgumentException("invitationId is missing")
        val groupId = params["sharedGroupId"]
            ?: params["SharedGroupId"]
            ?: params["groupId"]
            ?: throw IllegalArgumentException("groupId is missing")
        val securityKey = params["securityKey"] ?: params["key"]
        if (securityKey.isNullOrBlank()) {
            throw IllegalArgumentException("securityKey is missing")
        }

        return InvitationPayload(
            invitationId = invitationId,
            groupId = groupId,
            securityKey = securityKey,
            type = params["type"] ?: "secure_qr_invitation",
            version = params["version"] ?: "3.1",
        )
    }

    private fun parseQuery(rawQuery: String?): Map<String, String> {
        if (rawQuery.isNullOrBlank()) return emptyMap()
        return rawQuery
            .split('&')
            .mapNotNull { entry ->
                val parts = entry.split('=', limit = 2)
                if (parts.isEmpty() || parts[0].isBlank()) {
                    return@mapNotNull null
                }
                val value = if (parts.size > 1) {
                    URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                } else {
                    ""
                }
                parts[0] to value
            }
            .toMap()
    }

    private fun firstString(jsonObject: JsonObject, vararg keys: String): String? {
        return keys.asSequence()
            .mapNotNull { key -> (jsonObject[key] as? JsonPrimitive)?.contentOrNull }
            .firstOrNull { it.isNotBlank() }
    }
}

fun Invitation.toInvitationPayloadJson(): String {
    val normalizedSecurityKey = securityKey?.takeIf { it.isNotBlank() }
        ?: throw IllegalStateException("Invitation has no securityKey")

    val payload = buildJsonObject {
        put("invitationId", invitationId)
        put("token", invitationId)
        put("sharedGroupId", groupId)
        put("SharedGroupId", groupId)
        put("groupId", groupId)
        put("securityKey", normalizedSecurityKey)
        put("type", "secure_qr_invitation")
        put("version", "3.1")
    }
    return Json.encodeToString(JsonObject.serializer(), payload)
}

private val Invitation.invitationId: String
    get() = token