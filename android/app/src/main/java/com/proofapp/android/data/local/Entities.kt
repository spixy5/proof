package com.proofapp.android.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.proofapp.android.domain.model.Category
import com.proofapp.android.domain.model.Difficulty
import com.proofapp.android.domain.model.Ingredient
import com.proofapp.android.domain.model.MeasureUnit
import com.proofapp.android.domain.model.Recipe
import com.proofapp.android.domain.model.Step

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val category: Category,
    val emoji: String,
    val servings: Int,
    val yieldUnit: String,
    val prepMinutes: Int,
    val cookMinutes: Int,
    val difficulty: Difficulty,
    val ingredients: List<Ingredient>,
    val steps: List<Step>,
    /** Lower-cased ingredient names, so search can match "butter" or "yeast". */
    val searchText: String,
    val updatedAt: Long,
)

/**
 * Favourites live in their own table so a recipe refresh from the server never clobbers them.
 * [pendingSync] marks local changes that have not reached the server yet (offline, or signed out).
 */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val recipeId: String,
    val isFavorite: Boolean,
    val pendingSync: Boolean,
    val updatedAt: Long,
)

@Entity(tableName = "shopping_items", indices = [Index("key")])
data class ShoppingItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Normalised name used to merge the same ingredient coming from several recipes. */
    val key: String,
    /** Stored in the dimension's base unit (g, ml, tsp, piece) so amounts can be summed. */
    val amount: Double?,
    val unit: MeasureUnit?,
    val sources: String,
    val checked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

data class RecipeWithFavorite(
    @Embedded val recipe: RecipeEntity,
    val isFavorite: Boolean,
) {
    fun toDomain() = Recipe(
        id = recipe.id,
        title = recipe.title,
        description = recipe.description,
        category = recipe.category,
        emoji = recipe.emoji,
        servings = recipe.servings,
        yieldUnit = recipe.yieldUnit,
        prepMinutes = recipe.prepMinutes,
        cookMinutes = recipe.cookMinutes,
        difficulty = recipe.difficulty,
        ingredients = recipe.ingredients,
        steps = recipe.steps,
        isFavorite = isFavorite,
    )
}
