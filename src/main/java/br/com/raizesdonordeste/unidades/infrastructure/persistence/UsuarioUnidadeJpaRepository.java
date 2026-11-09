package br.com.raizesdonordeste.unidades.infrastructure.persistence;

import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioUnidadeJpaRepository extends JpaRepository<UsuarioUnidadeEntity, UsuarioUnidadeId> {
    boolean existsByUsuarioIdAndUnidadeId(UUID usuarioId, UUID unidadeId);
    void deleteByUsuarioIdAndUnidadeId(UUID usuarioId, UUID unidadeId);
    List<UsuarioUnidadeEntity> findByUsuarioId(UUID usuarioId);
}
