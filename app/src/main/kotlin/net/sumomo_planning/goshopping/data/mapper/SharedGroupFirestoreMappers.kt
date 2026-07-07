package net.sumomo_planning.goshopping.data.mapper

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import java.time.Instant
import net.sumomo_planning.goshopping.data.remote.dto.SharedGroupDto
import net.sumomo_planning.goshopping.data.remote.dto.SharedGroupMemberDto
import net.sumomo_planning.goshopping.domain.model.SharedGroup
import net.sumomo_planning.goshopping.domain.model.SharedGroupMember
import net.sumomo_planning.goshopping.domain.model.SyncStatus

// ---- Firestore DTO → Domain ----

fun SharedGroupDto.toDomain(): SharedGroup = SharedGroup(
    groupId = groupId,
    groupName = groupName,
    ownerUid = ownerUid,
    allowedUid = allowedUid,
    members = members.map { it.toDomain() },
    groupType = groupType.toGroupType(),
    syncStatus = syncStatus.toSyncStatus(),
    createdAt = createdAt?.toDate()?.toInstant() ?: Instant.now(),
    updatedAt = updatedAt?.toDate()?.toInstant(),
)

fun SharedGroupMemberDto.toDomain(): SharedGroupMember = SharedGroupMember(
    memberId = memberId,
    name = name,
    contact = contact,
    role = role.toSharedGroupRole(),
    isSignedIn = isSignedIn,
    invitationStatus = invitationStatus.toInvitationStatus(),
    securityKey = securityKey,
    invitedAt = invitedAt?.toDate()?.toInstant(),
    acceptedAt = acceptedAt?.toDate()?.toInstant(),
)

// ---- Domain → Firestore map (used for set() writes) ----

/**
 * Converts a [SharedGroup] to a Firestore-compatible map.
 *
 * Uses [FieldValue.serverTimestamp] for `createdAt` / `updatedAt` so that
 * the server records the authoritative write time.
 */
fun SharedGroup.toFirestoreMap(): Map<String, Any?> = mapOf(
    "groupId" to groupId,
    "groupName" to groupName,
    "ownerUid" to ownerUid,
    "allowedUid" to allowedUid,
    "members" to members.map { it.toMemberMap() },
    "groupType" to groupType.storageValue,
    "syncStatus" to SyncStatus.SYNCED.storageValue,
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp(),
)

fun SharedGroupMember.toMemberMap(): Map<String, Any?> = mapOf(
    "memberId" to memberId,
    "name" to name,
    "contact" to contact,
    "role" to role.storageValue,
    "isSignedIn" to isSignedIn,
    "invitationStatus" to invitationStatus.storageValue,
    "securityKey" to securityKey,
    "invitedAt" to invitedAt?.let { Timestamp(it.epochSecond, it.nano) },
    "acceptedAt" to acceptedAt?.let { Timestamp(it.epochSecond, it.nano) },
)

// ---- DTO member → plain map (used for update() writes after leaveGroup) ----

internal fun SharedGroupMemberDto.toUpdateMap(): Map<String, Any?> = mapOf(
    "memberId" to memberId,
    "name" to name,
    "contact" to contact,
    "role" to role,
    "isSignedIn" to isSignedIn,
    "invitationStatus" to invitationStatus,
    "securityKey" to securityKey,
    "invitedAt" to invitedAt,
    "acceptedAt" to acceptedAt,
)
