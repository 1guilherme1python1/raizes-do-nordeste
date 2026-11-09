package br.com.raizesdonordeste.auth.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
        @NotBlank @Pattern(regexp = "^\\s*[^\\s@]+@[^\\s@]+\\.[^\\s@]+\\s*$",
                message = "deve ser um e-mail válido") @Schema(example = "cliente@example.com") String email,
        @NotBlank @Schema(example = "senha-forte-123", format = "password") String senha) {
}
