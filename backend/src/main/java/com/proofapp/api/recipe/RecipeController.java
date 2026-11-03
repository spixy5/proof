package com.proofapp.api.recipe;

import com.proofapp.api.common.Errors.NotFoundException;
import com.proofapp.api.recipe.Recipe.Category;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Locale;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recipes")
@Tag(name = "Recipes", description = "Public recipe catalogue")
@Transactional(readOnly = true)
public class RecipeController {

    private final RecipeRepository recipes;

    public RecipeController(RecipeRepository recipes) {
        this.recipes = recipes;
    }

    @GetMapping
    @Operation(summary = "List or search recipes",
            description = "Searches titles, descriptions and ingredient names. Both filters are optional.")
    public List<RecipeDto> list(
            @Parameter(description = "Free text, e.g. 'butter'") @RequestParam(required = false) String q,
            @RequestParam(required = false) Category category) {
        String pattern = StringUtils.hasText(q) ? "%" + escapeLike(q.trim().toLowerCase(Locale.ROOT)) + "%" : null;
        return recipes.search(pattern, category).stream().map(RecipeDto::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one recipe with its ingredients and steps")
    public RecipeDto get(@PathVariable String id) {
        return recipes.findById(id).map(RecipeDto::from)
                .orElseThrow(() -> new NotFoundException("No recipe with id '" + id + "'"));
    }

    private static String escapeLike(String s) {
        return s.replace("%", "").replace("_", "");
    }
}
