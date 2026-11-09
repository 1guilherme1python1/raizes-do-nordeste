package br.com.raizesdonordeste.pedidos.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

import br.com.raizesdonordeste.pedidos.domain.CanalPedido;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoJpaRepository extends JpaRepository<PedidoEntity, UUID>, JpaSpecificationExecutor<PedidoEntity> {

    List<PedidoEntity> findByCanalPedido(CanalPedido canalPedido);

    @EntityGraph(attributePaths = "itens")
    @Query("select p from PedidoEntity p where p.id = :id")
    Optional<PedidoEntity> findWithItensById(@Param("id") UUID id);

    @EntityGraph(attributePaths = "itens")
    Optional<PedidoEntity> findByCriadoPorUsuarioIdAndUnidadeIdAndIdempotencyKey(
            UUID criadoPorUsuarioId, UUID unidadeId, String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "itens")
    @Query("select p from PedidoEntity p where p.id = :id")
    Optional<PedidoEntity> findForUpdateById(@Param("id") UUID id);

    @Query("select p.id from PedidoEntity p where p.status = 'AGUARDANDO_PAGAMENTO' and p.reservaExpiraEm <= :agora order by p.reservaExpiraEm, p.id")
    List<UUID> findExpiredIds(@Param("agora") Instant agora, Pageable pageable);
}
