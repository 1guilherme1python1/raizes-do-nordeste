package br.com.raizesdonordeste.unidades.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import br.com.raizesdonordeste.shared.infrastructure.persistence.AuditableEntity;
import br.com.raizesdonordeste.unidades.domain.Unidade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(schema = "app", name = "unidade")
public class UnidadeEntity extends AuditableEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 50)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false)
    private boolean ativa;

    protected UnidadeEntity() {
    }

    public UnidadeEntity(UUID id, String codigo, String nome, boolean ativa,
            Instant criadoEm, Instant atualizadoEm) {
        super(criadoEm, atualizadoEm);
        this.id = id;
        this.codigo = codigo;
        this.nome = nome;
        this.ativa = ativa;
    }

    public static UnidadeEntity fromDomain(Unidade unidade) {
        return new UnidadeEntity(unidade.id(), unidade.codigo(), unidade.nome(), unidade.ativa(),
                unidade.criadoEm(), unidade.atualizadoEm());
    }

    public Unidade toDomain() {
        return new Unidade(id, codigo, nome, ativa, getCriadoEm(), getAtualizadoEm());
    }

    public UUID getId() {
        return id;
    }

    public String getCodigo() { return codigo; }
    public String getNome() { return nome; }
    public boolean isAtiva() { return ativa; }

    public void atualizar(String nome, boolean ativa) {
        this.nome = nome;
        this.ativa = ativa;
    }
}
