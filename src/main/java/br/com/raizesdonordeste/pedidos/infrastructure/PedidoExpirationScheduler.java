package br.com.raizesdonordeste.pedidos.infrastructure;

import java.time.Clock;
import br.com.raizesdonordeste.pedidos.application.PedidoExpirationProcessor;
import br.com.raizesdonordeste.pedidos.infrastructure.persistence.PedidoJpaRepository;
import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component @EnableScheduling
@ConditionalOnProperty(name="app.pedidos.expiration.enabled",havingValue="true",matchIfMissing=true)
public class PedidoExpirationScheduler {
    private static final Logger log=LoggerFactory.getLogger(PedidoExpirationScheduler.class);
    private final PedidoJpaRepository pedidos; private final PedidoExpirationProcessor processor; private final Clock clock; private final int lote;
    public PedidoExpirationScheduler(PedidoJpaRepository pedidos,PedidoExpirationProcessor processor,Clock clock,@Value("${app.pedidos.expiration.batch-size:50}")int lote){this.pedidos=pedidos;this.processor=processor;this.clock=clock;this.lote=lote;}
    @Scheduled(fixedDelayString="${app.pedidos.expiration.fixed-delay:PT1M}")
    public void executar(){for(var id:pedidos.findExpiredIds(clock.instant(),PageRequest.of(0,lote))){try{processor.expirar(id);}catch(Exception e){log.error("Falha ao expirar pedido {}",id,e);}}}
}
