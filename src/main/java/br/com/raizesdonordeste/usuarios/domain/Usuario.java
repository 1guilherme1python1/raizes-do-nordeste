package br.com.raizesdonordeste.usuarios.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Usuario(
        UUID id,
        String codigo,
        String nomeExibicao,
        String emailNormalizado,
        String senhaHash,
        Perfil perfil,
        boolean ativo,
        Instant criadoEm,
        Instant atualizadoEm) {

    public Usuario {
        Objects.requireNonNull(id);
        Objects.requireNonNull(codigo);
        Objects.requireNonNull(nomeExibicao);
        var semCredenciais = emailNormalizado == null && senhaHash == null && perfil == null;
        var credenciaisCompletas = emailNormalizado != null && senhaHash != null && perfil != null;
        if (!semCredenciais && !credenciaisCompletas) {
            throw new IllegalArgumentException("As credenciais do usuário devem estar completas");
        }
    }

    public Usuario(UUID id, String codigo, String nomeExibicao, boolean ativo,
            Instant criadoEm, Instant atualizadoEm) {
        this(id, codigo, nomeExibicao, null, null, null, ativo, criadoEm, atualizadoEm);
    }

    public boolean autenticavel() {
        return emailNormalizado != null && senhaHash != null && perfil != null;
    }
}
