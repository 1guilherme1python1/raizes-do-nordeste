# Escopo funcional proposto para o MVP

## Fluxo mínimo

O corte implementado cria pedidos multicanal, registra snapshots de preço e reserva estoque. O pedido permanece aguardando pagamento; integração ou execução de pagamento não faz parte da TASK-007.

## Incluído no MVP proposto

- catálogo mínimo necessário para selecionar itens válidos;
- pedido com itens, valores monetários e estado explícito;
- forma de pagamento `MOCK` apenas como contrato preparatório;
- cancelamento e expiração com liberação idempotente da reserva;
- persistência PostgreSQL evoluída exclusivamente por migrations;
- autorização mínima para as operações expostas;
- testes unitários das regras e testes de integração de persistência/fluxo;
- rastreabilidade técnica dos eventos relevantes.

## Fora do MVP inicial

- gateway de pagamento real, estorno financeiro real ou conciliação;
- fidelidade, promoções complexas e integrações externas;
- microsserviços, filas distribuídas, Kubernetes ou Keycloak;
- dados pessoais reais;
- relatórios avançados e operação multicanal completa.

## Decisões já materializadas no modelo

- UUID como identificador interno;
- dinheiro em `NUMERIC(12,2)`/`BigDecimal`, exclusivamente BRL no MVP;
- pedido obrigatoriamente associado a uma unidade e a um canal;
- preço e disponibilidade por produto/unidade;
- saldo físico e reservado, com histórico de movimentação;
- múltiplas tentativas de pagamento por pedido.

## Decisões que o arquiteto ainda precisa aprovar antes dos casos de uso

1. Eventos e permissões que disparam cada transição de pedido, especialmente cancelamento após confirmação e nova tentativa após recusa.
2. Regra determinística do pagamento mock e como o cenário é escolhido sem contaminar o contrato público.
3. Momento da reserva e da baixa de estoque e comportamento em falha de pagamento.
4. Limite transacional entre pedido, pagamento mock e estoque; estratégia de idempotência.
5. Identificador público separado do UUID e política de arredondamento além da escala de duas casas.
6. Unidade proprietária do pedido e permissões de acesso entre unidades.
7. Dados mínimos de cliente e política de anonimização/retenção.
8. Versão inicial e convenção dos endpoints REST e formato de erros.
9. Conteúdo, retenção e acesso aos registros de auditoria.
10. Proposta de segurança descrita em [security.md](security.md).

Não há implementação de fluxo funcional nesta task; o modelo persistente estabelece uma fronteira revisável para o próximo planejamento.
