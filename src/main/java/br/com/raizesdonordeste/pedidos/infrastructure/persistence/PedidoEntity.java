package br.com.raizesdonordeste.pedidos.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import br.com.raizesdonordeste.pedidos.domain.CanalPedido;
import br.com.raizesdonordeste.pedidos.domain.Pedido;
import br.com.raizesdonordeste.pedidos.domain.StatusPedido;
import br.com.raizesdonordeste.shared.infrastructure.persistence.AuditableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import br.com.raizesdonordeste.pedidos.domain.FormaPagamento;

@Entity
@Table(schema = "app", name = "pedido")
public class PedidoEntity extends AuditableEntity {

    @Id
    private UUID id;

    @Column(name = "unidade_id", nullable = false)
    private UUID unidadeId;

    @Column(name = "cliente_id")
    private UUID clienteId;

    @Column(name = "criado_por_usuario_id")
    private UUID criadoPorUsuarioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal_pedido", nullable = false, length = 20)
    private CanalPedido canalPedido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusPedido status;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", length = 20)
    private FormaPagamento formaPagamento;

    @Column(name = "valor_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal;

    @Column(nullable = false, length = 3)
    private String moeda;

    @Column(name = "reserva_expira_em")
    private Instant reservaExpiraEm;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "requisicao_hash", length = 64)
    private String requisicaoHash;

    @Version
    @Column(nullable = false)
    private long versao;

    @OneToMany(mappedBy = "pedido", fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    private final List<ItemPedidoEntity> itens = new ArrayList<>();

    protected PedidoEntity() {
    }

    public PedidoEntity(UUID id, UUID unidadeId, UUID clienteId, UUID criadoPorUsuarioId,
            CanalPedido canalPedido, StatusPedido status, FormaPagamento formaPagamento,
            BigDecimal valorTotal, String moeda, Instant reservaExpiraEm, String idempotencyKey,
            String requisicaoHash, long versao, Instant criadoEm, Instant atualizadoEm) {
        super(criadoEm, atualizadoEm);
        this.id = id;
        this.unidadeId = unidadeId;
        this.clienteId = clienteId;
        this.criadoPorUsuarioId = criadoPorUsuarioId;
        this.canalPedido = canalPedido;
        this.status = status;
        this.formaPagamento = formaPagamento;
        this.valorTotal = valorTotal;
        this.moeda = moeda;
        this.reservaExpiraEm = reservaExpiraEm;
        this.idempotencyKey = idempotencyKey;
        this.requisicaoHash = requisicaoHash;
        this.versao = versao;
    }

    public static PedidoEntity fromDomain(Pedido pedido) {
        var entity = new PedidoEntity(pedido.id(), pedido.unidadeId(), pedido.clienteId(),
                pedido.criadoPorUsuarioId(), pedido.canalPedido(), pedido.status(), pedido.formaPagamento(),
                pedido.valorTotal(), pedido.moeda(), pedido.reservaExpiraEm(), pedido.idempotencyKey(), null,
                pedido.versao(), pedido.criadoEm(), pedido.atualizadoEm());
        pedido.itens().stream()
                .map(item -> ItemPedidoEntity.fromDomain(item, pedido.unidadeId()))
                .forEach(entity::addItem);
        return entity;
    }

    public Pedido toDomain() {
        var itensDomain = itens.stream().map(ItemPedidoEntity::toDomain).toList();
        return new Pedido(id, unidadeId, clienteId, criadoPorUsuarioId, canalPedido, status, formaPagamento,
                valorTotal, moeda, reservaExpiraEm, idempotencyKey, itensDomain, versao,
                getCriadoEm(), getAtualizadoEm());
    }

    private void addItem(ItemPedidoEntity item) {
        itens.add(item);
        item.setPedido(this);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUnidadeId() { return unidadeId; }
    public UUID getClienteId() { return clienteId; }
    public UUID getCriadoPorUsuarioId() { return criadoPorUsuarioId; }
    public CanalPedido getCanalPedido() { return canalPedido; }
    public StatusPedido getStatus() { return status; }
    public FormaPagamento getFormaPagamento() { return formaPagamento; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public String getMoeda() { return moeda; }
    public Instant getReservaExpiraEm() { return reservaExpiraEm; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getRequisicaoHash() { return requisicaoHash; }
    public long getVersao() { return versao; }
    public List<ItemPedidoEntity> getItens() { return List.copyOf(itens); }

    public void configurarRequisicao(String requisicaoHash) { this.requisicaoHash = requisicaoHash; }
    public void cancelar() { this.status = StatusPedido.CANCELADO; }
    public void expirar() { this.status = StatusPedido.EXPIRADO; }
}
