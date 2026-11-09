package br.com.raizesdonordeste.catalogo.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import br.com.raizesdonordeste.catalogo.domain.Produto;
import br.com.raizesdonordeste.shared.infrastructure.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(schema = "app", name = "produto")
public class ProdutoEntity extends AuditableEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 50)
    private String sku;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(length = 500)
    private String descricao;

    @Column(nullable = false)
    private boolean ativo;

    protected ProdutoEntity() {
    }

    public ProdutoEntity(UUID id, String sku, String nome, String descricao, boolean ativo,
            Instant criadoEm, Instant atualizadoEm) {
        super(criadoEm, atualizadoEm);
        this.id = id;
        this.sku = sku;
        this.nome = nome;
        this.descricao = descricao;
        this.ativo = ativo;
    }

    public static ProdutoEntity fromDomain(Produto produto) {
        return new ProdutoEntity(produto.id(), produto.sku(), produto.nome(), produto.descricao(), produto.ativo(),
                produto.criadoEm(), produto.atualizadoEm());
    }

    public Produto toDomain() {
        return new Produto(id, sku, nome, descricao, ativo, getCriadoEm(), getAtualizadoEm());
    }

    public UUID getId() {
        return id;
    }

    public String getSku() { return sku; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public boolean isAtivo() { return ativo; }

    public void atualizar(String nome, String descricao, boolean ativo) {
        this.nome = nome;
        this.descricao = descricao;
        this.ativo = ativo;
    }
}
