package br.com.raizesdonordeste.pagamentos.application;

import java.math.BigDecimal;
import java.util.UUID;

public interface GatewayPagamento {
    Resultado processar(UUID pedidoId, BigDecimal valor, String moeda, String cenario);
    record Resultado(boolean aprovado, String transacaoId, String motivo) {}
}
