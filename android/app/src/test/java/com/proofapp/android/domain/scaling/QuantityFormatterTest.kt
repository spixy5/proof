package com.proofapp.android.domain.scaling

import com.proofapp.android.domain.model.MeasureUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class QuantityFormatterTest {

    private fun fmt(amount: Double, unit: MeasureUnit) = QuantityFormatter.format(Quantity(amount, unit))

    @Test
    fun `whole numbers have no decimals`() {
        assertEquals("500 g", fmt(500.0, MeasureUnit.GRAM))
        assertEquals("2", fmt(2.0, MeasureUnit.PIECE))
    }

    @Test
    fun `decimals are trimmed`() {
        assertEquals("1.25 kg", fmt(1.25, MeasureUnit.KILOGRAM))
        assertEquals("1.5 l", fmt(1.50, MeasureUnit.LITRE))
        assertEquals("2.5 g", fmt(2.5, MeasureUnit.GRAM))
    }

    @Test
    fun `spoons and pieces use fraction glyphs`() {
        assertEquals("1½ tbsp", fmt(1.5, MeasureUnit.TABLESPOON))
        assertEquals("¼ tsp", fmt(0.25, MeasureUnit.TEASPOON))
        assertEquals("2¾ tsp", fmt(2.75, MeasureUnit.TEASPOON))
        assertEquals("½", fmt(0.5, MeasureUnit.PIECE))
    }

    @Test
    fun `pinches read naturally`() {
        assertEquals("a pinch", fmt(1.0, MeasureUnit.PINCH))
        assertEquals("2 pinches", fmt(2.0, MeasureUnit.PINCH))
    }
}
