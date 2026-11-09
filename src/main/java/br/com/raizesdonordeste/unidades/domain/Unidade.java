package br.com.raizesdonordeste.unidades.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Unidade(
        UUID id,
        String codigo,
        String nome,
        boolean ativa,
        Instant criadoEm,
        Instant atualizadoEm) {

    public Unidade {
        Objects.requireNonNull(id);
        Objects.requireNonNull(codigo);
        Objects.requireNonNull(nome);
    }
}
