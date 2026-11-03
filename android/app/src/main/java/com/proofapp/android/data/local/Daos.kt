package com.proofapp.android.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.proofapp.android.domain.model.Category
import com.proofapp.android.domain.model.MeasureUnit
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {

    @Query(
        """
        SELECT r.*, COALESCE(f.isFavorite, 0) AS isFavorite
        FROM recipes r LEFT JOIN favorites f ON f.recipeId = r.id
        WHERE (:category IS NULL OR r.category = :category)
          AND (:query = '' OR r.title LIKE '%' || :query || '%' OR r.searchText LIKE '%' || :query || '%')
        ORDER BY r.title COLLATE NOCASE
        """
    )
    fun observe(query: String, category: Category?): Flow<List<RecipeWithFavorite>>

    @Query(
        """
        SELECT r.*, COALESCE(f.isFavorite, 0) AS isFavorite
        FROM recipes r LEFT JOIN favorites f ON f.recipeId = r.id
        WHERE r.id = :id
        """
    )
    fun observeById(id: String): Flow<RecipeWithFavorite?>

    @Query(
        """
        SELECT r.*, 1 AS isFavorite
        FROM recipes r INNER JOIN favorites f ON f.recipeId = r.id
        WHERE f.isFavorite = 1
        ORDER BY f.updatedAt DESC
        """
    )
    fun observeFavorites(): Flow<List<RecipeWithFavorite>>

    @Upsert
    suspend fun upsertAll(recipes: List<RecipeEntity>)

    @Query("SELECT COUNT(*) FROM recipes")
    suspend fun count(): Int
}

@Dao
interface FavoriteDao {

    @Upsert
    suspend fun upsert(favorite: FavoriteEntity)

    @Query("SELECT * FROM favorites WHERE pendingSync = 1")
    suspend fun pending(): List<FavoriteEntity>

    @Query("SELECT * FROM favorites")
    suspend fun all(): List<FavoriteEntity>

    @Query("SELECT COUNT(*) FROM favorites WHERE pendingSync = 1")
    fun observePendingCount(): Flow<Int>

    @Query("UPDATE favorites SET pendingSync = 0 WHERE recipeId = :recipeId AND updatedAt = :updatedAt")
    suspend fun markSynced(recipeId: String, updatedAt: Long)

    /**
     * Server wins for everything that has no unsynced local change: favourites added on another
     * device appear here, and ones removed elsewhere disappear.
     */
    @Transaction
    suspend fun mergeRemote(remoteIds: Set<String>, now: Long) {
        val local = all().associateBy { it.recipeId }
        for (id in remoteIds) {
            val existing = local[id]
            if (existing == null || (!existing.pendingSync && !existing.isFavorite)) {
                upsert(FavoriteEntity(id, isFavorite = true, pendingSync = false, updatedAt = now))
            }
        }
        for (fav in local.values) {
            if (!fav.pendingSync && fav.isFavorite && fav.recipeId !in remoteIds) {
                upsert(fav.copy(isFavorite = false, updatedAt = now))
            }
        }
    }
}

@Dao
interface ShoppingDao {

    @Query("SELECT * FROM shopping_items ORDER BY checked ASC, createdAt ASC")
    fun observeAll(): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM shopping_items WHERE `key` = :key AND unit IS :unit AND checked = 0 LIMIT 1")
    suspend fun findOpen(key: String, unit: MeasureUnit?): ShoppingItemEntity?

    @Insert
    suspend fun insert(item: ShoppingItemEntity): Long

    @Update
    suspend fun update(item: ShoppingItemEntity)

    @Delete
    suspend fun delete(item: ShoppingItemEntity)

    @Query("UPDATE shopping_items SET checked = :checked WHERE id = :id")
    suspend fun setChecked(id: Long, checked: Boolean)

    @Query("DELETE FROM shopping_items WHERE checked = 1")
    suspend fun clearChecked()

    @Query("DELETE FROM shopping_items")
    suspend fun clearAll()
}
