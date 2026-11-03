package com.proofapp.api.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "proof")
public record ProofProperties(Jwt jwt, Seed seed) {

    public record Jwt(String secret, Duration ttl) {}

    public record Seed(boolean enabled) {}
}
