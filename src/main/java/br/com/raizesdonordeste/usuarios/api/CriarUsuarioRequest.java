package br.com.raizesdonordeste.usuarios.api;

import br.com.raizesdonordeste.usuarios.domain.Perfil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CriarUsuarioRequest(
        @NotBlank @Size(max = 150) @Schema(example = "Atendente da Unidade") String nomeExibicao,
        @NotBlank @Size(max = 254)
        @Pattern(regexp = "^\\s*[^\\s@]+@[^\\s@]+\\.[^\\s@]+\\s*$", message = "deve ser um e-mail válido")
        @Schema(example = "atendente@example.com") String email,
        @NotBlank @Size(min = 8, max = 72) @Schema(example = "senha-forte-123", format = "password")
        String senha,
        @NotNull @Schema(example = "ATENDENTE") Perfil perfil) {
}
