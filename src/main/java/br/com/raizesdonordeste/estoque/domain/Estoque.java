package br.com.raizesdonordeste.estoque.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Estoque(
        UUID id,
        UUID produtoUnidadeId,
        UUID unidadeId,
        int quantidadeFisica,
        int quantidadeReservada,
        long versao,
        Instant criadoEm,
        Instant atualizadoEm) {

    public Estoque {
        Objects.requireNonNull(id);
        Objects.requireNonNull(produtoUnidadeId);
        Objects.requireNonNull(unidadeId);
        if (quantidadeFisica < 0 || quantidadeReservada < 0 || quantidadeReservada > quantidadeFisica) {
            throw new IllegalArgumentException("Saldo de estoque inválido");
        }
    }

    public int quantidadeDisponivel() {
        return quantidadeFisica - quantidadeReservada;
    }

    public Estoque aplicar(TipoMovimentacaoEstoque tipo, int quantidade) {
        if (quantidade <= 0) throw new IllegalArgumentException("A quantidade deve ser positiva");
        if (tipo != TipoMovimentacaoEstoque.ENTRADA && tipo != TipoMovimentacaoEstoque.SAIDA) {
            throw new IllegalArgumentException("Somente ENTRADA e SAIDA são aceitas nesta operação");
        }
        if (tipo == TipoMovimentacaoEstoque.SAIDA && quantidade > quantidadeDisponivel()) {
            throw new IllegalArgumentException("Saldo disponível insuficiente");
        }
        var fisica = tipo == TipoMovimentacaoEstoque.ENTRADA
                ? Math.addExact(quantidadeFisica, quantidade) : quantidadeFisica - quantidade;
        return new Estoque(id, produtoUnidadeId, unidadeId, fisica, quantidadeReservada, versao,
                criadoEm, atualizadoEm);
    }
}
