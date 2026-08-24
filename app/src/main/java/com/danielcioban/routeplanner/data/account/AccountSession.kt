package com.danielcioban.routeplanner.data.account

/** Local account state — no tokens until a real auth provider is wired. */
sealed interface AccountSession {
    data object SignedOut : AccountSession

    data class SignedIn(
        val displayName: String,
        val email: String,
        /** Provider slug, e.g. `preview`, `firebase`, `entra`. */
        val providerId: String,
    ) : AccountSession
}
