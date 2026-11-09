package br.com.raizesdonordeste.catalogo.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import br.com.raizesdonordeste.catalogo.domain.ProdutoUnidade;
import br.com.raizesdonordeste.shared.infrastructure.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(schema = "app", name = "produto_unidade")
public class ProdutoUnidadeEntity extends AuditableEntity {

    @Id
    private UUID id;

    @Column(name = "produto_id", nullable = false)
    private UUID produtoId;

    @Column(name = "unidade_id", nullable = false)
    private UUID unidadeId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal preco;

    @Column(nullable = false)
    private boolean disponivel;

    protected ProdutoUnidadeEntity() {
    }

    public ProdutoUnidadeEntity(UUID id, UUID produtoId, UUID unidadeId, BigDecimal preco, boolean disponivel,
            Instant criadoEm, Instant atualizadoEm) {
        super(criadoEm, atualizadoEm);
        this.id = id;
        this.produtoId = produtoId;
        this.unidadeId = unidadeId;
        this.preco = preco;
        this.disponivel = disponivel;
    }

    public static ProdutoUnidadeEntity fromDomain(ProdutoUnidade produtoUnidade) {
        return new ProdutoUnidadeEntity(produtoUnidade.id(), produtoUnidade.produtoId(), produtoUnidade.unidadeId(),
                produtoUnidade.preco(), produtoUnidade.disponivel(), produtoUnidade.criadoEm(),
                produtoUnidade.atualizadoEm());
    }

    public ProdutoUnidade toDomain() {
        return new ProdutoUnidade(id, produtoId, unidadeId, preco, disponivel, getCriadoEm(), getAtualizadoEm());
    }

    public UUID getId() {
        return id;
    }

    public UUID getProdutoId() { return produtoId; }
    public UUID getUnidadeId() { return unidadeId; }
    public BigDecimal getPreco() { return preco; }
    public boolean isDisponivel() { return disponivel; }

    public void atualizar(BigDecimal preco, boolean disponivel) {
        this.preco = preco;
        this.disponivel = disponivel;
    }
}
