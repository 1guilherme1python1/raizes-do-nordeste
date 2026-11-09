package br.com.raizesdonordeste.pedidos.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record Pedido(
        UUID id,
        UUID unidadeId,
        UUID clienteId,
        UUID criadoPorUsuarioId,
        CanalPedido canalPedido,
        StatusPedido status,
        FormaPagamento formaPagamento,
        BigDecimal valorTotal,
        String moeda,
        Instant reservaExpiraEm,
        String idempotencyKey,
        List<ItemPedido> itens,
        long versao,
        Instant criadoEm,
        Instant atualizadoEm) {

    public Pedido {
        Objects.requireNonNull(id);
        Objects.requireNonNull(unidadeId);
        Objects.requireNonNull(canalPedido);
        Objects.requireNonNull(status);
        Objects.requireNonNull(valorTotal);
        Objects.requireNonNull(moeda);
        Objects.requireNonNull(itens);
        if (valorTotal.signum() < 0) {
            throw new IllegalArgumentException("O total do pedido não pode ser negativo");
        }
        if (!"BRL".equals(moeda)) {
            throw new IllegalArgumentException("A moeda do MVP deve ser BRL");
        }
        itens = List.copyOf(itens);
    }

    public Pedido(UUID id, UUID unidadeId, UUID clienteId, CanalPedido canalPedido, StatusPedido status,
            BigDecimal valorTotal, String moeda, List<ItemPedido> itens, Instant criadoEm, Instant atualizadoEm) {
        this(id, unidadeId, clienteId, null, canalPedido, status, null, valorTotal, moeda, null, null,
                itens, 0, criadoEm, atualizadoEm);
    }
}
