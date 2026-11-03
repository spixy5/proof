package com.proofapp.android.domain.scaling

import com.proofapp.android.domain.model.Dimension
import com.proofapp.android.domain.model.Ingredient
import com.proofapp.android.domain.model.MeasureUnit
import kotlin.math.max
import kotlin.math.roundToInt

data class Quantity(val amount: Double, val unit: MeasureUnit) {
    /** The amount expressed in the dimension's base unit (g, ml, tsp or pieces). */
    val baseAmount: Double get() = amount * unit.toBase

    /** Same quantity expressed in g, ml, tsp or pieces, which makes amounts summable. */
    fun inBaseUnit(): Quantity = Quantity(baseAmount, baseUnitOf(unit))

    operator fun plus(other: Quantity): Quantity {
        require(unit.dimension == other.unit.dimension) {
            "Cannot add ${unit.dimension} to ${other.unit.dimension}"
        }
        return Quantity(baseAmount + other.baseAmount, baseUnitOf(unit)).let(RecipeScaler::normalize)
    }
}

data class ScaledIngredient(
    val ingredient: Ingredient,
    val quantity: Quantity,
    /** Weight relative to total flour, or null when the recipe has no flour or the unit is not weighable. */
    val bakersPercent: Double?,
) {
    val display: String get() = QuantityFormatter.format(quantity)
}

/**
 * Pure, side-effect free scaling engine. Every rule a baker cares about lives here so it can
 * be unit tested without Android:
 *
 *  - amounts scale linearly with servings,
 *  - results are normalised to the friendliest metric unit (1250 g becomes 1.25 kg, 0.4 l becomes 400 ml),
 *  - small amounts keep precision (yeast and salt matter), large amounts drop noise,
 *  - countable things (eggs) never drop below half a piece,
 *  - baker's percentages stay constant regardless of the batch size.
 */
object RecipeScaler {

    const val MIN_SERVINGS = 1
    const val MAX_SERVINGS = 99

    fun factor(baseServings: Int, targetServings: Int): Double {
        require(baseServings > 0) { "Base servings must be positive" }
        require(targetServings > 0) { "Target servings must be positive" }
        return targetServings.toDouble() / baseServings
    }

    fun scale(ingredient: Ingredient, factor: Double): Quantity {
        require(factor > 0) { "Scale factor must be positive" }
        val base = ingredient.amount * ingredient.unit.toBase * factor
        return normalize(Quantity(base, baseUnitOf(ingredient.unit)))
    }

    fun scaleAll(
        ingredients: List<Ingredient>,
        baseServings: Int,
        targetServings: Int,
    ): List<ScaledIngredient> {
        val f = factor(baseServings, targetServings)
        val percentages = bakersPercentages(ingredients)
        return ingredients.map { ScaledIngredient(it, scale(it, f), percentages[it]) }
    }

    /**
     * Baker's percentages express every ingredient as a share of the total flour weight.
     * Liquids are treated as 1 g per ml, which is the convention bakers use for water and milk.
     */
    fun bakersPercentages(ingredients: List<Ingredient>): Map<Ingredient, Double> {
        val flourGrams = ingredients.filter { it.flour }.sumOf { weighableGrams(it) ?: 0.0 }
        if (flourGrams <= 0.0) return emptyMap()
        return ingredients.mapNotNull { ing ->
            weighableGrams(ing)?.let { grams -> ing to (grams / flourGrams * 100.0) }
        }.toMap()
    }

    /** Picks the most readable unit and rounds to a precision that makes sense in a kitchen. */
    fun normalize(quantity: Quantity): Quantity {
        val base = quantity.baseAmount
        return when (quantity.unit.dimension) {
            Dimension.MASS -> when {
                base >= 1000 -> Quantity(roundTo(base / 1000, 100), MeasureUnit.KILOGRAM)
                else -> Quantity(roundSmall(base), MeasureUnit.GRAM)
            }

            Dimension.VOLUME -> when {
                base >= 1000 -> Quantity(roundTo(base / 1000, 100), MeasureUnit.LITRE)
                else -> Quantity(roundSmall(base), MeasureUnit.MILLILITRE)
            }

            // 1 tbsp = 3 tsp = 15 ml. Past a quarter cup (12 tsp) spoons get silly, so switch to ml.
            Dimension.SPOON -> when {
                base >= 12 -> Quantity(roundSmall(base * 5), MeasureUnit.MILLILITRE)
                base >= 3 -> Quantity(roundTo(base / 3, 4), MeasureUnit.TABLESPOON)
                else -> Quantity(max(roundTo(base, 4), 0.25), MeasureUnit.TEASPOON)
            }

            Dimension.COUNT -> when (quantity.unit) {
                MeasureUnit.PINCH -> Quantity(max(base.roundToInt(), 1).toDouble(), MeasureUnit.PINCH)
                else -> Quantity(max(roundTo(base, 2), 0.5), MeasureUnit.PIECE)
            }
        }
    }

    private fun weighableGrams(ingredient: Ingredient): Double? = when (ingredient.unit.dimension) {
        Dimension.MASS, Dimension.VOLUME -> ingredient.amount * ingredient.unit.toBase
        else -> null
    }

    /** Under 10 g or ml keep one decimal (half a gram matters for yeast); above, whole units. */
    private fun roundSmall(value: Double): Double =
        if (value < 10) max(roundTo(value, 2), 0.5) else value.roundToInt().toDouble()

    /** Rounds to the nearest 1/[stepsPerUnit]. Dividing (not multiplying by 0.01) keeps doubles exact. */
    private fun roundTo(value: Double, stepsPerUnit: Int): Double =
        (value * stepsPerUnit).roundToInt().toDouble() / stepsPerUnit
}

fun baseUnitOf(unit: MeasureUnit): MeasureUnit = when (unit.dimension) {
    Dimension.MASS -> MeasureUnit.GRAM
    Dimension.VOLUME -> MeasureUnit.MILLILITRE
    Dimension.SPOON -> MeasureUnit.TEASPOON
    Dimension.COUNT -> if (unit == MeasureUnit.PINCH) MeasureUnit.PINCH else MeasureUnit.PIECE
}
