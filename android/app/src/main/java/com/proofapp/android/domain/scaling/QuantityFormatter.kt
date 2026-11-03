package com.proofapp.android.domain.scaling

import com.proofapp.android.domain.model.MeasureUnit
import java.math.BigDecimal
import kotlin.math.abs
import kotlin.math.floor

object QuantityFormatter {

    private val fractions = mapOf(0.25 to "¼", 0.5 to "½", 0.75 to "¾")

    fun format(quantity: Quantity): String {
        val number = when (quantity.unit) {
            MeasureUnit.TEASPOON, MeasureUnit.TABLESPOON, MeasureUnit.PIECE -> fraction(quantity.amount)
            else -> decimal(quantity.amount)
        }
        return when (quantity.unit) {
            MeasureUnit.PIECE -> number
            MeasureUnit.PINCH -> if (quantity.amount > 1) "$number pinches" else "a pinch"
            else -> "$number ${quantity.unit.symbol}"
        }
    }

    /** 1.0 -> "1", 1.25 -> "1.25", 0.50 -> "0.5". Always uses a dot, independent of locale. */
    fun decimal(value: Double): String =
        BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()

    /** 1.5 -> "1½", 0.25 -> "¼", 2.0 -> "2". Falls back to decimals for anything else. */
    fun fraction(value: Double): String {
        val whole = floor(value)
        val rest = value - whole
        if (abs(rest) < 1e-9) return decimal(whole)
        val glyph = fractions.entries.firstOrNull { abs(it.key - rest) < 1e-9 }?.value
            ?: return decimal(value)
        return if (whole == 0.0) glyph else "${decimal(whole)}$glyph"
    }
}
