package br.com.raizesdonordeste.catalogo.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProdutoJpaRepository extends JpaRepository<ProdutoEntity, UUID>, JpaSpecificationExecutor<ProdutoEntity> {

    Optional<ProdutoEntity> findBySku(String sku);

    boolean existsBySku(String sku);
}
