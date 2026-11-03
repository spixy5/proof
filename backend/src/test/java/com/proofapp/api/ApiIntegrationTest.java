package com.proofapp.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {

    @Autowired
    MockMvc mvc;

    private String register(String email) throws Exception {
        String body = """
                {"email": "%s", "password": "sourdough123", "displayName": "Test Baker"}
                """.formatted(email);
        String json = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }

    private static String uniqueEmail() {
        return "baker-" + UUID.randomUUID() + "@proof.dev";
    }

    @Nested
    class Recipes {

        @Test
        void listsSeededRecipesWithoutAuth() throws Exception {
            mvc.perform(get("/api/recipes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(10)))
                    .andExpect(jsonPath("$[0].ingredients[0].unit", notNullValue()));
        }

        @Test
        void filtersByCategory() throws Exception {
            mvc.perform(get("/api/recipes").param("category", "BREAD"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[*].category", everyItem(is("BREAD"))));
        }

        @Test
        void searchesIngredientNames() throws Exception {
            mvc.perform(get("/api/recipes").param("q", "tipo 00"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].id", contains("neapolitan-pizza-dough")));
        }

        @Test
        void returnsOneRecipeWithOrderedSteps() throws Exception {
            mvc.perform(get("/api/recipes/country-sourdough"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title", is("Country Sourdough")))
                    .andExpect(jsonPath("$.steps", hasSize(9)))
                    .andExpect(jsonPath("$.steps[0].timerSeconds", is(2700)))
                    .andExpect(jsonPath("$.ingredients[0].flour", is(true)));
        }

        @Test
        void unknownRecipeIs404WithErrorBody() throws Exception {
            mvc.perform(get("/api/recipes/does-not-exist"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", notNullValue()));
        }
    }

    @Nested
    class Auth {

        @Test
        void registerThenLogin() throws Exception {
            String email = uniqueEmail();
            register(email);
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + email.toUpperCase() + "\",\"password\":\"sourdough123\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token", notNullValue()))
                    .andExpect(jsonPath("$.user.displayName", is("Test Baker")));
        }

        @Test
        void duplicateEmailIsConflict() throws Exception {
            String email = uniqueEmail();
            register(email);
            mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + email + "\",\"password\":\"sourdough123\",\"displayName\":\"Again\"}"))
                    .andExpect(status().isConflict());
        }

        @Test
        void wrongPasswordIsUnauthorized() throws Exception {
            String email = uniqueEmail();
            register(email);
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + email + "\",\"password\":\"croissant\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message", is("Wrong email or password")));
        }

        @Test
        void shortPasswordIsRejected() throws Exception {
            mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + uniqueEmail() + "\",\"password\":\"short\",\"displayName\":\"X\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void meReturnsTheTokenOwner() throws Exception {
            String email = uniqueEmail();
            String token = register(email);
            mvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email", is(email)));
        }
    }

    @Nested
    class Favorites {

        @Test
        void requireAuthentication() throws Exception {
            mvc.perform(get("/api/me/favorites")).andExpect(status().isUnauthorized());
            mvc.perform(get("/api/me/favorites").header("Authorization", "Bearer not-a-jwt"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void addListAndRemoveIdempotently() throws Exception {
            String auth = "Bearer " + register(uniqueEmail());

            mvc.perform(get("/api/me/favorites").header("Authorization", auth))
                    .andExpect(jsonPath("$", empty()));

            // Offline replays can send the same PUT twice; it must stay a single favourite.
            mvc.perform(put("/api/me/favorites/fudgy-brownies").header("Authorization", auth))
                    .andExpect(status().isNoContent());
            mvc.perform(put("/api/me/favorites/fudgy-brownies").header("Authorization", auth))
                    .andExpect(status().isNoContent());

            mvc.perform(get("/api/me/favorites").header("Authorization", auth))
                    .andExpect(jsonPath("$", contains("fudgy-brownies")));

            mvc.perform(delete("/api/me/favorites/fudgy-brownies").header("Authorization", auth))
                    .andExpect(status().isNoContent());
            mvc.perform(delete("/api/me/favorites/fudgy-brownies").header("Authorization", auth))
                    .andExpect(status().isNoContent());

            mvc.perform(get("/api/me/favorites").header("Authorization", auth))
                    .andExpect(jsonPath("$", empty()));
        }

        @Test
        void favouritesArePerUser() throws Exception {
            String alice = "Bearer " + register(uniqueEmail());
            String bob = "Bearer " + register(uniqueEmail());
            mvc.perform(put("/api/me/favorites/banana-bread").header("Authorization", alice))
                    .andExpect(status().isNoContent());
            mvc.perform(get("/api/me/favorites").header("Authorization", bob))
                    .andExpect(jsonPath("$", empty()));
        }

        @Test
        void unknownRecipeIs404() throws Exception {
            String auth = "Bearer " + register(uniqueEmail());
            mvc.perform(put("/api/me/favorites/nope").header("Authorization", auth))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void openApiDocsArePublished() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title", is("Proof API")));
    }
}
