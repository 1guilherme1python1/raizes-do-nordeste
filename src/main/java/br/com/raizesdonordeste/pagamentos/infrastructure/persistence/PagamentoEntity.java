package br.com.raizesdonordeste.pagamentos.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import br.com.raizesdonordeste.pagamentos.domain.Pagamento;
import br.com.raizesdonordeste.pagamentos.domain.StatusPagamento;
import br.com.raizesdonordeste.shared.infrastructure.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(schema = "app", name = "pagamento")
public class PagamentoEntity extends AuditableEntity {

    @Id
    private UUID id;

    @Column(name = "pedido_id", nullable = false)
    private UUID pedidoId;

    @Column(name = "numero_tentativa", nullable = false)
    private int numeroTentativa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPagamento status;

    @Column(name = "transacao_id", nullable = false, length = 100)
    private String transacaoId;

    @Column(name = "solicitado_em", nullable = false)
    private Instant solicitadoEm;

    @Column(name = "resultado_em")
    private Instant resultadoEm;

    @Column(name = "motivo_resultado", length = 250)
    private String motivoResultado;

    protected PagamentoEntity() {
    }

    public PagamentoEntity(UUID id, UUID pedidoId, int numeroTentativa, StatusPagamento status,
            String transacaoId, Instant solicitadoEm, Instant resultadoEm, String motivoResultado,
            Instant criadoEm, Instant atualizadoEm) {
        super(criadoEm, atualizadoEm);
        this.id = id;
        this.pedidoId = pedidoId;
        this.numeroTentativa = numeroTentativa;
        this.status = status;
        this.transacaoId = transacaoId;
        this.solicitadoEm = solicitadoEm;
        this.resultadoEm = resultadoEm;
        this.motivoResultado = motivoResultado;
    }

    public static PagamentoEntity fromDomain(Pagamento pagamento) {
        return new PagamentoEntity(pagamento.id(), pagamento.pedidoId(), pagamento.numeroTentativa(),
                pagamento.status(), pagamento.transacaoId(), pagamento.solicitadoEm(), pagamento.resultadoEm(),
                pagamento.motivoResultado(), pagamento.criadoEm(), pagamento.atualizadoEm());
    }

    public Pagamento toDomain() {
        return new Pagamento(id, pedidoId, numeroTentativa, status, transacaoId, solicitadoEm, resultadoEm,
                motivoResultado, getCriadoEm(), getAtualizadoEm());
    }

    public UUID getId() {
        return id;
    }
}
