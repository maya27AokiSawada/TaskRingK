package com.goshopping.android.data.model

import com.google.firebase.Timestamp
import java.util.Date

enum class GroupType {
    SHOPPING, TODO;

    fun toFirestoreValue(): String = name.lowercase()

    companion object {
        fun fromFirestoreValue(value: String): GroupType =
            when (value.lowercase()) {
                "todo" -> TODO
                else -> SHOPPING
            }
    }
}

enum class SyncStatus {
    SYNCED, PENDING, LOCAL;

    fun toFirestoreValue(): String = name.lowercase()

    companion object {
        fun fromFirestoreValue(value: String): SyncStatus =
            when (value.lowercase()) {
                "pending" -> PENDING
                "local" -> LOCAL
                else -> SYNCED
            }
    }
}

enum class SharedGroupRole {
    OWNER, MEMBER, MANAGER, PARTNER;

    fun toFirestoreValue(): String = name.lowercase()

    companion object {
        fun fromFirestoreValue(value: String): SharedGroupRole =
            when (value.lowercase()) {
                "owner" -> OWNER
                "manager" -> MANAGER
                "partner" -> PARTNER
                else -> MEMBER
            }
    }
}

enum class InvitationStatus {
    SELF, PENDING, ACCEPTED, DELETED;

    fun toFirestoreValue(): String = name.lowercase()

    companion object {
        fun fromFirestoreValue(value: String): InvitationStatus =
            when (value.lowercase()) {
                "pending" -> PENDING
                "accepted" -> ACCEPTED
                "deleted" -> DELETED
                else -> SELF
            }
    }
}

data class SharedGroupMember(
    val memberId: String = "",
    val name: String = "",
    val contact: String = "",
    val role: SharedGroupRole = SharedGroupRole.MEMBER,
    val isSignedIn: Boolean = false,
    val invitationStatus: InvitationStatus = InvitationStatus.SELF,
    val securityKey: String? = null,
    val invitedAt: Date? = null,
    val acceptedAt: Date? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "memberId" to memberId,
        "name" to name,
        "contact" to contact,
        "role" to role.toFirestoreValue(),
        "isSignedIn" to isSignedIn,
        "invitationStatus" to invitationStatus.toFirestoreValue(),
        "securityKey" to securityKey,
        "invitedAt" to invitedAt,
        "acceptedAt" to acceptedAt
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): SharedGroupMember = SharedGroupMember(
            memberId = map["memberId"] as? String ?: "",
            name = map["name"] as? String ?: "",
            contact = map["contact"] as? String ?: "",
            role = SharedGroupRole.fromFirestoreValue(map["role"] as? String ?: "member"),
            isSignedIn = map["isSignedIn"] as? Boolean ?: false,
            invitationStatus = InvitationStatus.fromFirestoreValue(
                map["invitationStatus"] as? String ?: "self"
            ),
            securityKey = map["securityKey"] as? String,
            invitedAt = (map["invitedAt"] as? Timestamp)?.toDate(),
            acceptedAt = (map["acceptedAt"] as? Timestamp)?.toDate()
        )
    }
}

data class SharedGroup(
    val groupId: String = "",
    val groupName: String = "",
    val ownerUid: String = "",
    val allowedUid: List<String> = emptyList(),
    val members: List<SharedGroupMember> = emptyList(),
    val groupType: GroupType = GroupType.SHOPPING,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val createdAt: Date = Date(),
    val updatedAt: Date? = null
) {
    /** 指定ユーザーがこのグループにアクセス可能か判定する */
    fun canAccess(currentUid: String): Boolean =
        ownerUid == currentUid || allowedUid.contains(currentUid)

    fun toMap(): Map<String, Any?> = mapOf(
        "groupId" to groupId,
        "groupName" to groupName,
        "ownerUid" to ownerUid,
        "allowedUid" to allowedUid,
        "members" to members.map { it.toMap() },
        "groupType" to groupType.toFirestoreValue(),
        "syncStatus" to syncStatus.toFirestoreValue(),
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): SharedGroup = SharedGroup(
            groupId = map["groupId"] as? String ?: "",
            groupName = map["groupName"] as? String ?: "",
            ownerUid = map["ownerUid"] as? String ?: "",
            allowedUid = (map["allowedUid"] as? List<*>)
                ?.filterIsInstance<String>() ?: emptyList(),
            members = (map["members"] as? List<*>)?.mapNotNull { entry ->
                (entry as? Map<String, Any?>)?.let { SharedGroupMember.fromMap(it) }
            } ?: emptyList(),
            groupType = GroupType.fromFirestoreValue(map["groupType"] as? String ?: "shopping"),
            syncStatus = SyncStatus.fromFirestoreValue(map["syncStatus"] as? String ?: "synced"),
            createdAt = (map["createdAt"] as? Timestamp)?.toDate() ?: Date(),
            updatedAt = (map["updatedAt"] as? Timestamp)?.toDate()
        )
    }
}
