package net.sumomo_planning.goshopping.domain.model

/**
 * Authenticated user representation in the domain layer.
 *
 * Intentionally thin — only the fields the domain layer cares about.
 */
data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
)
