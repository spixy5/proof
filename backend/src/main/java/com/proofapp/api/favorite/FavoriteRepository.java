package com.proofapp.api.favorite;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoriteRepository extends JpaRepository<Favorite, Favorite.Key> {

    @Query("SELECT f.recipeId FROM Favorite f WHERE f.userId = :userId ORDER BY f.createdAt DESC")
    List<String> findRecipeIds(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM Favorite f WHERE f.userId = :userId AND f.recipeId = :recipeId")
    int deleteByKey(@Param("userId") Long userId, @Param("recipeId") String recipeId);
}
