package br.com.raizesdonordeste.pagamentos.infrastructure;

import java.math.BigDecimal;
import java.util.UUID;
import br.com.raizesdonordeste.pagamentos.application.GatewayPagamento;
import org.springframework.stereotype.Component;

@Component
public class MockGatewayPagamento implements GatewayPagamento {
    @Override
    public Resultado processar(UUID pedidoId, BigDecimal valor, String moeda, String cenario) {
        boolean aprovado = "APROVAR".equals(cenario);
        return new Resultado(aprovado, "MOCK-" + pedidoId, aprovado ? null : "RECUSA_SIMULADA");
    }
}
