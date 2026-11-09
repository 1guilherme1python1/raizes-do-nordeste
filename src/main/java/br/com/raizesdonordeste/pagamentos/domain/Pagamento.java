package br.com.raizesdonordeste.pagamentos.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Pagamento(
        UUID id,
        UUID pedidoId,
        int numeroTentativa,
        StatusPagamento status,
        String transacaoId,
        Instant solicitadoEm,
        Instant resultadoEm,
        String motivoResultado,
        Instant criadoEm,
        Instant atualizadoEm) {

    public Pagamento {
        Objects.requireNonNull(id);
        Objects.requireNonNull(pedidoId);
        Objects.requireNonNull(status);
        Objects.requireNonNull(transacaoId);
        Objects.requireNonNull(solicitadoEm);
        if (numeroTentativa <= 0) {
            throw new IllegalArgumentException("O número da tentativa deve ser positivo");
        }
        if (status == StatusPagamento.PENDENTE && (resultadoEm != null || motivoResultado != null)) {
            throw new IllegalArgumentException("Pagamento pendente não possui resultado");
        }
        if (status != StatusPagamento.PENDENTE && resultadoEm == null) {
            throw new IllegalArgumentException("Pagamento concluído deve possuir data de resultado");
        }
        if (status == StatusPagamento.RECUSADO && motivoResultado == null) {
            throw new IllegalArgumentException("Pagamento recusado deve possuir motivo");
        }
    }
}
