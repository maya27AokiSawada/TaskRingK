package com.goshopping.android.data.model

import com.google.firebase.Timestamp
import java.util.Date

data class Invitation(
    val token: String = "",
    val groupId: String = "",
    val groupName: String = "",
    val invitedBy: String = "",
    val inviterName: String = "",
    val createdAt: Date = Date(),
    val expiresAt: Date = Date(),
    val maxUses: Int = 5,
    val currentUses: Int = 0,
    val usedBy: List<String> = emptyList(),
    val securityKey: String? = null
) {
    fun isValid(currentUid: String): Boolean {
        val now = Date()
        return expiresAt.after(now)
                && currentUses < maxUses
                && !usedBy.contains(currentUid)
    }

    fun toMap(): Map<String, Any?> = mapOf(
        "token" to token,
        "groupId" to groupId,
        "groupName" to groupName,
        "invitedBy" to invitedBy,
        "inviterName" to inviterName,
        "createdAt" to createdAt,
        "expiresAt" to expiresAt,
        "maxUses" to maxUses,
        "currentUses" to currentUses,
        "usedBy" to usedBy,
        "securityKey" to securityKey
    )

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>, token: String = ""): Invitation = Invitation(
            token = token.ifEmpty { map["token"] as? String ?: "" },
            groupId = map["groupId"] as? String ?: "",
            groupName = map["groupName"] as? String ?: "",
            invitedBy = map["invitedBy"] as? String ?: "",
            inviterName = map["inviterName"] as? String ?: "",
            createdAt = (map["createdAt"] as? Timestamp)?.toDate() ?: Date(),
            expiresAt = (map["expiresAt"] as? Timestamp)?.toDate() ?: Date(),
            maxUses = (map["maxUses"] as? Long)?.toInt() ?: 5,
            currentUses = (map["currentUses"] as? Long)?.toInt() ?: 0,
            usedBy = (map["usedBy"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            securityKey = map["securityKey"] as? String
        )
    }
}
