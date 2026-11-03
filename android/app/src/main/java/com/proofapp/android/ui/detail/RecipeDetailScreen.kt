package com.proofapp.android.ui.detail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.proofapp.android.domain.model.Recipe
import com.proofapp.android.domain.scaling.QuantityFormatter
import com.proofapp.android.domain.scaling.ScaledIngredient
import com.proofapp.android.ui.components.EmptyState
import com.proofapp.android.ui.components.FavoriteButton
import com.proofapp.android.ui.components.MetaLabel
import com.proofapp.android.ui.components.Pill
import com.proofapp.android.ui.components.RecipeArt
import com.proofapp.android.ui.components.SectionTitle
import com.proofapp.android.ui.components.formatClock
import com.proofapp.android.ui.components.formatMinutes
import com.proofapp.android.ui.theme.LocalProofExtras
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    onBack: () -> Unit,
    onStartCooking: (String) -> Unit,
    viewModel: RecipeDetailViewModel = viewModel(factory = RecipeDetailViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { viewModel.messages.collect { snackbar.showSnackbar(it) } }

    val recipe = state.recipe
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (recipe != null) FavoriteButton(recipe.isFavorite, viewModel::toggleFavorite)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        floatingActionButton = {
            if (recipe != null) {
                ExtendedFloatingActionButton(
                    onClick = { onStartCooking(recipe.id) },
                    icon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null) },
                    text = { Text("Start baking") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            recipe == null -> EmptyState("🥖", "Recipe not found", "It may have been removed. Pull to refresh on the recipe list.")
            else -> DetailContent(state, recipe, viewModel, Modifier.padding(top = padding.calculateTopPadding()))
        }
    }
}

@Composable
private fun DetailContent(state: DetailState, recipe: Recipe, vm: RecipeDetailViewModel, modifier: Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Hero(recipe) }
        item {
            ScalerCard(
                state = state,
                onIncrement = vm::increment,
                onDecrement = vm::decrement,
                onMultiplier = vm::applyMultiplier,
                onReset = vm::reset,
            )
        }
        item {
            SectionTitle("Ingredients", Modifier.padding(top = 8.dp)) {
                if (state.hasFlour) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Baker's %", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.width(8.dp))
                        Switch(checked = state.showBakersPercent, onCheckedChange = { vm.toggleBakersPercent() })
                    }
                }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    state.ingredients.forEachIndexed { index, item ->
                        IngredientRow(item, state.showBakersPercent)
                        if (index != state.ingredients.lastIndex) {
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
        item {
            OutlinedButton(onClick = vm::addToShoppingList, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Rounded.AddShoppingCart, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add to shopping list")
            }
        }
        item { SectionTitle("Method", Modifier.padding(top = 8.dp)) }
        itemsIndexed(recipe.steps) { index, step ->
            Row {
                Box(
                    Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f).padding(top = 4.dp)) {
                    Text(step.instruction, style = MaterialTheme.typography.bodyLarge)
                    step.timerSeconds?.let {
                        Spacer(Modifier.height(6.dp))
                        MetaLabel(Icons.Rounded.Timer, formatClock(it.toLong()))
                    }
                }
            }
        }
    }
}

@Composable
private fun Hero(recipe: Recipe) {
    Column {
        RecipeArt(recipe, Modifier.fillMaxWidth().height(200.dp), emojiSize = 96.dp, corner = 32.dp)
        Spacer(Modifier.height(20.dp))
        Text(
            recipe.category.label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp,
        )
        Text(recipe.title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(recipe.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MetaLabel(Icons.Rounded.Schedule, "Prep ${formatMinutes(recipe.prepMinutes)}")
            MetaLabel(Icons.Rounded.LocalFireDepartment, "Bake ${formatMinutes(recipe.cookMinutes)}")
            MetaLabel(Icons.Rounded.SignalCellularAlt, recipe.difficulty.label)
        }
    }
}

@Composable
private fun ScalerCard(
    state: DetailState,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onMultiplier: (Double) -> Unit,
    onReset: () -> Unit,
) {
    val recipe = state.recipe ?: return
    val palette = LocalProofExtras.current.palette(recipe.category)
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Makes", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Row(verticalAlignment = Alignment.Bottom) {
                        AnimatedContent(
                            targetState = state.servings,
                            transitionSpec = {
                                val up = targetState > initialState
                                (slideInVertically { if (up) it else -it } + fadeIn()) togetherWith
                                    (slideOutVertically { if (up) -it else it } + fadeOut())
                            },
                            label = "servings",
                        ) { servings ->
                            Text(
                                "$servings",
                                style = MaterialTheme.typography.displaySmall.merge(LocalProofExtras.current.numberStyle),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            recipe.yieldUnit,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                }
                FilledTonalIconButton(
                    onClick = onDecrement,
                    enabled = state.servings > 1,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = palette.start),
                    modifier = Modifier.size(48.dp),
                ) { Icon(Icons.Rounded.Remove, contentDescription = "Fewer") }
                Spacer(Modifier.width(10.dp))
                FilledTonalIconButton(
                    onClick = onIncrement,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = palette.start),
                    modifier = Modifier.size(48.dp),
                ) { Icon(Icons.Rounded.Add, contentDescription = "More") }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(0.5 to "½×", 1.0 to "1×", 2.0 to "2×", 3.0 to "3×").forEach { (m, label) ->
                    Pill(label, onClick = { onMultiplier(m) })
                }
                Spacer(Modifier.weight(1f))
                AnimatedVisibility(state.isScaled) {
                    TextButton(onClick = onReset) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("×${QuantityFormatter.decimal((state.factor * 100).roundToInt() / 100.0)}")
                    }
                }
            }
        }
    }
}

@Composable
private fun IngredientRow(item: ScaledIngredient, showPercent: Boolean) {
    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        AnimatedContent(targetState = item.display, label = "amount") { amount ->
            Text(
                amount,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.widthIn(min = 84.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.ingredient.name, style = MaterialTheme.typography.bodyLarge)
            item.ingredient.note?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        AnimatedVisibility(showPercent && item.bakersPercent != null) {
            Text(
                "${QuantityFormatter.decimal(((item.bakersPercent ?: 0.0) * 10).roundToInt() / 10.0)}%",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}
