package net.sumomo_planning.taskringk.core.common

import kotlin.random.Random
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceIdService @Inject constructor() {

    private val securityChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

    fun generateGroupId(devicePrefix: String, nowMillis: Long = System.currentTimeMillis()): String =
        "${devicePrefix}_$nowMillis"

    fun generateListId(devicePrefix: String): String = "${devicePrefix}_${uuid8()}"

    fun generateItemId(): String = UUID.randomUUID().toString()

    fun generateMemberId(): String = UUID.randomUUID().toString()

    fun generateInvitationToken(): String = "INV_${UUID.randomUUID()}"

    fun generateSecurityKey(length: Int = 32): String =
        buildString(length) {
            repeat(length) {
                append(securityChars[Random.nextInt(securityChars.length)])
            }
        }

    private fun uuid8(): String = UUID.randomUUID().toString().replace("-", "").substring(0, 8)
}
