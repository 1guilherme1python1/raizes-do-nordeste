package br.com.raizesdonordeste.auth.application;

import java.time.Instant;

public record AccessToken(String value, Instant expiresAt) {
}
