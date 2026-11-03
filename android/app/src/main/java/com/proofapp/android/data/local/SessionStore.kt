package com.proofapp.android.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class Session(val token: String, val email: String, val displayName: String)

/** Persists the JWT and profile with DataStore, and keeps the token in memory for the interceptor. */
class SessionStore(
    private val dataStore: DataStore<Preferences>,
    private val scope: CoroutineScope,
) {
    private object Keys {
        val token = stringPreferencesKey("session_token")
        val email = stringPreferencesKey("session_email")
        val name = stringPreferencesKey("session_name")
        val lastSync = longPreferencesKey("last_synced_at")
    }

    val session: StateFlow<Session?> = dataStore.data
        .map { prefs ->
            val token = prefs[Keys.token] ?: return@map null
            Session(token, prefs[Keys.email].orEmpty(), prefs[Keys.name].orEmpty())
        }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, null)

    val lastSyncedAt: Flow<Long?> = dataStore.data.map { it[Keys.lastSync] }

    /** Read on OkHttp's thread, so it must never block on disk. */
    val cachedToken: String? get() = session.value?.token

    suspend fun save(session: Session) {
        dataStore.edit {
            it[Keys.token] = session.token
            it[Keys.email] = session.email
            it[Keys.name] = session.displayName
        }
    }

    suspend fun clear() {
        dataStore.edit {
            it.remove(Keys.token)
            it.remove(Keys.email)
            it.remove(Keys.name)
            it.remove(Keys.lastSync)
        }
    }

    fun invalidateAsync() {
        scope.launch { clear() }
    }

    suspend fun markSynced(at: Long) {
        dataStore.edit { it[Keys.lastSync] = at }
    }
}
