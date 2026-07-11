package net.sumomo_planning.taskringk.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import net.sumomo_planning.taskringk.core.common.DeviceIdService
import net.sumomo_planning.taskringk.core.common.NotificationFactory
import net.sumomo_planning.taskringk.data.remote.firestore.FirestoreInvitationDataSource
import net.sumomo_planning.taskringk.domain.model.NotificationType
import net.sumomo_planning.taskringk.domain.model.AcceptedInvitation
import net.sumomo_planning.taskringk.domain.model.Invitation
import net.sumomo_planning.taskringk.domain.model.SharedGroup
import net.sumomo_planning.taskringk.domain.repository.NotificationRepository
import net.sumomo_planning.taskringk.domain.repository.InvitationRepository

@Singleton
class HybridInvitationRepositoryImpl @Inject constructor(
    private val firestoreInvitationDataSource: FirestoreInvitationDataSource,
    private val deviceIdService: DeviceIdService,
    private val notificationRepository: NotificationRepository,
) : InvitationRepository {
    override suspend fun createInvitation(
        group: SharedGroup,
        invitedBy: String,
        inviterName: String,
    ): Result<Invitation> = runCatching {
        val token = deviceIdService.generateInvitationToken()
        firestoreInvitationDataSource.createInvitation(
            group = group,
            invitedBy = invitedBy,
            inviterName = inviterName,
            token = token,
        )
    }

    override suspend fun validateInvitation(groupId: String, token: String): Result<Invitation> =
        runCatching { firestoreInvitationDataSource.validateInvitation(groupId, token) }

    override suspend fun acceptInvitation(
        groupId: String,
        token: String,
        acceptorUid: String,
        acceptorEmail: String,
        acceptorName: String,
    ): Result<AcceptedInvitation> = runCatching {
        val invitation = firestoreInvitationDataSource.validateInvitation(groupId, token)
        val accepted = firestoreInvitationDataSource.acceptInvitation(
            invitation = invitation,
            acceptorUid = acceptorUid,
            acceptorEmail = acceptorEmail,
            acceptorName = acceptorName,
        )

        val recipients = invitation.usedBy.filterNot { it == acceptorUid } + invitation.invitedBy
        if (recipients.isNotEmpty()) {
            notificationRepository.createNotifications(
                recipients.distinct().map { recipientUid ->
                    NotificationFactory.create(
                        userId = recipientUid,
                        type = NotificationType.MEMBER_JOINED,
                        groupId = groupId,
                        message = "$acceptorName さんがグループに参加しました",
                    )
                }
            )
        }

        accepted
    }
}