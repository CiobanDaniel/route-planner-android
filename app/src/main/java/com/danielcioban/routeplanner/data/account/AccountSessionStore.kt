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
    }

    val session: Flow<AccountSession> = context.accountDataStore.data.map { prefs ->
        val email = prefs[Keys.email].orEmpty()
        if (email.isBlank()) {
            AccountSession.SignedOut
        } else {
            AccountSession.SignedIn(
                displayName = prefs[Keys.displayName].orEmpty().ifBlank { email },
                email = email,
                providerId = prefs[Keys.providerId].orEmpty().ifBlank { "preview" },
            )
        }
    }

    suspend fun signInPreview(displayName: String, email: String) {
        val trimmedEmail = email.trim()
        require(trimmedEmail.isNotEmpty()) { "Email required" }
        context.accountDataStore.edit {
            it[Keys.displayName] = displayName.trim()
            it[Keys.email] = trimmedEmail
            it[Keys.providerId] = "preview"
        }
    }

    suspend fun signOut() {
        context.accountDataStore.edit { it.clear() }
    }
}
