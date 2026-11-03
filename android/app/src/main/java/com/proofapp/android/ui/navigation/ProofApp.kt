package com.proofapp.android.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ShoppingBasket
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.proofapp.android.ui.cooking.CookingModeScreen
import com.proofapp.android.ui.detail.RecipeDetailScreen
import com.proofapp.android.ui.favorites.FavoritesScreen
import com.proofapp.android.ui.profile.ProfileScreen
import com.proofapp.android.ui.recipes.RecipeListScreen
import com.proofapp.android.ui.shopping.ShoppingListScreen
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

// Type-safe routes: arguments are checked at compile time instead of string templates.
@Serializable data object RecipesRoute
@Serializable data object FavoritesRoute
@Serializable data object ShoppingRoute
@Serializable data object ProfileRoute
@Serializable data class RecipeDetailRoute(val id: String)
@Serializable data class CookingRoute(val id: String)

private data class TopLevel(
    val route: Any,
    val routeClass: KClass<*>,
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
)

private val topLevel = listOf(
    TopLevel(RecipesRoute, RecipesRoute::class, "Recipes", Icons.Rounded.MenuBook, Icons.Outlined.MenuBook),
    TopLevel(FavoritesRoute, FavoritesRoute::class, "Favorites", Icons.Rounded.Favorite, Icons.Outlined.FavoriteBorder),
    TopLevel(ShoppingRoute, ShoppingRoute::class, "Shopping", Icons.Rounded.ShoppingBasket, Icons.Outlined.ShoppingBasket),
    TopLevel(ProfileRoute, ProfileRoute::class, "Profile", Icons.Rounded.Person, Icons.Outlined.Person),
)

@Composable
fun ProofApp() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val showBottomBar = topLevel.any { tl -> destination?.hierarchy?.any { it.hasRoute(tl.routeClass) } == true }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topLevel.forEach { item ->
                        val selected = destination?.hierarchy?.any { it.hasRoute(item.routeClass) } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(if (selected) item.selectedIcon else item.icon, contentDescription = null) },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        val openRecipe: (String) -> Unit = { navController.navigate(RecipeDetailRoute(it)) }
        NavHost(
            navController = navController,
            startDestination = RecipesRoute,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
            enterTransition = { fadeIn() + slideInHorizontally { it / 12 } },
            exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            popExitTransition = { fadeOut() + slideOutHorizontally { it / 12 } },
        ) {
            composable<RecipesRoute> { RecipeListScreen(onOpenRecipe = openRecipe) }
            composable<FavoritesRoute> {
                FavoritesScreen(
                    onOpenRecipe = openRecipe,
                    onSignIn = { navController.navigate(ProfileRoute) { launchSingleTop = true } },
                )
            }
            composable<ShoppingRoute> { ShoppingListScreen() }
            composable<ProfileRoute> { ProfileScreen() }
            composable<RecipeDetailRoute> {
                RecipeDetailScreen(
                    onBack = { navController.popBackStack() },
                    onStartCooking = { id -> navController.navigate(CookingRoute(id)) },
                )
            }
            composable<CookingRoute> { CookingModeScreen(onClose = { navController.popBackStack() }) }
        }
    }
}
