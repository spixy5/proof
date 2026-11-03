package com.proofapp.api.recipe;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recipes")
public class Recipe {

    /** Enum names match the Android app's, so JSON travels between them unchanged. */
    public enum Category { BREAD, PASTRY, CAKES, COOKIES, DESSERTS, BREAKFAST }

    public enum Difficulty { EASY, MEDIUM, HARD }

    public enum MeasureUnit { GRAM, KILOGRAM, MILLILITRE, LITRE, TEASPOON, TABLESPOON, PIECE, PINCH }

    @Embeddable
    public record Ingredient(
            @Column(nullable = false) String name,
            @Column(nullable = false) double amount,
            @Enumerated(EnumType.STRING) @Column(nullable = false) MeasureUnit unit,
            @Column(nullable = false) boolean flour,
            String note) {}

    @Embeddable
    public record Step(
            @Column(nullable = false, columnDefinition = "TEXT") String instruction,
            @Column(name = "timer_seconds") Integer timerSeconds) {}

    /** A readable slug such as "country-sourdough". Stable across environments and devices. */
    @Id
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(nullable = false)
    private String emoji;

    @Column(nullable = false)
    private int servings;

    @Column(name = "yield_unit", nullable = false)
    private String yieldUnit;

    @Column(name = "prep_minutes", nullable = false)
    private int prepMinutes;

    @Column(name = "cook_minutes", nullable = false)
    private int cookMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    @ElementCollection
    @CollectionTable(name = "recipe_ingredients", joinColumns = @JoinColumn(name = "recipe_id"))
    @OrderColumn(name = "position")
    private List<Ingredient> ingredients = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "recipe_steps", joinColumns = @JoinColumn(name = "recipe_id"))
    @OrderColumn(name = "position")
    private List<Step> steps = new ArrayList<>();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Recipe() {}

    public Recipe(String id, String title, String description, Category category, String emoji, int servings,
                  String yieldUnit, int prepMinutes, int cookMinutes, Difficulty difficulty,
                  List<Ingredient> ingredients, List<Step> steps) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.emoji = emoji;
        this.servings = servings;
        this.yieldUnit = yieldUnit;
        this.prepMinutes = prepMinutes;
        this.cookMinutes = cookMinutes;
        this.difficulty = difficulty;
        this.ingredients = new ArrayList<>(ingredients);
        this.steps = new ArrayList<>(steps);
        this.updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public String getEmoji() { return emoji; }
    public int getServings() { return servings; }
    public String getYieldUnit() { return yieldUnit; }
    public int getPrepMinutes() { return prepMinutes; }
    public int getCookMinutes() { return cookMinutes; }
    public Difficulty getDifficulty() { return difficulty; }
    public List<Ingredient> getIngredients() { return ingredients; }
    public List<Step> getSteps() { return steps; }
    public Instant getUpdatedAt() { return updatedAt; }
}
