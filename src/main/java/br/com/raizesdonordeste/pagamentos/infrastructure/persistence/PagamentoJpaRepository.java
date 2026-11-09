package br.com.raizesdonordeste.pagamentos.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PagamentoJpaRepository extends JpaRepository<PagamentoEntity, UUID> {

    List<PagamentoEntity> findByPedidoIdOrderByNumeroTentativaAsc(UUID pedidoId);
}
