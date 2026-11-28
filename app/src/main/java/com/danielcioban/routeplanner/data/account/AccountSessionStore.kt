package com.danielcioban.routeplanner.data.account

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.accountDataStore: DataStore<Preferences> by preferencesDataStore(name = "account_session")

/**
 * Persists signed-in identity metadata only (display name, email, provider).
 * Auth tokens belong in Credential Manager / EncryptedSharedPreferences when P4 lands.
 */
class AccountSessionStore(private val context: Context) {
    private object Keys {
        val displayName = stringPreferencesKey("display_name")
        val email = stringPreferencesKey("email")
        val providerId = stringPreferencesKey("provider_id")
        val userId = stringPreferencesKey("user_id")
    }

    val session: Flow<AccountSession> = context.accountDataStore.data.map { prefs ->
        val email = prefs[Keys.email].orEmpty()
        val userId = prefs[Keys.userId].orEmpty()
        val provider = prefs[Keys.providerId].orEmpty().ifBlank { PREVIEW_PROVIDER }
        if (email.isBlank() && userId.isBlank()) {
            AccountSession.SignedOut
        } else {
            AccountSession.SignedIn(
                userId = userId,
                displayName = prefs[Keys.displayName].orEmpty().ifBlank { email.ifBlank { "Google account" } },
                email = email,
                providerId = provider,
            )
        }
    }

    suspend fun signInPreview(displayName: String, email: String) {
        val trimmedEmail = email.trim()
        require(trimmedEmail.isNotEmpty()) { "Email required" }
        context.accountDataStore.edit {
            it.remove(Keys.userId)
            it[Keys.displayName] = displayName.trim()
            it[Keys.email] = trimmedEmail
            it[Keys.providerId] = PREVIEW_PROVIDER
        }
    }

    suspend fun signInDeveloper() {
        context.accountDataStore.edit {
            it.remove(Keys.userId)
            it[Keys.displayName] = "Developer"
            it[Keys.email] = "dev@local"
            it[Keys.providerId] = DEVELOPER_PROVIDER
        }
    }

    suspend fun signOut() {
        context.accountDataStore.edit { it.clear() }
    }

    suspend fun saveFirebaseSession(userId: String, displayName: String?, email: String?) {
        val safeEmail = email.orEmpty().trim()
        require(userId.isNotBlank()) { "Firebase user id required" }
        context.accountDataStore.edit {
            it[Keys.userId] = userId
            it[Keys.displayName] = displayName.orEmpty().trim().ifBlank { safeEmail }
            it[Keys.email] = safeEmail
            it[Keys.providerId] = GOOGLE_PROVIDER
        }
    }

    /** Does not erase a local preview/developer session when Firebase is unavailable. */
    suspend fun clearFirebaseSession() {
        context.accountDataStore.edit { prefs ->
            if (prefs[Keys.providerId] == GOOGLE_PROVIDER) prefs.clear()
        }
    }

    companion object {
        const val DEVELOPER_PROVIDER = "developer"
        const val PREVIEW_PROVIDER = "preview"
        const val GOOGLE_PROVIDER = "google"
    }
}
