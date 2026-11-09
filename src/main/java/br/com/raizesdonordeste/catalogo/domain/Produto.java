package br.com.raizesdonordeste.catalogo.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Produto(
        UUID id,
        String sku,
        String nome,
        String descricao,
        boolean ativo,
        Instant criadoEm,
        Instant atualizadoEm) {

    public Produto {
        Objects.requireNonNull(id);
        Objects.requireNonNull(sku);
        Objects.requireNonNull(nome);
    }
}
