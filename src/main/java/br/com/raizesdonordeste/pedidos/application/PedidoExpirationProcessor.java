package br.com.raizesdonordeste.pedidos.application;

import java.time.Clock;
import java.util.UUID;
import br.com.raizesdonordeste.estoque.application.EstoqueService;
import br.com.raizesdonordeste.estoque.application.EstoqueService.ReservaItem;
import br.com.raizesdonordeste.pedidos.domain.StatusPedido;
import br.com.raizesdonordeste.pedidos.infrastructure.persistence.PedidoJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PedidoExpirationProcessor {
    private final PedidoJpaRepository pedidos; private final EstoqueService estoque; private final Clock clock;
    public PedidoExpirationProcessor(PedidoJpaRepository pedidos, EstoqueService estoque, Clock clock){this.pedidos=pedidos;this.estoque=estoque;this.clock=clock;}
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void expirar(UUID id){
        var p=pedidos.findForUpdateById(id).orElse(null);
        if(p==null||p.getStatus()!=StatusPedido.AGUARDANDO_PAGAMENTO||p.getReservaExpiraEm().isAfter(clock.instant()))return;
        var itens=p.getItens().stream().map(i->new ReservaItem(i.getId(),i.getProdutoId(),i.getQuantidade())).toList();
        estoque.liberarPedido(p.getUnidadeId(),null,p.getId(),itens); p.expirar(); pedidos.saveAndFlush(p);
    }
}
