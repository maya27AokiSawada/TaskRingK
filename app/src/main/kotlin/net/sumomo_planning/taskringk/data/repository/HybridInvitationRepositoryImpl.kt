package net.sumomo_planning.taskringk.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import net.sumomo_planning.taskringk.core.common.DeviceIdService
import net.sumomo_planning.taskringk.core.common.NotificationFactory
import net.sumomo_planning.taskringk.data.remote.firestore.FirestoreInvitationDataSource
import net.sumomo_planning.taskringk.domain.model.NotificationType
import net.sumomo_planning.taskringk.domain.model.AcceptedInvitation
import net.sumomo_planning.taskringk.domain.model.Invitation
import net.sumomo_planning.taskringk.domain.model.Notification
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
        val securityKey = deviceIdService.generateSecurityKey()
        firestoreInvitationDataSource.createInvitation(
            group = group,
            invitedBy = invitedBy,
            inviterName = inviterName,
            token = token,
            securityKey = securityKey,
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
        require(invitation.invitedBy != acceptorUid) { "自分自身を招待することはできません" }

        val accepted = firestoreInvitationDataSource.processAcceptedInvitation(
            invitation = invitation,
            acceptorUid = acceptorUid,
            acceptorEmail = acceptorEmail,
            acceptorName = acceptorName,
        )

        notificationRepository.createNotifications(
            listOf(
                NotificationFactory.create(
                    userId = invitation.invitedBy,
                    type = NotificationType.INVITATION_ACCEPTED,
                    groupId = groupId,
                    message = "$acceptorName さんが「${invitation.groupName}」への参加を希望しています",
                    metadata = mapOf(
                        "invitationId" to invitation.token,
                        "acceptorUid" to acceptorUid,
                        "acceptorEmail" to acceptorEmail,
                        "acceptorName" to acceptorName,
                        "groupName" to invitation.groupName,
                    ),
                )
            )
        )

        accepted
    }

    override suspend fun processInvitationAcceptedNotification(notification: Notification): Result<Unit> = runCatching {
        require(notification.type == NotificationType.INVITATION_ACCEPTED) {
            "Unsupported notification type: ${notification.type}"
        }

        val invitationId = notification.metadata["invitationId"]
            ?: throw IllegalStateException("invitationId metadata is missing")
        val acceptorUid = notification.metadata["acceptorUid"]
            ?: throw IllegalStateException("acceptorUid metadata is missing")
        val acceptorEmail = notification.metadata["acceptorEmail"].orEmpty()
        val acceptorName = notification.metadata["acceptorName"]
            ?: throw IllegalStateException("acceptorName metadata is missing")

        val invitation = firestoreInvitationDataSource.validateInvitation(notification.groupId, invitationId)
        firestoreInvitationDataSource.processAcceptedInvitation(
            invitation = invitation,
            acceptorUid = acceptorUid,
            acceptorEmail = acceptorEmail,
            acceptorName = acceptorName,
        )

        val recipients = firestoreInvitationDataSource.getAllowedUids(notification.groupId)
            .filterNot { recipientUid -> recipientUid == acceptorUid }

        if (recipients.isNotEmpty()) {
            notificationRepository.createNotifications(
                recipients.map { recipientUid ->
                    NotificationFactory.create(
                        userId = recipientUid,
                        type = NotificationType.MEMBER_JOINED,
                        groupId = notification.groupId,
                        message = "$acceptorName さんがグループに参加しました",
                        metadata = mapOf(
                            "acceptorUid" to acceptorUid,
                            "acceptorName" to acceptorName,
                            "invitationId" to invitationId,
                        ),
                    )
                }
            )
        }
    }
}