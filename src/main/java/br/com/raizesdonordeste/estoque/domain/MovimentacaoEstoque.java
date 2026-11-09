package br.com.raizesdonordeste.estoque.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record MovimentacaoEstoque(
        UUID id,
        UUID estoqueId,
        UUID unidadeId,
        UUID responsavelUsuarioId,
        TipoMovimentacaoEstoque tipo,
        int quantidade,
        int quantidadeFisicaResultante,
        int quantidadeReservadaResultante,
        String motivo,
        String idempotencyKey,
        String requisicaoHash,
        UUID pedidoId,
        UUID itemPedidoId,
        Instant criadoEm) {

    public MovimentacaoEstoque(UUID id, UUID estoqueId, UUID unidadeId, UUID responsavelUsuarioId,
            TipoMovimentacaoEstoque tipo, int quantidade, int quantidadeFisicaResultante,
            int quantidadeReservadaResultante, String motivo, String idempotencyKey,
            String requisicaoHash, Instant criadoEm) {
        this(id, estoqueId, unidadeId, responsavelUsuarioId, tipo, quantidade,
                quantidadeFisicaResultante, quantidadeReservadaResultante, motivo,
                idempotencyKey, requisicaoHash, null, null, criadoEm);
    }

    public MovimentacaoEstoque {
        Objects.requireNonNull(id);
        Objects.requireNonNull(estoqueId);
        Objects.requireNonNull(unidadeId);
        Objects.requireNonNull(tipo);
        Objects.requireNonNull(motivo);
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade movimentada deve ser positiva");
        }
        if (quantidadeFisicaResultante < 0
                || quantidadeReservadaResultante < 0
                || quantidadeReservadaResultante > quantidadeFisicaResultante) {
            throw new IllegalArgumentException("Saldo resultante inválido");
        }
    }

}
