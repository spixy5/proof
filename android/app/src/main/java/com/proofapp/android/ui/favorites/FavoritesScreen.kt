package com.proofapp.android.ui.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.proofapp.android.data.repository.AuthRepository
import com.proofapp.android.data.repository.RecipeRepository
import com.proofapp.android.domain.model.Recipe
import com.proofapp.android.ui.components.EmptyState
import com.proofapp.android.ui.components.RecipeCard
import com.proofapp.android.ui.container
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FavoritesState(
    val recipes: List<Recipe> = emptyList(),
    val signedIn: Boolean = false,
    val pendingChanges: Int = 0,
    val loading: Boolean = true,
)

class FavoritesViewModel(private val recipes: RecipeRepository, auth: AuthRepository) : ViewModel() {
    val state: StateFlow<FavoritesState> =
        combine(recipes.favorites(), auth.session, recipes.pendingFavoriteChanges) { list, session, pending ->
            FavoritesState(list, session != null, pending, loading = false)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FavoritesState())

    fun remove(recipe: Recipe) {
        viewModelScope.launch { recipes.setFavorite(recipe.id, false) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { FavoritesViewModel(container.recipeRepository, container.authRepository) }
        }
    }
}

@Composable
fun FavoritesScreen(
    onOpenRecipe: (String) -> Unit,
    onSignIn: () -> Unit,
    viewModel: FavoritesViewModel = viewModel(factory = FavoritesViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text(
            "Favorites",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
        )
        SyncBanner(state, onSignIn)
        if (!state.loading && state.recipes.isEmpty()) {
            EmptyState(
                emoji = "💛",
                title = "No favorites yet",
                body = "Tap the heart on any recipe to keep it here. Works offline, and syncs when you sign in.",
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.recipes, key = { it.id }) { recipe ->
                    RecipeCard(
                        recipe,
                        onClick = { onOpenRecipe(recipe.id) },
                        onToggleFavorite = { viewModel.remove(recipe) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncBanner(state: FavoritesState, onSignIn: () -> Unit) {
    val (icon, text) = when {
        !state.signedIn -> Icons.Rounded.CloudOff to "Saved on this device only"
        state.pendingChanges > 0 -> Icons.Rounded.CloudSync to "${state.pendingChanges} change(s) waiting to sync"
        else -> return
    }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (!state.signedIn) TextButton(onClick = onSignIn) { Text("Sign in to sync") }
        }
    }
}
