package br.com.raizesdonordeste.shared.infrastructure.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.security.jwt")
public record JwtProperties(
        String secret,
        String issuer,
        String audience,
        Duration accessTokenValidity) {
}
