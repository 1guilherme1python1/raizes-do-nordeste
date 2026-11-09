package br.com.raizesdonordeste.usuarios.api;

import java.time.Instant;
import java.util.UUID;

import br.com.raizesdonordeste.usuarios.domain.Perfil;
import br.com.raizesdonordeste.usuarios.domain.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;

public record UsuarioResponse(
        UUID id,
        String codigo,
        String nomeExibicao,
        @Schema(example = "cliente@example.com") String email,
        Perfil perfil,
        boolean ativo,
        Instant criadoEm,
        Instant atualizadoEm) {

    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(usuario.id(), usuario.codigo(), usuario.nomeExibicao(),
                usuario.emailNormalizado(), usuario.perfil(), usuario.ativo(), usuario.criadoEm(),
                usuario.atualizadoEm());
    }
}
