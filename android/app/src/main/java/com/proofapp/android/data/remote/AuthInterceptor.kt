package com.proofapp.android.data.remote

import com.proofapp.android.data.local.SessionStore
import okhttp3.Interceptor
import okhttp3.Response

/** Adds `Authorization: Bearer <jwt>` to every request when the user is signed in. */
class AuthInterceptor(private val sessionStore: SessionStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = sessionStore.cachedToken
        val request = if (token != null) {
            chain.request().newBuilder().header("Authorization", "Bearer $token").build()
        } else {
            chain.request()
        }
        val response = chain.proceed(request)
        // An expired or revoked token: drop it so the app falls back to guest mode cleanly.
        if (response.code == 401 && token != null) sessionStore.invalidateAsync()
        return response
    }
}
