package br.com.raizesdonordeste.shared.infrastructure.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

@MappedSuperclass
public abstract class AuditableEntity {

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected AuditableEntity() {
    }

    protected AuditableEntity(Instant criadoEm, Instant atualizadoEm) {
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    @PrePersist
    protected void prePersist() {
        var agora = Instant.now();
        if (criadoEm == null) {
            criadoEm = agora;
        }
        if (atualizadoEm == null) {
            atualizadoEm = criadoEm;
        }
    }

    @PreUpdate
    protected void preUpdate() {
        atualizadoEm = Instant.now();
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
