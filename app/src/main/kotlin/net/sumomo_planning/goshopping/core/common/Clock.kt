package net.sumomo_planning.goshopping.core.common

import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

interface Clock {
    fun now(): Instant
}

@Singleton
class SystemClock @Inject constructor() : Clock {
    override fun now(): Instant = Instant.now()
}
