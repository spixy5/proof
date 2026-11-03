package com.proofapp.api.favorite;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "favorites")
@IdClass(Favorite.Key.class)
public class Favorite {

    public static class Key implements Serializable {
        private Long userId;
        private String recipeId;

        protected Key() {}

        public Key(Long userId, String recipeId) {
            this.userId = userId;
            this.recipeId = recipeId;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key k && Objects.equals(userId, k.userId) && Objects.equals(recipeId, k.recipeId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, recipeId);
        }
    }

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "recipe_id")
    private String recipeId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Favorite() {}

    public Favorite(Long userId, String recipeId) {
        this.userId = userId;
        this.recipeId = recipeId;
        this.createdAt = Instant.now();
    }

    public Long getUserId() { return userId; }
    public String getRecipeId() { return recipeId; }
    public Instant getCreatedAt() { return createdAt; }
}
