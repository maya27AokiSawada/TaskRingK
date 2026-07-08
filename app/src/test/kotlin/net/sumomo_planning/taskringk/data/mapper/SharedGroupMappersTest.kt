package net.sumomo_planning.taskringk.data.mapper

import java.time.Instant
import kotlinx.serialization.json.Json
import net.sumomo_planning.taskringk.domain.model.GroupType
import net.sumomo_planning.taskringk.domain.model.InvitationStatus
import net.sumomo_planning.taskringk.domain.model.SharedGroup
import net.sumomo_planning.taskringk.domain.model.SharedGroupMember
import net.sumomo_planning.taskringk.domain.model.SharedGroupRole
import net.sumomo_planning.taskringk.domain.model.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class SharedGroupMappersTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun sharedGroup_roundTripsThroughEntity() {
        val member = SharedGroupMember(
            memberId = "member-1",
            name = "Taro",
            contact = "taro@example.com",
            role = SharedGroupRole.OWNER,
            isSignedIn = true,
            invitationStatus = InvitationStatus.SELF,
            acceptedAt = Instant.parse("2026-06-30T01:00:00Z"),
        )
        val group = SharedGroup(
            groupId = "group-1",
            groupName = "Family",
            ownerUid = "owner",
            allowedUid = listOf("owner", "member"),
            members = listOf(member),
            groupType = GroupType.TODO,
            syncStatus = SyncStatus.LOCAL,
            createdAt = Instant.parse("2026-06-30T00:00:00Z"),
            updatedAt = Instant.parse("2026-06-30T02:00:00Z"),
        )

        val restored = group.toEntity(json).toDomain(json)

        assertEquals(group, restored)
    }
}
