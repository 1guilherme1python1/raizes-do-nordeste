package br.com.raizesdonordeste.usuarios.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistroClienteRequest(
        @NotBlank @Size(max = 150) @Schema(example = "Maria do Sertão") String nomeExibicao,
        @NotBlank @Size(max = 254)
        @Pattern(regexp = "^\\s*[^\\s@]+@[^\\s@]+\\.[^\\s@]+\\s*$", message = "deve ser um e-mail válido")
        @Schema(example = "maria@example.com") String email,
        @NotBlank @Size(min = 8, max = 72) @Schema(example = "senha-forte-123", format = "password")
        String senha) {
}
