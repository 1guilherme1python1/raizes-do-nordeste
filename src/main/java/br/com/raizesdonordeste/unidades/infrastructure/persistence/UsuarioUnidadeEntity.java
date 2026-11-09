package br.com.raizesdonordeste.unidades.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(schema = "app", name = "usuario_unidade")
@IdClass(UsuarioUnidadeId.class)
public class UsuarioUnidadeEntity {
    @Id @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;
    @Id @Column(name = "unidade_id", nullable = false)
    private UUID unidadeId;
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected UsuarioUnidadeEntity() {}
    public UsuarioUnidadeEntity(UUID usuarioId, UUID unidadeId) {
        this.usuarioId = usuarioId;
        this.unidadeId = unidadeId;
    }
    @PrePersist void prePersist() { if (criadoEm == null) criadoEm = Instant.now(); }
    public UUID getUsuarioId() { return usuarioId; }
    public UUID getUnidadeId() { return unidadeId; }
    public Instant getCriadoEm() { return criadoEm; }
}
