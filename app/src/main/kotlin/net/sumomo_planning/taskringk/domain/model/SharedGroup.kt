package net.sumomo_planning.taskringk.domain.model

import java.time.Instant

data class SharedGroup(
    val groupId: String,
    val groupName: String,
    val ownerUid: String,
    val allowedUid: List<String>,
    val members: List<SharedGroupMember>,
    val groupType: GroupType = GroupType.SHOPPING,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val createdAt: Instant,
    val updatedAt: Instant? = null,
) {
    fun canAccess(uid: String): Boolean = ownerUid == uid || allowedUid.contains(uid)
}

enum class GroupType { SHOPPING, TODO }

enum class SyncStatus { SYNCED, PENDING, LOCAL }
