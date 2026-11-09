package br.com.raizesdonordeste.catalogo.infrastructure.persistence;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoUnidadeJpaRepository extends JpaRepository<ProdutoUnidadeEntity, UUID> {

    Optional<ProdutoUnidadeEntity> findByProdutoIdAndUnidadeId(UUID produtoId, UUID unidadeId);

    List<ProdutoUnidadeEntity> findByUnidadeId(UUID unidadeId);
}
