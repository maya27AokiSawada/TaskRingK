package net.sumomo_planning.goshopping.data.mapper

import net.sumomo_planning.goshopping.domain.model.GroupType
import net.sumomo_planning.goshopping.domain.model.InvitationStatus
import net.sumomo_planning.goshopping.domain.model.ListType
import net.sumomo_planning.goshopping.domain.model.NotificationType
import net.sumomo_planning.goshopping.domain.model.SharedGroupRole
import net.sumomo_planning.goshopping.domain.model.SyncStatus

val GroupType.storageValue: String
    get() = when (this) {
        GroupType.SHOPPING -> "shopping"
        GroupType.TODO -> "todo"
    }

fun String.toGroupType(): GroupType = when (lowercase()) {
    "todo" -> GroupType.TODO
    else -> GroupType.SHOPPING
}

val ListType.storageValue: String
    get() = when (this) {
        ListType.SHOPPING -> "shopping"
        ListType.TODO -> "todo"
    }

fun String.toListType(): ListType = when (lowercase()) {
    "todo" -> ListType.TODO
    else -> ListType.SHOPPING
}

val SyncStatus.storageValue: String
    get() = when (this) {
        SyncStatus.SYNCED -> "synced"
        SyncStatus.PENDING -> "pending"
        SyncStatus.LOCAL -> "local"
    }

fun String.toSyncStatus(): SyncStatus = when (lowercase()) {
    "pending" -> SyncStatus.PENDING
    "local" -> SyncStatus.LOCAL
    else -> SyncStatus.SYNCED
}

val SharedGroupRole.storageValue: String
    get() = when (this) {
        SharedGroupRole.OWNER -> "owner"
        SharedGroupRole.MEMBER -> "member"
        SharedGroupRole.MANAGER -> "manager"
        SharedGroupRole.PARTNER -> "partner"
    }

fun String.toSharedGroupRole(): SharedGroupRole = when (lowercase()) {
    "owner" -> SharedGroupRole.OWNER
    "manager" -> SharedGroupRole.MANAGER
    "partner" -> SharedGroupRole.PARTNER
    else -> SharedGroupRole.MEMBER
}

val InvitationStatus.storageValue: String
    get() = when (this) {
        InvitationStatus.SELF -> "self"
        InvitationStatus.PENDING -> "pending"
        InvitationStatus.ACCEPTED -> "accepted"
        InvitationStatus.DELETED -> "deleted"
    }

fun String.toInvitationStatus(): InvitationStatus = when (lowercase()) {
    "self" -> InvitationStatus.SELF
    "accepted" -> InvitationStatus.ACCEPTED
    "deleted" -> InvitationStatus.DELETED
    else -> InvitationStatus.PENDING
}

val NotificationType.storageValue: String
    get() = when (this) {
        NotificationType.LIST_CREATED -> "listCreated"
        NotificationType.LIST_DELETED -> "listDeleted"
        NotificationType.LIST_RENAMED -> "listRenamed"
        NotificationType.MEMBER_JOINED -> "memberJoined"
        NotificationType.MEMBER_LEFT -> "memberLeft"
        NotificationType.GROUP_DELETED -> "groupDeleted"
    }

fun String.toNotificationType(): NotificationType = when (this) {
    "listDeleted" -> NotificationType.LIST_DELETED
    "listRenamed" -> NotificationType.LIST_RENAMED
    "memberJoined" -> NotificationType.MEMBER_JOINED
    "memberLeft" -> NotificationType.MEMBER_LEFT
    "groupDeleted" -> NotificationType.GROUP_DELETED
    else -> NotificationType.LIST_CREATED
}
