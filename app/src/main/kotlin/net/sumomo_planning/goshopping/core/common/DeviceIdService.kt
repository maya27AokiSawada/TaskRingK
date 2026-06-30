package net.sumomo_planning.goshopping.core.common

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceIdService @Inject constructor() {

    fun generateGroupId(devicePrefix: String, nowMillis: Long = System.currentTimeMillis()): String =
        "${devicePrefix}_$nowMillis"

    fun generateListId(devicePrefix: String): String = "${devicePrefix}_${uuid8()}"

    fun generateItemId(): String = UUID.randomUUID().toString()

    fun generateMemberId(): String = UUID.randomUUID().toString()

    fun generateInvitationToken(): String = "INV_${UUID.randomUUID()}"

    private fun uuid8(): String = UUID.randomUUID().toString().replace("-", "").substring(0, 8)
}
