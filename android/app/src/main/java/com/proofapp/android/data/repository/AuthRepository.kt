package com.proofapp.android.data.repository

import com.proofapp.android.data.local.Session
import com.proofapp.android.data.local.SessionStore
import com.proofapp.android.data.remote.ApiError
import com.proofapp.android.data.remote.AuthResponse
import com.proofapp.android.data.remote.LoginRequest
import com.proofapp.android.data.remote.ProofApi
import com.proofapp.android.data.remote.RegisterRequest
import com.proofapp.android.data.sync.SyncScheduler
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

class AuthRepository(
    private val api: ProofApi,
    private val sessionStore: SessionStore,
    private val syncScheduler: SyncScheduler,
    private val json: Json,
) {
    val session: StateFlow<Session?> = sessionStore.session

    suspend fun login(email: String, password: String): Result<Unit> =
        authenticate { api.login(LoginRequest(email.trim(), password)) }

    suspend fun register(email: String, password: String, displayName: String): Result<Unit> =
        authenticate { api.register(RegisterRequest(email.trim(), password, displayName.trim())) }

    suspend fun logout() = sessionStore.clear()

    private suspend fun authenticate(call: suspend () -> AuthResponse): Result<Unit> = try {
        val response = call()
        sessionStore.save(Session(response.token, response.user.email, response.user.displayName))
        // Anything favourited while signed out is pushed straight away.
        syncScheduler.requestSync()
        Result.success(Unit)
    } catch (e: HttpException) {
        val message = e.response()?.errorBody()?.string()
            ?.let { runCatching { json.decodeFromString<ApiError>(it).message }.getOrNull() }
            ?.takeIf { it.isNotBlank() }
            ?: if (e.code() == 401) "Wrong email or password" else "Something went wrong (${e.code()})"
        Result.failure(AuthException(message))
    } catch (_: IOException) {
        Result.failure(AuthException("Can't reach the Proof server. Check your connection."))
    }
}

class AuthException(message: String) : Exception(message)
