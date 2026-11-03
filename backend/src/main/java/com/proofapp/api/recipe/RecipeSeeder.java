package com.proofapp.api.recipe;

import com.proofapp.api.config.ProofProperties;
import java.io.InputStream;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;

/** Loads the same recipes the Android app ships with, so a fresh database is never empty. */
@Component
public class RecipeSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RecipeSeeder.class);

    private final RecipeRepository recipes;
    private final ObjectMapper mapper;
    private final ProofProperties properties;

    public RecipeSeeder(RecipeRepository recipes, ObjectMapper mapper, ProofProperties properties) {
        this.recipes = recipes;
        this.mapper = mapper;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (!properties.seed().enabled() || recipes.count() > 0) return;
        try (InputStream in = new ClassPathResource("seed/recipes.json").getInputStream()) {
            // The seed file omits "flour": false and "timerSeconds" when they don't apply.
            List<RecipeDto> seed = mapper.readerFor(new TypeReference<List<RecipeDto>>() {})
                    .without(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                    .readValue(in);
            recipes.saveAll(seed.stream().map(RecipeDto::toEntity).toList());
            log.info("Seeded {} recipes", seed.size());
        }
    }
}
