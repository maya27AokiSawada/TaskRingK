package net.sumomo_planning.taskringk.core.common

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class InvitationCode(
    val token: String,
    val groupId: String,
    val securityKey: String? = null,
)

object InvitationCodeCodec {
    private const val PREFIX = "taskringk://invite"

    fun encode(code: InvitationCode): String {
        val token = URLEncoder.encode(code.token, StandardCharsets.UTF_8)
        val groupId = URLEncoder.encode(code.groupId, StandardCharsets.UTF_8)
        val securityKey = code.securityKey?.let {
            "&key=${URLEncoder.encode(it, StandardCharsets.UTF_8)}"
        } ?: ""
        return "$PREFIX?token=$token&groupId=$groupId$securityKey"
    }

    fun decode(raw: String): InvitationCode? {
        val uri = runCatching { java.net.URI(raw.trim()) }.getOrNull() ?: return null
        if (uri.scheme != "taskringk" || uri.host != "invite") return null

        val params = uri.rawQuery
            ?.split('&')
            ?.mapNotNull { entry ->
                val parts = entry.split('=', limit = 2)
                if (parts.size != 2) return@mapNotNull null
                parts[0] to URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
            }
            ?.toMap()
            .orEmpty()

        val token = params["token"] ?: return null
        val groupId = params["groupId"] ?: return null
        return InvitationCode(
            token = token,
            groupId = groupId,
            securityKey = params["key"],
        )
    }
}