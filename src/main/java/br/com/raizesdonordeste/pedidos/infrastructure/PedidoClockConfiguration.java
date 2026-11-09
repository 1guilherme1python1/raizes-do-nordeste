package br.com.raizesdonordeste.pedidos.infrastructure;

import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PedidoClockConfiguration {
    @Bean @ConditionalOnMissingBean(Clock.class)
    Clock clock() { return Clock.systemUTC(); }
}
