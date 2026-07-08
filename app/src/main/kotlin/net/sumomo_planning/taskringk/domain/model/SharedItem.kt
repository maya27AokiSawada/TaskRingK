package net.sumomo_planning.taskringk.domain.model

import java.time.Instant

data class SharedItem(
    val itemId: String,
    val name: String,
    val quantity: Int = 1,
    val memberId: String,
    val registeredDate: Instant,
    val purchaseDate: Instant? = null,
    val isPurchased: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Instant? = null,
    val shoppingInterval: Int = 0,
    val deadline: Instant? = null,
)
