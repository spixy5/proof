package com.proofapp.api.recipe;

import com.proofapp.api.recipe.Recipe.Category;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecipeRepository extends JpaRepository<Recipe, String> {

    /** Matches the title, the description or any ingredient name. Both filters are optional. */
    @Query("""
            SELECT r FROM Recipe r
            WHERE (:category IS NULL OR r.category = :category)
              AND (:q IS NULL
                   OR LOWER(r.title) LIKE :q
                   OR LOWER(r.description) LIKE :q
                   OR EXISTS (SELECT 1 FROM Recipe r2 JOIN r2.ingredients i
                              WHERE r2 = r AND LOWER(i.name) LIKE :q))
            ORDER BY r.title
            """)
    List<Recipe> search(@Param("q") String likePattern, @Param("category") Category category);
}
