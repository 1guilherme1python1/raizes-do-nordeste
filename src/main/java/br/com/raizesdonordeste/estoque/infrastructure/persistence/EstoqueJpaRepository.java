package br.com.raizesdonordeste.estoque.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EstoqueJpaRepository extends JpaRepository<EstoqueEntity, UUID> {

    Optional<EstoqueEntity> findByProdutoUnidadeId(UUID produtoUnidadeId);

    Page<EstoqueEntity> findByUnidadeId(UUID unidadeId, Pageable pageable);

    java.util.List<EstoqueEntity> findByUnidadeId(UUID unidadeId);
}
