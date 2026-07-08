package net.sumomo_planning.taskringk.data.local.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val userName: Flow<String?> = context.userPreferencesDataStore.data.map { it[KEY_USER_NAME] }

    val uiMode: Flow<UiMode> = context.userPreferencesDataStore.data.map { preferences ->
        UiMode.fromStorageValue(preferences[KEY_UI_MODE])
    }

    val appMode: Flow<AppMode> = context.userPreferencesDataStore.data.map { preferences ->
        AppMode.fromStorageValue(preferences[KEY_APP_MODE])
    }

    suspend fun setUserName(name: String) {
        context.userPreferencesDataStore.edit { it[KEY_USER_NAME] = name }
    }

    suspend fun setUiMode(mode: UiMode) {
        context.userPreferencesDataStore.edit { it[KEY_UI_MODE] = mode.storageValue }
    }

    suspend fun setAppMode(mode: AppMode) {
        context.userPreferencesDataStore.edit { it[KEY_APP_MODE] = mode.storageValue }
    }

    suspend fun clearAll() {
        context.userPreferencesDataStore.edit { it.clear() }
    }

    private companion object {
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_UI_MODE = stringPreferencesKey("ui_mode")
        val KEY_APP_MODE = stringPreferencesKey("app_mode")
    }
}

enum class UiMode(val storageValue: String) {
    SINGLE("single"),
    MULTI("multi");

    companion object {
        fun fromStorageValue(value: String?): UiMode = when (value) {
            SINGLE.storageValue -> SINGLE
            else -> MULTI
        }
    }
}

enum class AppMode(val storageValue: String) {
    SHOPPING("shopping"),
    TODO("todo");

    companion object {
        fun fromStorageValue(value: String?): AppMode = when (value) {
            TODO.storageValue -> TODO
            else -> SHOPPING
        }
    }
}
