package br.com.raizesdonordeste.pedidos.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ItemPedido(
        UUID id,
        UUID produtoId,
        UUID produtoUnidadeId,
        int quantidade,
        BigDecimal precoUnitario,
        BigDecimal subtotal,
        Instant criadoEm,
        Instant atualizadoEm) {

    public ItemPedido {
        Objects.requireNonNull(id);
        Objects.requireNonNull(produtoId);
        Objects.requireNonNull(produtoUnidadeId);
        Objects.requireNonNull(precoUnitario);
        Objects.requireNonNull(subtotal);
        if (quantidade <= 0 || precoUnitario.signum() < 0 || subtotal.signum() < 0) {
            throw new IllegalArgumentException("Valores do item são inválidos");
        }
        var esperado = precoUnitario.multiply(BigDecimal.valueOf(quantidade)).setScale(2, RoundingMode.HALF_EVEN);
        if (subtotal.compareTo(esperado) != 0) {
            throw new IllegalArgumentException("Subtotal diferente de preço unitário x quantidade");
        }
    }
}
