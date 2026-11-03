package com.proofapp.api.recipe;

import com.proofapp.api.recipe.Recipe.Category;
import com.proofapp.api.recipe.Recipe.Difficulty;
import com.proofapp.api.recipe.Recipe.Ingredient;
import com.proofapp.api.recipe.Recipe.Step;
import java.util.List;

/** Wire format, shared with the Android client and the seed file. */
public record RecipeDto(
        String id,
        String title,
        String description,
        Category category,
        String emoji,
        int servings,
        String yieldUnit,
        int prepMinutes,
        int cookMinutes,
        Difficulty difficulty,
        List<Ingredient> ingredients,
        List<Step> steps) {

    public static RecipeDto from(Recipe r) {
        return new RecipeDto(
                r.getId(), r.getTitle(), r.getDescription(), r.getCategory(), r.getEmoji(), r.getServings(),
                r.getYieldUnit(), r.getPrepMinutes(), r.getCookMinutes(), r.getDifficulty(),
                List.copyOf(r.getIngredients()), List.copyOf(r.getSteps()));
    }

    public Recipe toEntity() {
        return new Recipe(id, title, description, category, emoji, servings, yieldUnit, prepMinutes, cookMinutes,
                difficulty, ingredients, steps);
    }
}
