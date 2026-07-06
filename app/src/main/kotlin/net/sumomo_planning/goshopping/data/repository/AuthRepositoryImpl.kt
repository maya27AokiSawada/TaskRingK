package net.sumomo_planning.goshopping.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import net.sumomo_planning.goshopping.data.local.prefs.UserPreferences
import net.sumomo_planning.goshopping.data.local.room.dao.SharedGroupDao
import net.sumomo_planning.goshopping.data.local.room.dao.SharedListDao
import net.sumomo_planning.goshopping.data.local.room.dao.WhiteboardDao
import net.sumomo_planning.goshopping.data.remote.auth.FirebaseAuthDataSource
import net.sumomo_planning.goshopping.domain.model.AuthUser
import net.sumomo_planning.goshopping.domain.repository.AuthRepository

/**
 * Implements [AuthRepository] with Firebase Auth + Firestore.
 *
 * Sign-up order (porting_spec §4 strict):
 *   1. clearLocalCaches()
 *   2. Auth.createUser()
 *   3. Auth.updateDisplayName()
 *   4. Firestore /users/{uid}
 *   5. UserPreferences.setUserName()
 *
 * Sign-out order (porting_spec §12-8 strict):
 *   1. clearLocalCaches()
 *   2. UserPreferences.clearAll()
 *   3. Auth.signOut()  ← MUST be last
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authDataSource: FirebaseAuthDataSource,
    private val firestore: FirebaseFirestore,
    private val userPreferences: UserPreferences,
    private val sharedGroupDao: SharedGroupDao,
    private val sharedListDao: SharedListDao,
    private val whiteboardDao: WhiteboardDao,
) : AuthRepository {

    override fun observeAuthState(): Flow<AuthUser?> = authDataSource.observeAuthState()

    override suspend fun currentUser(): AuthUser? = authDataSource.currentUser()

    override suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
    ): Result<AuthUser> = runCatching {
        // 1. Clear local caches before creating a new account
        clearLocalCaches()

        // 2. Register with Firebase Auth
        val user = authDataSource.signUp(email, password)

        // 3. Update Firebase Auth displayName
        authDataSource.updateDisplayName(displayName)

        // 4. Write /users/{uid} to Firestore
        saveUserDocument(user.uid, email, displayName)

        // 5. Persist display name locally
        userPreferences.setUserName(displayName)

        user.copy(displayName = displayName)
    }

    override suspend fun signIn(email: String, password: String): Result<AuthUser> = runCatching {
        authDataSource.signIn(email, password)
    }

    override suspend fun signOut(): Result<Unit> = runCatching {
        // 1. Clear Room caches
        clearLocalCaches()

        // 2. Clear UserPreferences
        userPreferences.clearAll()

        // 3. Sign out from Firebase Auth — MUST be last (porting_spec §12-8)
        authDataSource.signOut()
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = runCatching {
        authDataSource.sendPasswordResetEmail(email)
    }

    // ---- private helpers ----

    /** Writes a minimal user document to /users/{uid}. */
    private suspend fun saveUserDocument(uid: String, email: String, displayName: String) {
        val data = mapOf(
            "uid" to uid,
            "email" to email,
            "displayName" to displayName,
            "createdAt" to com.google.firebase.Timestamp.now(),
        )
        firestore.collection("users").document(uid).set(data).await()
    }

    private suspend fun clearLocalCaches() {
        runCatching {
            sharedGroupDao.clearAll()
            sharedListDao.clearAll()
            whiteboardDao.clearAll()
        }.onFailure { e ->
            Log.w(TAG, "clearLocalCaches failed (non-fatal)", e)
        }
    }

    companion object {
        private const val TAG = "AuthRepository"
    }
}
