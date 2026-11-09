package br.com.raizesdonordeste.pedidos.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import br.com.raizesdonordeste.pedidos.domain.ItemPedido;
import br.com.raizesdonordeste.shared.infrastructure.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(schema = "app", name = "item_pedido")
public class ItemPedidoEntity extends AuditableEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private PedidoEntity pedido;

    @Column(name = "unidade_id", nullable = false)
    private UUID unidadeId;

    @Column(name = "produto_unidade_id", nullable = false)
    private UUID produtoUnidadeId;

    @Column(name = "produto_id", nullable = false)
    private UUID produtoId;

    @Column(nullable = false)
    private int quantidade;

    @Column(name = "preco_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoUnitario;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    protected ItemPedidoEntity() {
    }

    public ItemPedidoEntity(UUID id, UUID unidadeId, UUID produtoUnidadeId, UUID produtoId, int quantidade,
            BigDecimal precoUnitario, BigDecimal subtotal, Instant criadoEm, Instant atualizadoEm) {
        super(criadoEm, atualizadoEm);
        this.id = id;
        this.unidadeId = unidadeId;
        this.produtoUnidadeId = produtoUnidadeId;
        this.produtoId = produtoId;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
        this.subtotal = subtotal;
    }

    static ItemPedidoEntity fromDomain(ItemPedido item, UUID unidadeId) {
        return new ItemPedidoEntity(item.id(), unidadeId, item.produtoUnidadeId(), item.produtoId(),
                item.quantidade(), item.precoUnitario(), item.subtotal(), item.criadoEm(), item.atualizadoEm());
    }

    ItemPedido toDomain() {
        return new ItemPedido(id, produtoId, produtoUnidadeId, quantidade, precoUnitario, subtotal,
                getCriadoEm(), getAtualizadoEm());
    }

    void setPedido(PedidoEntity pedido) {
        this.pedido = pedido;
    }

    public UUID getId() { return id; }
    public UUID getProdutoUnidadeId() { return produtoUnidadeId; }
    public UUID getProdutoId() { return produtoId; }
    public int getQuantidade() { return quantidade; }
    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public BigDecimal getSubtotal() { return subtotal; }
}
