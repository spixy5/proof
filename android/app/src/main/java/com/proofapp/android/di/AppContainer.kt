package com.proofapp.android.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.proofapp.android.BuildConfig
import com.proofapp.android.data.local.ProofDatabase
import com.proofapp.android.data.local.SessionStore
import com.proofapp.android.data.remote.AuthInterceptor
import com.proofapp.android.data.remote.ProofApi
import com.proofapp.android.data.repository.AuthRepository
import com.proofapp.android.data.repository.RecipeRepository
import com.proofapp.android.data.repository.SettingsRepository
import com.proofapp.android.data.repository.ShoppingRepository
import com.proofapp.android.data.sync.SyncScheduler
import com.proofapp.android.ui.cooking.TimerAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "proof")

/**
 * Hand-rolled dependency injection. One place builds the object graph, every ViewModel asks for
 * what it needs. Easy to read, no annotation processing, and trivial to swap fakes in tests.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val database: ProofDatabase =
        Room.databaseBuilder(appContext, ProofDatabase::class.java, "proof.db").build()

    val sessionStore = SessionStore(appContext.dataStore, applicationScope)
    val settingsRepository = SettingsRepository(appContext.dataStore)
    val syncScheduler = SyncScheduler(appContext)
    val timerAlarms = TimerAlarmScheduler(appContext)

    private val okHttp = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(sessionStore))
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
            }
        }
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val api: ProofApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(okHttp)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(ProofApi::class.java)

    val recipeRepository = RecipeRepository(
        context = appContext,
        api = api,
        recipeDao = database.recipeDao(),
        favoriteDao = database.favoriteDao(),
        sessionStore = sessionStore,
        syncScheduler = syncScheduler,
        json = json,
    )

    val authRepository = AuthRepository(api, sessionStore, syncScheduler, json)
    val shoppingRepository = ShoppingRepository(database.shoppingDao())
}
