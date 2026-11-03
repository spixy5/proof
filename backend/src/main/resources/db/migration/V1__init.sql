CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    display_name  VARCHAR(80)  NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE recipes (
    id            VARCHAR(80)  PRIMARY KEY,
    title         VARCHAR(160) NOT NULL,
    description   TEXT         NOT NULL,
    category      VARCHAR(20)  NOT NULL,
    emoji         VARCHAR(16)  NOT NULL,
    servings      INT          NOT NULL CHECK (servings > 0),
    yield_unit    VARCHAR(40)  NOT NULL,
    prep_minutes  INT          NOT NULL,
    cook_minutes  INT          NOT NULL,
    difficulty    VARCHAR(10)  NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_recipes_category ON recipes (category);

CREATE TABLE recipe_ingredients (
    recipe_id VARCHAR(80)  NOT NULL REFERENCES recipes (id) ON DELETE CASCADE,
    position  INT          NOT NULL,
    name      VARCHAR(160) NOT NULL,
    amount    DOUBLE PRECISION NOT NULL,
    unit      VARCHAR(20)  NOT NULL,
    flour     BOOLEAN      NOT NULL DEFAULT FALSE,
    note      VARCHAR(160),
    PRIMARY KEY (recipe_id, position)
);

CREATE TABLE recipe_steps (
    recipe_id     VARCHAR(80) NOT NULL REFERENCES recipes (id) ON DELETE CASCADE,
    position      INT         NOT NULL,
    instruction   TEXT        NOT NULL,
    timer_seconds INT,
    PRIMARY KEY (recipe_id, position)
);

CREATE TABLE favorites (
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    recipe_id  VARCHAR(80) NOT NULL REFERENCES recipes (id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (user_id, recipe_id)
);
