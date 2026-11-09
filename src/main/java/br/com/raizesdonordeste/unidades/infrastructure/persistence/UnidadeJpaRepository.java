package br.com.raizesdonordeste.unidades.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UnidadeJpaRepository extends JpaRepository<UnidadeEntity, UUID>, JpaSpecificationExecutor<UnidadeEntity> {

    Optional<UnidadeEntity> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);
}
