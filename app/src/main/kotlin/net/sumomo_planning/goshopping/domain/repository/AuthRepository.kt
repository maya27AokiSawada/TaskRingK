package net.sumomo_planning.goshopping.domain.repository

import kotlinx.coroutines.flow.Flow
import net.sumomo_planning.goshopping.domain.model.AuthUser

/**
 * Authentication operations exposed to the domain layer.
 *
 * All methods return [Result] so callers can handle errors without try/catch.
 * [observeAuthState] emits null when the user is signed out.
 */
interface AuthRepository {

    /** Emits the current [AuthUser], or null when unauthenticated. */
    fun observeAuthState(): Flow<AuthUser?>

    /** Returns the currently signed-in user, or null. */
    suspend fun currentUser(): AuthUser?

    /**
     * Sign up with email/password.
     *
     * Sequence (porting_spec §4 strict order):
     * 1. Clear all local caches.
     * 2. Register with Firebase Auth.
     * 3. Save display name to Firebase Auth profile.
     * 4. Write /users/{uid} to Firestore.
     * 5. Persist display name to UserPreferences.
     */
    suspend fun signUp(email: String, password: String, displayName: String): Result<AuthUser>

    /**
     * Sign in with email/password.
     */
    suspend fun signIn(email: String, password: String): Result<AuthUser>

    /**
     * Sign out.
     *
     * Sequence (porting_spec §12-8 strict order):
     * 1. Clear all local Room caches.
     * 2. Reset UserPreferences.
     * 3. Call Firebase Auth signOut() **last**.
     */
    suspend fun signOut(): Result<Unit>

    /**
     * Send a password-reset email (porting_spec §4-6).
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
}
