package net.sumomo_planning.taskringk.data.remote.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.UserProfileChangeRequest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import net.sumomo_planning.taskringk.domain.model.AuthUser

/**
 * Wraps Firebase Auth operations.
 *
 * All suspend functions catch [FirebaseAuthException] and rethrow as
 * [AuthException] with a localised Japanese message (porting_spec §4-7).
 */
@Singleton
class FirebaseAuthDataSource @Inject constructor(
    private val auth: FirebaseAuth,
) {
    /** Emits the current user, or null when signed out. */
    fun observeAuthState(): Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.toAuthUser())
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    fun currentUser(): AuthUser? = auth.currentUser?.toAuthUser()

    suspend fun signUp(email: String, password: String): AuthUser {
        return runCatchingAuth {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.toAuthUser() ?: throw IllegalStateException("User is null after signup")
        }
    }

    suspend fun updateDisplayName(displayName: String) {
        runCatchingAuth {
            val request = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            auth.currentUser?.updateProfile(request)?.await()
                ?: throw IllegalStateException("No current user")
        }
    }

    suspend fun signIn(email: String, password: String): AuthUser {
        return runCatchingAuth {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.toAuthUser() ?: throw IllegalStateException("User is null after sign-in")
        }
    }

    fun signOut() {
        auth.signOut()
    }

    suspend fun sendPasswordResetEmail(email: String) {
        runCatchingAuth {
            auth.sendPasswordResetEmail(email).await()
        }
    }

    // ---- helpers ----

    private fun com.google.firebase.auth.FirebaseUser.toAuthUser() = AuthUser(
        uid = uid,
        email = email,
        displayName = displayName,
    )

    /**
     * Executes [block] and converts [FirebaseAuthException] to [AuthException]
     * with a Japanese message (porting_spec §4-7).
     */
    private suspend fun <T> runCatchingAuth(block: suspend () -> T): T {
        return try {
            block()
        } catch (e: FirebaseAuthException) {
            throw AuthException(toJapaneseMessage(e.errorCode), e.errorCode, e)
        }
    }

    private fun toJapaneseMessage(errorCode: String): String = when (errorCode) {
        "ERROR_EMAIL_ALREADY_IN_USE"    -> "このメールアドレスはすでに使用されています"
        "ERROR_INVALID_EMAIL"           -> "メールアドレスの形式が正しくありません"
        "ERROR_WEAK_PASSWORD"           -> "パスワードは6文字以上にしてください"
        "ERROR_WRONG_PASSWORD"          -> "パスワードが間違っています"
        "ERROR_USER_NOT_FOUND"          -> "このメールアドレスは登録されていません"
        "ERROR_USER_DISABLED"           -> "このアカウントは無効化されています"
        "ERROR_TOO_MANY_REQUESTS"       -> "しばらく時間をおいてからお試しください"
        "ERROR_OPERATION_NOT_ALLOWED"   -> "この操作は許可されていません"
        "ERROR_NETWORK_REQUEST_FAILED"  -> "ネットワークエラーが発生しました"
        "ERROR_INVALID_CREDENTIAL"      -> "メールアドレスまたはパスワードが正しくありません"
        else                            -> "認証エラーが発生しました（$errorCode）"
    }
}

/** Domain-facing exception carrying a localised [message]. */
class AuthException(
    override val message: String,
    val errorCode: String,
    cause: Throwable? = null,
) : Exception(message, cause)
