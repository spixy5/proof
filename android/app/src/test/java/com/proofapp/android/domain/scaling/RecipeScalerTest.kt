package com.proofapp.android.domain.scaling

import com.proofapp.android.domain.model.Ingredient
import com.proofapp.android.domain.model.MeasureUnit
import com.proofapp.android.domain.model.MeasureUnit.GRAM
import com.proofapp.android.domain.model.MeasureUnit.KILOGRAM
import com.proofapp.android.domain.model.MeasureUnit.LITRE
import com.proofapp.android.domain.model.MeasureUnit.MILLILITRE
import com.proofapp.android.domain.model.MeasureUnit.PIECE
import com.proofapp.android.domain.model.MeasureUnit.PINCH
import com.proofapp.android.domain.model.MeasureUnit.TABLESPOON
import com.proofapp.android.domain.model.MeasureUnit.TEASPOON
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeScalerTest {

    private fun ing(amount: Double, unit: MeasureUnit, flour: Boolean = false, name: String = "x") =
        Ingredient(name = name, amount = amount, unit = unit, flour = flour)

    private fun scaled(amount: Double, unit: MeasureUnit, factor: Double) =
        RecipeScaler.scale(ing(amount, unit), factor)

    // ---- factor ----------------------------------------------------------------------------

    @Test
    fun `factor is target over base`() {
        assertEquals(2.0, RecipeScaler.factor(4, 8), 1e-9)
        assertEquals(0.5, RecipeScaler.factor(4, 2), 1e-9)
        assertEquals(1.0, RecipeScaler.factor(6, 6), 1e-9)
    }

    @Test
    fun `factor rejects zero or negative servings`() {
        assertThrows(IllegalArgumentException::class.java) { RecipeScaler.factor(0, 4) }
        assertThrows(IllegalArgumentException::class.java) { RecipeScaler.factor(4, 0) }
        assertThrows(IllegalArgumentException::class.java) { RecipeScaler.factor(-2, 4) }
    }

    // ---- mass ------------------------------------------------------------------------------

    @Test
    fun `grams scale linearly`() {
        assertEquals(Quantity(500.0, GRAM), scaled(250.0, GRAM, 2.0))
        assertEquals(Quantity(125.0, GRAM), scaled(250.0, GRAM, 0.5))
    }

    @Test
    fun `grams roll over into kilograms`() {
        assertEquals(Quantity(1.2, KILOGRAM), scaled(600.0, GRAM, 2.0))
        assertEquals(Quantity(1.0, KILOGRAM), scaled(500.0, GRAM, 2.0))
    }

    @Test
    fun `kilograms fall back to grams when scaled down`() {
        assertEquals(Quantity(500.0, GRAM), scaled(1.0, KILOGRAM, 0.5))
    }

    @Test
    fun `small weights keep half gram precision`() {
        // 7 g of yeast for a third of the batch: 2.33 g, rounded to 2.5 g, not 2 g.
        assertEquals(Quantity(2.5, GRAM), scaled(7.0, GRAM, 1.0 / 3))
    }

    @Test
    fun `large weights drop decimal noise`() {
        // 333.33 g becomes 333 g.
        assertEquals(Quantity(333.0, GRAM), scaled(1000.0, GRAM, 1.0 / 3))
    }

    @Test
    fun `tiny weights never round to zero`() {
        assertEquals(Quantity(0.5, GRAM), scaled(1.0, GRAM, 0.1))
    }

    // ---- volume ----------------------------------------------------------------------------

    @Test
    fun `millilitres roll over into litres`() {
        assertEquals(Quantity(1.5, LITRE), scaled(750.0, MILLILITRE, 2.0))
    }

    @Test
    fun `litres fall back to millilitres`() {
        assertEquals(Quantity(250.0, MILLILITRE), scaled(1.0, LITRE, 0.25))
    }

    // ---- spoons ----------------------------------------------------------------------------

    @Test
    fun `three teaspoons become a tablespoon`() {
        assertEquals(Quantity(1.0, TABLESPOON), scaled(1.0, TEASPOON, 3.0))
    }

    @Test
    fun `tablespoons fall back to teaspoons`() {
        assertEquals(Quantity(1.5, TEASPOON), scaled(1.0, TABLESPOON, 0.5))
    }

    @Test
    fun `spoons round to the nearest quarter`() {
        // 1 tsp * 1/3 = 0.333 tsp -> ¼ tsp
        assertEquals(Quantity(0.25, TEASPOON), scaled(1.0, TEASPOON, 1.0 / 3))
    }

    @Test
    fun `lots of spoons become millilitres`() {
        // 2 tbsp * 3 = 6 tbsp = 18 tsp = 90 ml
        assertEquals(Quantity(90.0, MILLILITRE), scaled(2.0, TABLESPOON, 3.0))
    }

    // ---- counts ----------------------------------------------------------------------------

    @Test
    fun `eggs round to the nearest half`() {
        // 3 eggs for 4, scaled to 6: 4.5 eggs
        assertEquals(Quantity(4.5, PIECE), scaled(3.0, PIECE, 1.5))
        // 3 eggs * 0.4 = 1.2 -> 1
        assertEquals(Quantity(1.0, PIECE), scaled(3.0, PIECE, 0.4))
    }

    @Test
    fun `never less than half an egg`() {
        assertEquals(Quantity(0.5, PIECE), scaled(1.0, PIECE, 0.1))
    }

    @Test
    fun `a pinch stays at least a pinch`() {
        assertEquals(Quantity(1.0, PINCH), scaled(1.0, PINCH, 0.25))
        assertEquals(Quantity(3.0, PINCH), scaled(1.0, PINCH, 3.0))
    }

    // ---- round trips -----------------------------------------------------------------------

    @Test
    fun `scaling up then down returns the original`() {
        val original = ing(450.0, GRAM)
        val up = RecipeScaler.scale(original, 4.0)
        val back = RecipeScaler.scale(Ingredient("x", up.amount, up.unit), 0.25)
        assertEquals(Quantity(450.0, GRAM), back)
    }

    @Test
    fun `scale all keeps order and uses servings`() {
        val ingredients = listOf(ing(500.0, GRAM, name = "flour"), ing(2.0, PIECE, name = "eggs"))
        val result = RecipeScaler.scaleAll(ingredients, baseServings = 4, targetServings = 8)
        assertEquals(listOf("flour", "eggs"), result.map { it.ingredient.name })
        assertEquals(Quantity(1.0, KILOGRAM), result[0].quantity)
        assertEquals(Quantity(4.0, PIECE), result[1].quantity)
    }

    // ---- baker's percentages ---------------------------------------------------------------

    @Test
    fun `bakers percentages are relative to total flour`() {
        val bread = ing(800.0, GRAM, flour = true, name = "bread flour")
        val wholemeal = ing(200.0, GRAM, flour = true, name = "wholemeal")
        val water = ing(750.0, MILLILITRE, name = "water")
        val salt = ing(20.0, GRAM, name = "salt")
        val p = RecipeScaler.bakersPercentages(listOf(bread, wholemeal, water, salt))
        assertEquals(80.0, p.getValue(bread), 1e-9)
        assertEquals(20.0, p.getValue(wholemeal), 1e-9)
        assertEquals(75.0, p.getValue(water), 1e-9)
        assertEquals(2.0, p.getValue(salt), 1e-9)
    }

    @Test
    fun `bakers percentages do not change when the batch is scaled`() {
        val flour = ing(500.0, GRAM, flour = true, name = "flour")
        val water = ing(350.0, MILLILITRE, name = "water")
        val small = RecipeScaler.scaleAll(listOf(flour, water), 2, 1)
        val big = RecipeScaler.scaleAll(listOf(flour, water), 2, 10)
        assertEquals(small[1].bakersPercent!!, big[1].bakersPercent!!, 1e-9)
        assertEquals(70.0, big[1].bakersPercent!!, 1e-9)
    }

    @Test
    fun `no flour means no percentages`() {
        val p = RecipeScaler.bakersPercentages(listOf(ing(100.0, GRAM), ing(2.0, PIECE)))
        assertTrue(p.isEmpty())
    }

    @Test
    fun `spoons and pieces get no percentage`() {
        val eggs = ing(2.0, PIECE)
        val vanilla = ing(1.0, TEASPOON)
        val result = RecipeScaler.scaleAll(listOf(ing(250.0, GRAM, flour = true), eggs, vanilla), 1, 1)
        assertNull(result[1].bakersPercent)
        assertNull(result[2].bakersPercent)
    }

    // ---- adding quantities (shopping list merge) -------------------------------------------

    @Test
    fun `quantities in the same dimension add up and normalise`() {
        assertEquals(Quantity(1.1, KILOGRAM), Quantity(600.0, GRAM) + Quantity(0.5, KILOGRAM))
        assertEquals(Quantity(1.0, TABLESPOON), Quantity(2.0, TEASPOON) + Quantity(1.0, TEASPOON))
    }

    @Test
    fun `quantities in different dimensions cannot be added`() {
        assertThrows(IllegalArgumentException::class.java) {
            Quantity(100.0, GRAM) + Quantity(100.0, MILLILITRE)
        }
    }
}
