package net.sumomo_planning.taskringk

import android.app.Application
import android.util.Log
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import com.google.firebase.Firebase
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point.
 *
 * Responsibilities (Phase 0):
 * - Enable Hilt dependency injection ([HiltAndroidApp]).
 * - Enable Firestore offline persistence so reads/writes are queued locally and
 *   synced when connectivity returns (porting_spec §12-3).
 */
@HiltAndroidApp
class GoShopApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        configureFirestore()
    }

    private fun configureFirestore() {
        runCatching {
            Firebase.firestore.firestoreSettings = firestoreSettings {
                // Persistent local cache = Firestore offline persistence.
                setLocalCacheSettings(persistentCacheSettings {})
            }
            Log.i(TAG, "Firestore offline persistence enabled.")
        }.onFailure { e ->
            Log.e(TAG, "Failed to configure Firestore settings.", e)
        }
    }

    companion object {
        private const val TAG = "GoShopApplication"
    }
}
