package com.proofapp.android.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.proofapp.android.data.repository.AuthRepository
import com.proofapp.android.data.repository.RecipeRepository
import com.proofapp.android.domain.model.Category
import com.proofapp.android.domain.model.Recipe
import com.proofapp.android.ui.container
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RecipeListState(
    val query: String = "",
    val category: Category? = null,
    val recipes: List<Recipe> = emptyList(),
    val loading: Boolean = true,
)

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RecipeListViewModel(
    private val recipes: RecipeRepository,
    auth: AuthRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val category = MutableStateFlow<Category?>(null)

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    val userName: StateFlow<String?> = auth.session
        .map { it?.displayName?.substringBefore(" ")?.takeIf(String::isNotBlank) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val results = combine(query.debounce(200), category) { q, c -> q to c }
        .flatMapLatest { (q, c) -> recipes.recipes(q, c) }

    val state: StateFlow<RecipeListState> = combine(query, category, results) { q, c, list ->
        RecipeListState(query = q, category = c, recipes = list, loading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipeListState())

    fun onQueryChange(value: String) { query.value = value }

    fun onCategorySelected(value: Category?) {
        category.value = if (category.value == value) null else value
    }

    fun toggleFavorite(recipe: Recipe) {
        viewModelScope.launch { recipes.setFavorite(recipe.id, !recipe.isFavorite) }
    }

    fun refresh() {
        viewModelScope.launch {
            _refreshing.value = true
            recipes.refresh().onFailure {
                _messages.send("You're offline, showing recipes saved on this device")
            }
            _refreshing.value = false
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { RecipeListViewModel(container.recipeRepository, container.authRepository) }
        }
    }
}
