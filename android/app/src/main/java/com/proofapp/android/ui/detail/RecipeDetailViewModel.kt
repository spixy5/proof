package com.proofapp.android.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.proofapp.android.data.repository.RecipeRepository
import com.proofapp.android.data.repository.ShoppingRepository
import com.proofapp.android.domain.model.Recipe
import com.proofapp.android.domain.scaling.RecipeScaler
import com.proofapp.android.domain.scaling.ScaledIngredient
import com.proofapp.android.ui.container
import com.proofapp.android.ui.navigation.RecipeDetailRoute
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class DetailState(
    val recipe: Recipe? = null,
    val servings: Int = 0,
    val ingredients: List<ScaledIngredient> = emptyList(),
    val showBakersPercent: Boolean = false,
    val loading: Boolean = true,
) {
    val factor: Double get() = recipe?.let { servings.toDouble() / it.servings } ?: 1.0
    val isScaled: Boolean get() = recipe != null && servings != recipe.servings
    val hasFlour: Boolean get() = ingredients.any { it.bakersPercent != null }
}

class RecipeDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val recipes: RecipeRepository,
    private val shopping: ShoppingRepository,
) : ViewModel() {

    private val recipeId = savedStateHandle.toRoute<RecipeDetailRoute>().id

    /** null means "the recipe's own yield"; survives rotation through SavedStateHandle. */
    private val servingsOverride = savedStateHandle.getMutableStateFlow<Int?>("servings", null)
    private val showPercent = MutableStateFlow(false)

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    val state: StateFlow<DetailState> =
        combine(recipes.recipe(recipeId), servingsOverride, showPercent) { recipe, override, percent ->
            if (recipe == null) return@combine DetailState(loading = false)
            val servings = override ?: recipe.servings
            DetailState(
                recipe = recipe,
                servings = servings,
                ingredients = RecipeScaler.scaleAll(recipe.ingredients, recipe.servings, servings),
                showBakersPercent = percent,
                loading = false,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailState())

    fun setServings(value: Int) {
        servingsOverride.value = value.coerceIn(RecipeScaler.MIN_SERVINGS, RecipeScaler.MAX_SERVINGS)
    }

    fun increment() = setServings(state.value.servings + 1)
    fun decrement() = setServings(state.value.servings - 1)

    /** Quick presets: half, original, double, triple. */
    fun applyMultiplier(multiplier: Double) {
        val base = state.value.recipe?.servings ?: return
        setServings((base * multiplier).roundToInt().coerceAtLeast(1))
    }

    fun reset() { servingsOverride.value = null }

    fun toggleBakersPercent() { showPercent.value = !showPercent.value }

    fun toggleFavorite() {
        val recipe = state.value.recipe ?: return
        viewModelScope.launch { recipes.setFavorite(recipe.id, !recipe.isFavorite) }
    }

    fun addToShoppingList() {
        val s = state.value
        val recipe = s.recipe ?: return
        viewModelScope.launch {
            shopping.addIngredients(recipe.title, s.ingredients)
            _messages.send("${s.ingredients.size} ingredients added to your shopping list")
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                RecipeDetailViewModel(createSavedStateHandle(), container.recipeRepository, container.shoppingRepository)
            }
        }
    }
}
