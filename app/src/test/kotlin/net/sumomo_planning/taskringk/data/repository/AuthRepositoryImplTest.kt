package net.sumomo_planning.taskringk.data.repository

import app.cash.turbine.test
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.sumomo_planning.taskringk.data.local.prefs.UserPreferences
import net.sumomo_planning.taskringk.data.local.room.dao.SharedGroupDao
import net.sumomo_planning.taskringk.data.local.room.dao.SharedListDao
import net.sumomo_planning.taskringk.data.local.room.dao.WhiteboardDao
import net.sumomo_planning.taskringk.data.remote.auth.AuthException
import net.sumomo_planning.taskringk.data.remote.auth.FirebaseAuthDataSource
import net.sumomo_planning.taskringk.domain.model.AuthUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositoryImplTest {

    private val authDataSource = mockk<FirebaseAuthDataSource>()
    private val firestoreDocRef = mockk<DocumentReference>()
    private val firestoreColRef = mockk<CollectionReference>()
    private val firestore = mockk<FirebaseFirestore>()
    private val userPreferences = mockk<UserPreferences>()
    private val sharedGroupDao = mockk<SharedGroupDao>()
    private val sharedListDao = mockk<SharedListDao>()
    private val whiteboardDao = mockk<WhiteboardDao>()

    private lateinit var repository: AuthRepositoryImpl

    private val fakeUser = AuthUser(uid = "uid-1", email = "test@example.com", displayName = null)

    @Before
    fun setUp() {
        // Wire up firestore mock chain: collection() -> document() -> set() -> completed Task
        every { firestore.collection(any()) } returns firestoreColRef
        every { firestoreColRef.document(any()) } returns firestoreDocRef
        every { firestoreDocRef.set(any()) } returns Tasks.forResult(null)

        repository = AuthRepositoryImpl(
            authDataSource = authDataSource,
            firestore = firestore,
            userPreferences = userPreferences,
            sharedGroupDao = sharedGroupDao,
            sharedListDao = sharedListDao,
            whiteboardDao = whiteboardDao,
        )
    }

    // ---- observeAuthState ----

    @Test
    fun `observeAuthState emits null when signed out`() = runTest {
        every { authDataSource.observeAuthState() } returns flowOf(null)
        repository.observeAuthState().test {
            assertNull(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `observeAuthState emits user when signed in`() = runTest {
        every { authDataSource.observeAuthState() } returns flowOf(fakeUser)
        repository.observeAuthState().test {
            assertEquals(fakeUser, awaitItem())
            awaitComplete()
        }
    }

    // ---- signIn ----

    @Test
    fun `signIn returns success on valid credentials`() = runTest {
        coEvery { authDataSource.signIn(any(), any()) } returns fakeUser
        val result = repository.signIn("test@example.com", "password123")
        assertTrue(result.isSuccess)
        assertEquals(fakeUser, result.getOrNull())
    }

    @Test
    fun `signIn returns failure on wrong password`() = runTest {
        val error = AuthException("パスワードが間違っています", "ERROR_WRONG_PASSWORD")
        coEvery { authDataSource.signIn(any(), any()) } throws error
        val result = repository.signIn("test@example.com", "wrong")
        assertTrue(result.isFailure)
        assertEquals("パスワードが間違っています", result.exceptionOrNull()?.message)
    }

    // ---- signUp ----

    @Test
    fun `signUp calls clearLocalCaches then signUp then updateDisplayName then saveUser then setUserName`() = runTest {
        coJustRun { sharedGroupDao.clearAll() }
        coJustRun { sharedListDao.clearAll() }
        coJustRun { whiteboardDao.clearAll() }
        coEvery { authDataSource.signUp(any(), any()) } returns fakeUser
        coJustRun { authDataSource.updateDisplayName(any()) }
        // Firestore set() call is relaxed (mockk(relaxed=true))
        coJustRun { userPreferences.setUserName(any()) }

        val result = repository.signUp("test@example.com", "password123", "TestUser")
        assertTrue(result.isSuccess)

        // Verify the strict order: caches cleared BEFORE signUp
        coVerify(ordering = io.mockk.Ordering.ORDERED) {
            sharedGroupDao.clearAll()
            sharedListDao.clearAll()
            whiteboardDao.clearAll()
            authDataSource.signUp("test@example.com", "password123")
            authDataSource.updateDisplayName("TestUser")
            userPreferences.setUserName("TestUser")
        }
    }

    // ---- signOut ----

    @Test
    fun `signOut clears caches and prefs before calling auth signOut`() = runTest {
        coJustRun { sharedGroupDao.clearAll() }
        coJustRun { sharedListDao.clearAll() }
        coJustRun { whiteboardDao.clearAll() }
        coJustRun { userPreferences.clearAll() }
        every { authDataSource.signOut() } returns Unit

        val result = repository.signOut()
        assertTrue(result.isSuccess)

        // porting_spec §12-8: signOut must be called LAST
        coVerify(ordering = io.mockk.Ordering.ORDERED) {
            sharedGroupDao.clearAll()
            sharedListDao.clearAll()
            whiteboardDao.clearAll()
            userPreferences.clearAll()
            authDataSource.signOut()
        }
    }

    // ---- sendPasswordResetEmail ----

    @Test
    fun `sendPasswordResetEmail returns success`() = runTest {
        coJustRun { authDataSource.sendPasswordResetEmail(any()) }
        val result = repository.sendPasswordResetEmail("test@example.com")
        assertTrue(result.isSuccess)
    }
}
