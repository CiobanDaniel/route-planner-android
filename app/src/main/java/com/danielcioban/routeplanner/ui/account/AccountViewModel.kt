package com.danielcioban.routeplanner.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.danielcioban.routeplanner.data.account.AccountSession
import com.danielcioban.routeplanner.data.account.AccountSessionStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AccountViewModel(
    private val accountSessionStore: AccountSessionStore,
) : ViewModel() {
    val session: StateFlow<AccountSession> = accountSessionStore.session
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountSession.SignedOut)

    fun previewSignIn(displayName: String, email: String) {
        viewModelScope.launch {
            accountSessionStore.signInPreview(displayName, email)
        }
    }

    fun signOut() {
        viewModelScope.launch { accountSessionStore.signOut() }
    }

    class Factory(
        private val accountSessionStore: AccountSessionStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AccountViewModel(accountSessionStore) as T
        }
    }
}
