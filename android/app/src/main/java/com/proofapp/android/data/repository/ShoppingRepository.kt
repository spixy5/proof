package com.proofapp.android.data.repository

import com.proofapp.android.data.local.ShoppingDao
import com.proofapp.android.data.local.ShoppingItemEntity
import com.proofapp.android.domain.scaling.Quantity
import com.proofapp.android.domain.scaling.QuantityFormatter
import com.proofapp.android.domain.scaling.RecipeScaler
import com.proofapp.android.domain.scaling.ScaledIngredient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class ShoppingItem(
    val id: Long,
    val name: String,
    val amount: String?,
    val sources: String,
    val checked: Boolean,
    internal val entity: ShoppingItemEntity,
)

/** Local-only, so it works in the shop with no signal. */
class ShoppingRepository(private val dao: ShoppingDao) {

    val items: Flow<List<ShoppingItem>> = dao.observeAll().map { list ->
        list.map { e ->
            ShoppingItem(
                id = e.id,
                name = e.name,
                amount = if (e.amount != null && e.unit != null) {
                    QuantityFormatter.format(RecipeScaler.normalize(Quantity(e.amount, e.unit)))
                } else null,
                sources = e.sources,
                checked = e.checked,
                entity = e,
            )
        }
    }

    /**
     * Adds scaled ingredients. The same ingredient from two recipes is merged into one line
     * (200 g butter + 175 g butter = 375 g butter) as long as the units are compatible.
     */
    suspend fun addIngredients(recipeTitle: String, ingredients: List<ScaledIngredient>) {
        for (scaled in ingredients) {
            val base = scaled.quantity.inBaseUnit()
            // "Unsalted butter, melted" and "Unsalted butter" are the same thing to buy.
            val name = scaled.ingredient.name.substringBefore(",").trim()
            val key = keyOf(name)
            val existing = dao.findOpen(key, base.unit)
            if (existing != null) {
                dao.update(
                    existing.copy(
                        amount = (existing.amount ?: 0.0) + base.amount,
                        sources = mergeSources(existing.sources, recipeTitle),
                    )
                )
            } else {
                dao.insert(
                    ShoppingItemEntity(
                        name = name,
                        key = key,
                        amount = base.amount,
                        unit = base.unit,
                        sources = recipeTitle,
                    )
                )
            }
        }
    }

    suspend fun addCustom(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        dao.insert(ShoppingItemEntity(name = trimmed, key = keyOf(trimmed), amount = null, unit = null, sources = ""))
    }

    suspend fun setChecked(item: ShoppingItem, checked: Boolean) = dao.setChecked(item.id, checked)
    suspend fun delete(item: ShoppingItem) = dao.delete(item.entity)
    suspend fun clearChecked() = dao.clearChecked()
    suspend fun clearAll() = dao.clearAll()

    private fun keyOf(name: String) = name.lowercase().replace(Regex("[^a-z0-9 ]"), "").trim()

    private fun mergeSources(existing: String, added: String) =
        (existing.split(" · ").filter { it.isNotBlank() } + added).distinct().joinToString(" · ")
}
