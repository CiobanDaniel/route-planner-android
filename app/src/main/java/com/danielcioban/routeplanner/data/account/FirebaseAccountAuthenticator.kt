package com.danielcioban.routeplanner.data.account

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import com.danielcioban.routeplanner.data.google.GoogleServices
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

sealed interface GoogleSignInResult {
    data object Success : GoogleSignInResult
    data object NotConfigured : GoogleSignInResult
    data object RecentLoginRequired : GoogleSignInResult
    data class Failure(val cause: Throwable? = null) : GoogleSignInResult
}

/** Firebase Auth + Credential Manager, isolated from Compose and route data. */
class FirebaseAccountAuthenticator(
    private val appContext: Context,
    private val sessionStore: AccountSessionStore,
) {
    suspend fun restoreSession() {
        if (!GoogleServices.isFirebaseReady(appContext)) return
        val user = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()
        if (user == null) {
            sessionStore.clearFirebaseSession()
        } else {
            sessionStore.saveFirebaseSession(user.uid, user.displayName, user.email)
        }
    }

    suspend fun signInWithGoogle(context: Context): GoogleSignInResult {
        if (!GoogleServices.isFirebaseReady(context) || !GoogleServices.isSignInAvailable(context)) {
            return GoogleSignInResult.NotConfigured
        }
        return try {
            val manager = CredentialManager.create(context)
            val credential = try {
                requestCredential(manager, context, authorizedAccountsOnly = true)
            } catch (_: NoCredentialException) {
                requestCredential(manager, context, authorizedAccountsOnly = false)
            }
            if (credential !is CustomCredential || credential.type != TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                return GoogleSignInResult.Failure()
            }
            val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
            val firebaseUser = FirebaseAuth.getInstance()
                .signInWithCredential(GoogleAuthProvider.getCredential(token, null))
                .await()
                .user
                ?: return GoogleSignInResult.Failure()
            sessionStore.saveFirebaseSession(firebaseUser.uid, firebaseUser.displayName, firebaseUser.email)
            GoogleSignInResult.Success
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            GoogleSignInResult.Failure(error)
        }
    }

    suspend fun signOut(context: Context) {
        if (GoogleServices.isFirebaseReady(context)) {
            runCatching { FirebaseAuth.getInstance().signOut() }
            runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
        }
        sessionStore.signOut()
    }

    suspend fun deleteGoogleAccount(context: Context): GoogleSignInResult {
        if (!GoogleServices.isFirebaseReady(context)) return GoogleSignInResult.NotConfigured
        return try {
            val user = FirebaseAuth.getInstance().currentUser ?: return GoogleSignInResult.Failure()
            user.delete().await()
            signOut(context)
            GoogleSignInResult.Success
        } catch (error: CancellationException) {
            throw error
        } catch (error: FirebaseAuthRecentLoginRequiredException) {
            GoogleSignInResult.RecentLoginRequired
        } catch (error: Exception) {
            GoogleSignInResult.Failure(error)
        }
    }

    private suspend fun requestCredential(
        manager: CredentialManager,
        context: Context,
        authorizedAccountsOnly: Boolean,
    ) = manager.getCredential(
        context = context,
        request = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(authorizedAccountsOnly)
                    .setServerClientId(GoogleServices.webClientId(context))
                    .setAutoSelectEnabled(false)
                    .build(),
            )
            .build(),
    ).credential
}
