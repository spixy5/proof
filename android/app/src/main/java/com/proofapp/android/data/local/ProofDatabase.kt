package com.proofapp.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.proofapp.android.domain.model.Ingredient
import com.proofapp.android.domain.model.Step
import kotlinx.serialization.json.Json

@Database(
    entities = [RecipeEntity::class, FavoriteEntity::class, ShoppingItemEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class ProofDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun shoppingDao(): ShoppingDao
}

/** Ingredients and steps are always read with their recipe, so they are stored as JSON columns. */
class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun ingredientsToJson(value: List<Ingredient>): String = json.encodeToString(value)

    @TypeConverter
    fun ingredientsFromJson(value: String): List<Ingredient> = json.decodeFromString(value)

    @TypeConverter
    fun stepsToJson(value: List<Step>): String = json.encodeToString(value)

    @TypeConverter
    fun stepsFromJson(value: String): List<Step> = json.decodeFromString(value)
}
