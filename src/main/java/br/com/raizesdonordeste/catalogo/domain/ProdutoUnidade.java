package br.com.raizesdonordeste.catalogo.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ProdutoUnidade(
        UUID id,
        UUID produtoId,
        UUID unidadeId,
        BigDecimal preco,
        boolean disponivel,
        Instant criadoEm,
        Instant atualizadoEm) {

    public ProdutoUnidade {
        Objects.requireNonNull(id);
        Objects.requireNonNull(produtoId);
        Objects.requireNonNull(unidadeId);
        Objects.requireNonNull(preco);
        if (preco.signum() < 0) {
            throw new IllegalArgumentException("O preço não pode ser negativo");
        }
    }
}
