package com.danielcioban.routeplanner.data.account

import com.danielcioban.routeplanner.BuildConfig

/** Local account state — no tokens until a real auth provider is wired. */
sealed interface AccountSession {
    data object SignedOut : AccountSession

    data class SignedIn(
        val displayName: String,
        val email: String,
        /** Provider slug, e.g. `preview`, `developer`, `google`. */
        val providerId: String,
    ) : AccountSession
}

/** Debug developer tools. Always false in release, even if leftover DataStore says otherwise. */
fun AccountSession.isDebugDeveloper(): Boolean =
    BuildConfig.DEBUG &&
        this is AccountSession.SignedIn &&
        providerId == AccountSessionStore.DEVELOPER_PROVIDER

