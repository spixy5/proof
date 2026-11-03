package com.proofapp.android.data.remote

import com.proofapp.android.domain.model.Ingredient
import com.proofapp.android.domain.model.Step
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/** Mirrors the Spring Boot API. See backend/ and http://localhost:8080/swagger-ui.html. */
interface ProofApi {

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @GET("api/recipes")
    suspend fun recipes(): List<RecipeDto>

    @GET("api/recipes/{id}")
    suspend fun recipe(@Path("id") id: String): RecipeDto

    @GET("api/me/favorites")
    suspend fun favorites(): List<String>

    @PUT("api/me/favorites/{recipeId}")
    suspend fun addFavorite(@Path("recipeId") recipeId: String)

    @DELETE("api/me/favorites/{recipeId}")
    suspend fun removeFavorite(@Path("recipeId") recipeId: String)
}

@Serializable
data class RecipeDto(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val emoji: String,
    val servings: Int,
    val yieldUnit: String,
    val prepMinutes: Int,
    val cookMinutes: Int,
    val difficulty: String,
    val ingredients: List<Ingredient>,
    val steps: List<Step>,
)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RegisterRequest(val email: String, val password: String, val displayName: String)

@Serializable
data class UserDto(val id: Long, val email: String, val displayName: String)

@Serializable
data class AuthResponse(val token: String, val expiresAt: String, val user: UserDto)

@Serializable
data class ApiError(val status: Int = 0, val message: String = "")
