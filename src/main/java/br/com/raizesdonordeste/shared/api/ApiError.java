package br.com.raizesdonordeste.shared.api;

import java.time.Instant;
import java.util.List;

public record ApiError(
        String error,
        String message,
        List<String> details,
        Instant timestamp,
        String path,
        String requestId) {
}
