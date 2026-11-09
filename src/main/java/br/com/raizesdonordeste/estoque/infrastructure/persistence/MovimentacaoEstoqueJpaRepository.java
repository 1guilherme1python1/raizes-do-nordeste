package br.com.raizesdonordeste.estoque.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MovimentacaoEstoqueJpaRepository extends JpaRepository<MovimentacaoEstoqueEntity, UUID> {

    List<MovimentacaoEstoqueEntity> findByEstoqueIdOrderByCriadoEmAsc(UUID estoqueId);

    Optional<MovimentacaoEstoqueEntity> findByUnidadeIdAndIdempotencyKey(UUID unidadeId, String idempotencyKey);

    Page<MovimentacaoEstoqueEntity> findByUnidadeId(UUID unidadeId, Pageable pageable);

    Page<MovimentacaoEstoqueEntity> findByUnidadeIdAndTipo(UUID unidadeId,
            br.com.raizesdonordeste.estoque.domain.TipoMovimentacaoEstoque tipo, Pageable pageable);
}
