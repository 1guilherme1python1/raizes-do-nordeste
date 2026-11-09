package br.com.raizesdonordeste.estoque.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import br.com.raizesdonordeste.estoque.domain.MovimentacaoEstoque;
import br.com.raizesdonordeste.estoque.domain.TipoMovimentacaoEstoque;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(schema = "app", name = "movimentacao_estoque")
public class MovimentacaoEstoqueEntity {

    @Id
    private UUID id;

    @Column(name = "estoque_id", nullable = false)
    private UUID estoqueId;

    @Column(name = "unidade_id", nullable = false)
    private UUID unidadeId;

    @Column(name = "responsavel_usuario_id")
    private UUID responsavelUsuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimentacaoEstoque tipo;

    @Column(nullable = false)
    private int quantidade;

    @Column(name = "quantidade_fisica_resultante", nullable = false)
    private int quantidadeFisicaResultante;

    @Column(name = "quantidade_reservada_resultante", nullable = false)
    private int quantidadeReservadaResultante;

    @Column(nullable = false, length = 250)
    private String motivo;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "requisicao_hash", length = 64)
    private String requisicaoHash;

    @Column(name = "pedido_id")
    private UUID pedidoId;

    @Column(name = "item_pedido_id")
    private UUID itemPedidoId;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected MovimentacaoEstoqueEntity() {
    }

    public MovimentacaoEstoqueEntity(UUID id, UUID estoqueId, UUID unidadeId, UUID responsavelUsuarioId,
            TipoMovimentacaoEstoque tipo, int quantidade, int quantidadeFisicaResultante,
            int quantidadeReservadaResultante, String motivo, String idempotencyKey, String requisicaoHash,
            Instant criadoEm) {
        this(id, estoqueId, unidadeId, responsavelUsuarioId, tipo, quantidade,
                quantidadeFisicaResultante, quantidadeReservadaResultante, motivo,
                idempotencyKey, requisicaoHash, null, null, criadoEm);
    }

    public MovimentacaoEstoqueEntity(UUID id, UUID estoqueId, UUID unidadeId, UUID responsavelUsuarioId,
            TipoMovimentacaoEstoque tipo, int quantidade, int quantidadeFisicaResultante,
            int quantidadeReservadaResultante, String motivo, String idempotencyKey, String requisicaoHash,
            UUID pedidoId, UUID itemPedidoId, Instant criadoEm) {
        this.id = id;
        this.estoqueId = estoqueId;
        this.unidadeId = unidadeId;
        this.responsavelUsuarioId = responsavelUsuarioId;
        this.tipo = tipo;
        this.quantidade = quantidade;
        this.quantidadeFisicaResultante = quantidadeFisicaResultante;
        this.quantidadeReservadaResultante = quantidadeReservadaResultante;
        this.motivo = motivo;
        this.idempotencyKey = idempotencyKey;
        this.requisicaoHash = requisicaoHash;
        this.pedidoId = pedidoId;
        this.itemPedidoId = itemPedidoId;
        this.criadoEm = criadoEm;
    }

    public static MovimentacaoEstoqueEntity fromDomain(MovimentacaoEstoque movimentacao) {
        return new MovimentacaoEstoqueEntity(movimentacao.id(), movimentacao.estoqueId(), movimentacao.unidadeId(),
                movimentacao.responsavelUsuarioId(), movimentacao.tipo(),
                movimentacao.quantidade(), movimentacao.quantidadeFisicaResultante(),
                movimentacao.quantidadeReservadaResultante(), movimentacao.motivo(), movimentacao.idempotencyKey(),
                movimentacao.requisicaoHash(), movimentacao.pedidoId(), movimentacao.itemPedidoId(), movimentacao.criadoEm());
    }

    public MovimentacaoEstoque toDomain() {
        return new MovimentacaoEstoque(id, estoqueId, unidadeId, responsavelUsuarioId, tipo, quantidade,
                quantidadeFisicaResultante, quantidadeReservadaResultante, motivo, idempotencyKey,
                requisicaoHash, pedidoId, itemPedidoId, criadoEm);
    }

    public UUID getId() {
        return id;
    }

    public UUID getEstoqueId() { return estoqueId; }
    public UUID getUnidadeId() { return unidadeId; }
    public UUID getResponsavelUsuarioId() { return responsavelUsuarioId; }
    public TipoMovimentacaoEstoque getTipo() { return tipo; }
    public int getQuantidade() { return quantidade; }
    public int getQuantidadeFisicaResultante() { return quantidadeFisicaResultante; }
    public int getQuantidadeReservadaResultante() { return quantidadeReservadaResultante; }
    public String getMotivo() { return motivo; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getRequisicaoHash() { return requisicaoHash; }
    public UUID getPedidoId() { return pedidoId; }
    public UUID getItemPedidoId() { return itemPedidoId; }
    public Instant getCriadoEm() { return criadoEm; }
}
