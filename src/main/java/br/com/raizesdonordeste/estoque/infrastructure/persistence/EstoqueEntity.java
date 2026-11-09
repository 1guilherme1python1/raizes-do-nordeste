package br.com.raizesdonordeste.estoque.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import br.com.raizesdonordeste.estoque.domain.Estoque;
import br.com.raizesdonordeste.shared.infrastructure.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(schema = "app", name = "estoque")
public class EstoqueEntity extends AuditableEntity {

    @Id
    private UUID id;

    @Column(name = "produto_unidade_id", nullable = false)
    private UUID produtoUnidadeId;

    @Column(name = "unidade_id", nullable = false)
    private UUID unidadeId;

    @Column(name = "quantidade_fisica", nullable = false)
    private int quantidadeFisica;

    @Column(name = "quantidade_reservada", nullable = false)
    private int quantidadeReservada;

    @Version
    @Column(name = "versao", nullable = false)
    private long versao;

    protected EstoqueEntity() {
    }

    public EstoqueEntity(UUID id, UUID produtoUnidadeId, UUID unidadeId, int quantidadeFisica,
            int quantidadeReservada, long versao, Instant criadoEm, Instant atualizadoEm) {
        super(criadoEm, atualizadoEm);
        this.id = id;
        this.produtoUnidadeId = produtoUnidadeId;
        this.unidadeId = unidadeId;
        this.quantidadeFisica = quantidadeFisica;
        this.quantidadeReservada = quantidadeReservada;
        this.versao = versao;
    }

    public static EstoqueEntity fromDomain(Estoque estoque) {
        return new EstoqueEntity(estoque.id(), estoque.produtoUnidadeId(), estoque.unidadeId(),
                estoque.quantidadeFisica(), estoque.quantidadeReservada(), estoque.versao(),
                estoque.criadoEm(), estoque.atualizadoEm());
    }

    public Estoque toDomain() {
        return new Estoque(id, produtoUnidadeId, unidadeId, quantidadeFisica, quantidadeReservada, versao,
                getCriadoEm(), getAtualizadoEm());
    }

    public UUID getId() {
        return id;
    }

    public UUID getProdutoUnidadeId() { return produtoUnidadeId; }
    public UUID getUnidadeId() { return unidadeId; }
    public int getQuantidadeFisica() { return quantidadeFisica; }
    public int getQuantidadeReservada() { return quantidadeReservada; }
    public long getVersao() { return versao; }

    public void movimentar(int novaQuantidadeFisica) {
        if (novaQuantidadeFisica < quantidadeReservada) {
            throw new IllegalArgumentException("Saldo físico não pode ser inferior ao reservado");
        }
        this.quantidadeFisica = novaQuantidadeFisica;
    }

    public void reservar(int quantidade) {
        if (quantidade <= 0 || quantidade > quantidadeFisica - quantidadeReservada) {
            throw new IllegalArgumentException("Saldo disponível insuficiente");
        }
        quantidadeReservada = Math.addExact(quantidadeReservada, quantidade);
    }

    public void liberar(int quantidade) {
        if (quantidade <= 0 || quantidade > quantidadeReservada) {
            throw new IllegalArgumentException("Reserva de estoque inválida");
        }
        quantidadeReservada -= quantidade;
    }
}
