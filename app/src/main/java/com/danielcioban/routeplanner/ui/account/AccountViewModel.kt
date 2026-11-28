package com.danielcioban.routeplanner.ui.account

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.danielcioban.routeplanner.data.account.AccountSession
import com.danielcioban.routeplanner.data.account.AccountSessionStore
import com.danielcioban.routeplanner.data.account.FirebaseAccountAuthenticator
import com.danielcioban.routeplanner.data.account.GoogleSignInResult
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AccountViewModel(
    private val accountSessionStore: AccountSessionStore,
    private val authenticator: FirebaseAccountAuthenticator,
) : ViewModel() {
    val session: StateFlow<AccountSession> = accountSessionStore.session
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountSession.SignedOut)

    private val _authResult = kotlinx.coroutines.flow.MutableStateFlow<GoogleSignInResult?>(null)
    val authResult: StateFlow<GoogleSignInResult?> = _authResult
    private val _busy = kotlinx.coroutines.flow.MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    fun previewSignIn(displayName: String, email: String) {
        viewModelScope.launch {
            accountSessionStore.signInPreview(displayName, email)
        }
    }

    fun signInDeveloper() {
        viewModelScope.launch { accountSessionStore.signInDeveloper() }
    }

    fun signInWithGoogle(context: Context) {
        if (_busy.value) return
        _busy.value = true
        _authResult.value = null
        viewModelScope.launch {
            try { _authResult.value = authenticator.signInWithGoogle(context) } finally { _busy.value = false }
        }
    }

    fun signOut(context: Context) {
        _authResult.value = null
        viewModelScope.launch { authenticator.signOut(context) }
    }

    fun deleteGoogleAccount(context: Context) {
        if (_busy.value) return
        _busy.value = true
        _authResult.value = null
        viewModelScope.launch {
            try { _authResult.value = authenticator.deleteGoogleAccount(context) } finally { _busy.value = false }
        }
    }

    fun clearAuthResult() {
        _authResult.value = null
    }

    class Factory(
        private val accountSessionStore: AccountSessionStore,
        private val authenticator: FirebaseAccountAuthenticator,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AccountViewModel(accountSessionStore, authenticator) as T
        }
    }
}
