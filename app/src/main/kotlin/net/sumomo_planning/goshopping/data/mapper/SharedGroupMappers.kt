package net.sumomo_planning.goshopping.data.mapper

import java.time.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.sumomo_planning.goshopping.data.local.dto.LocalSharedGroupMemberDto
import net.sumomo_planning.goshopping.data.local.room.entity.SharedGroupEntity
import net.sumomo_planning.goshopping.domain.model.SharedGroup
import net.sumomo_planning.goshopping.domain.model.SharedGroupMember

fun SharedGroupEntity.toDomain(json: Json): SharedGroup = SharedGroup(
    groupId = groupId,
    groupName = groupName,
    ownerUid = ownerUid,
    allowedUid = allowedUid,
    members = json.decodeFromString<List<LocalSharedGroupMemberDto>>(membersJson).map { it.toDomain() },
    groupType = groupType.toGroupType(),
    syncStatus = syncStatus.toSyncStatus(),
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = updatedAt?.let(Instant::ofEpochMilli),
)

fun SharedGroup.toEntity(json: Json): SharedGroupEntity = SharedGroupEntity(
    groupId = groupId,
    groupName = groupName,
    ownerUid = ownerUid,
    allowedUid = allowedUid,
    membersJson = json.encodeToString(members.map { it.toLocalDto() }),
    groupType = groupType.storageValue,
    syncStatus = syncStatus.storageValue,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt?.toEpochMilli(),
)

private fun LocalSharedGroupMemberDto.toDomain(): SharedGroupMember = SharedGroupMember(
    memberId = memberId,
    name = name,
    contact = contact,
    role = role.toSharedGroupRole(),
    isSignedIn = isSignedIn,
    invitationStatus = invitationStatus.toInvitationStatus(),
    securityKey = securityKey,
    invitedAt = invitedAt?.let(Instant::ofEpochMilli),
    acceptedAt = acceptedAt?.let(Instant::ofEpochMilli),
)

private fun SharedGroupMember.toLocalDto(): LocalSharedGroupMemberDto = LocalSharedGroupMemberDto(
    memberId = memberId,
    name = name,
    contact = contact,
    role = role.storageValue,
    isSignedIn = isSignedIn,
    invitationStatus = invitationStatus.storageValue,
    securityKey = securityKey,
    invitedAt = invitedAt?.toEpochMilli(),
    acceptedAt = acceptedAt?.toEpochMilli(),
)
