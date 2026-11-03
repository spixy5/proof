package com.proofapp.api.favorite;

import com.proofapp.api.common.Errors.NotFoundException;
import com.proofapp.api.recipe.RecipeRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * PUT and DELETE are idempotent on purpose: the app replays changes queued while offline, and a
 * retry after a dropped connection must never fail or create duplicates.
 */
@RestController
@RequestMapping("/api/me/favorites")
@Tag(name = "Favorites", description = "The signed-in user's saved recipes, synced across devices")
@SecurityRequirement(name = "bearerAuth")
public class FavoriteController {

    private final FavoriteRepository favorites;
    private final RecipeRepository recipes;

    public FavoriteController(FavoriteRepository favorites, RecipeRepository recipes) {
        this.favorites = favorites;
        this.recipes = recipes;
    }

    @GetMapping
    @Operation(summary = "Ids of my favourite recipes, newest first")
    public List<String> list(@AuthenticationPrincipal Jwt jwt) {
        return favorites.findRecipeIds(userId(jwt));
    }

    @PutMapping("/{recipeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    @Operation(summary = "Add a favourite (idempotent)")
    public void add(@AuthenticationPrincipal Jwt jwt, @PathVariable String recipeId) {
        if (!recipes.existsById(recipeId)) throw new NotFoundException("No recipe with id '" + recipeId + "'");
        Favorite.Key key = new Favorite.Key(userId(jwt), recipeId);
        if (!favorites.existsById(key)) favorites.save(new Favorite(userId(jwt), recipeId));
    }

    @DeleteMapping("/{recipeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    @Operation(summary = "Remove a favourite (idempotent)")
    public void remove(@AuthenticationPrincipal Jwt jwt, @PathVariable String recipeId) {
        favorites.deleteByKey(userId(jwt), recipeId);
    }

    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
