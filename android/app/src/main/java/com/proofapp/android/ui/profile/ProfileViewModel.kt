package com.proofapp.android.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.proofapp.android.data.local.Session
import com.proofapp.android.data.local.SessionStore
import com.proofapp.android.data.repository.AuthRepository
import com.proofapp.android.data.repository.SettingsRepository
import com.proofapp.android.data.repository.ThemeMode
import com.proofapp.android.data.sync.SyncScheduler
import com.proofapp.android.ui.container
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileState(
    val session: Session? = null,
    val lastSyncedAt: Long? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val busy: Boolean = false,
    val error: String? = null,
)

class ProfileViewModel(
    private val auth: AuthRepository,
    private val settings: SettingsRepository,
    private val sync: SyncScheduler,
    sessionStore: SessionStore,
) : ViewModel() {

    private val busy = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    val state: StateFlow<ProfileState> = combine(
        auth.session, sessionStore.lastSyncedAt, settings.themeMode, busy, error,
    ) { session, synced, theme, b, e ->
        ProfileState(session, synced, theme, b, e)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileState())

    fun login(email: String, password: String) = authenticate { auth.login(email, password) }

    fun register(email: String, password: String, name: String) {
        if (password.length < 8) {
            error.value = "Password must be at least 8 characters"
            return
        }
        authenticate { auth.register(email, password, name) }
    }

    private fun authenticate(block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            busy.value = true
            error.value = null
            block().onFailure { error.value = it.message }
            busy.value = false
        }
    }

    fun logout() = viewModelScope.launch { auth.logout() }
    fun syncNow() = sync.requestSync()
    fun setTheme(mode: ThemeMode) = viewModelScope.launch { settings.setThemeMode(mode) }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                ProfileViewModel(container.authRepository, container.settingsRepository, container.syncScheduler, container.sessionStore)
            }
        }
    }
}
