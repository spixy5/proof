package com.proofapp.android.data.repository

import android.content.Context
import com.proofapp.android.data.local.FavoriteDao
import com.proofapp.android.data.local.FavoriteEntity
import com.proofapp.android.data.local.RecipeDao
import com.proofapp.android.data.local.RecipeEntity
import com.proofapp.android.data.local.SessionStore
import com.proofapp.android.data.remote.ProofApi
import com.proofapp.android.data.remote.RecipeDto
import com.proofapp.android.data.sync.SyncScheduler
import com.proofapp.android.domain.model.Category
import com.proofapp.android.domain.model.Difficulty
import com.proofapp.android.domain.model.Recipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Offline-first: the UI only ever observes Room. The network is used to refresh Room in the
 * background, so every screen works on a plane and updates itself the moment a sync lands.
 */
class RecipeRepository(
    private val context: Context,
    private val api: ProofApi,
    private val recipeDao: RecipeDao,
    private val favoriteDao: FavoriteDao,
    private val sessionStore: SessionStore,
    private val syncScheduler: SyncScheduler,
    private val json: Json,
) {

    fun recipes(query: String, category: Category?): Flow<List<Recipe>> =
        recipeDao.observe(query.trim(), category).map { rows -> rows.map { it.toDomain() } }

    fun recipe(id: String): Flow<Recipe?> = recipeDao.observeById(id).map { it?.toDomain() }

    fun favorites(): Flow<List<Recipe>> =
        recipeDao.observeFavorites().map { rows -> rows.map { it.toDomain() } }

    val pendingFavoriteChanges: Flow<Int> = favoriteDao.observePendingCount()

    /** First launch with no network: load the bundled recipes so the app is never empty. */
    suspend fun seedIfEmpty() = withContext(Dispatchers.IO) {
        if (recipeDao.count() > 0) return@withContext
        val text = context.assets.open("seed_recipes.json").bufferedReader().use { it.readText() }
        val seed = json.decodeFromString<List<RecipeDto>>(text)
        recipeDao.upsertAll(seed.map { it.toEntity() })
    }

    suspend fun refresh(): Result<Unit> = runCatching {
        val remote = api.recipes()
        recipeDao.upsertAll(remote.map { it.toEntity() })
    }

    suspend fun setFavorite(recipeId: String, favorite: Boolean) {
        favoriteDao.upsert(
            FavoriteEntity(recipeId, favorite, pendingSync = true, updatedAt = System.currentTimeMillis())
        )
        syncScheduler.requestSync()
    }

    /** Push local favourite changes, then pull the server's view. Only runs when signed in. */
    suspend fun syncFavorites() {
        if (sessionStore.cachedToken == null) return
        for (change in favoriteDao.pending()) {
            if (change.isFavorite) api.addFavorite(change.recipeId) else api.removeFavorite(change.recipeId)
            favoriteDao.markSynced(change.recipeId, change.updatedAt)
        }
        favoriteDao.mergeRemote(api.favorites().toSet(), System.currentTimeMillis())
        sessionStore.markSynced(System.currentTimeMillis())
    }
}

internal fun RecipeDto.toEntity() = RecipeEntity(
    id = id,
    title = title,
    description = description,
    category = runCatching { Category.valueOf(category) }.getOrDefault(Category.DESSERTS),
    emoji = emoji,
    servings = servings,
    yieldUnit = yieldUnit,
    prepMinutes = prepMinutes,
    cookMinutes = cookMinutes,
    difficulty = runCatching { Difficulty.valueOf(difficulty) }.getOrDefault(Difficulty.MEDIUM),
    ingredients = ingredients,
    steps = steps,
    searchText = (listOf(description) + ingredients.map { it.name }).joinToString(" ").lowercase(),
    updatedAt = System.currentTimeMillis(),
)
