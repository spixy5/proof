package com.proofapp.android.domain.model

import kotlinx.serialization.Serializable

enum class Category(val label: String) {
    BREAD("Bread"),
    PASTRY("Pastry"),
    CAKES("Cakes"),
    COOKIES("Cookies"),
    DESSERTS("Desserts"),
    BREAKFAST("Breakfast"),
}

enum class Difficulty(val label: String) { EASY("Easy"), MEDIUM("Medium"), HARD("Hard") }

/** What a unit measures. Quantities are only ever converted within one dimension. */
enum class Dimension { MASS, VOLUME, SPOON, COUNT }

/**
 * Metric-first units. [toBase] converts one unit into the dimension's base unit:
 * grams for mass, millilitres for volume, teaspoons for spoons, and pieces for counts.
 */
enum class MeasureUnit(val symbol: String, val dimension: Dimension, val toBase: Double) {
    GRAM("g", Dimension.MASS, 1.0),
    KILOGRAM("kg", Dimension.MASS, 1000.0),
    MILLILITRE("ml", Dimension.VOLUME, 1.0),
    LITRE("l", Dimension.VOLUME, 1000.0),
    TEASPOON("tsp", Dimension.SPOON, 1.0),
    TABLESPOON("tbsp", Dimension.SPOON, 3.0),
    PIECE("", Dimension.COUNT, 1.0),
    PINCH("pinch", Dimension.COUNT, 1.0),
}

@Serializable
data class Ingredient(
    val name: String,
    val amount: Double,
    val unit: MeasureUnit,
    /** Flour ingredients form the 100% baseline for baker's percentages. */
    val flour: Boolean = false,
    val note: String? = null,
)

@Serializable
data class Step(
    val instruction: String,
    val timerSeconds: Int? = null,
)

data class Recipe(
    val id: String,
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
    val isFavorite: Boolean = false,
) {
    val totalMinutes: Int get() = prepMinutes + cookMinutes
}
