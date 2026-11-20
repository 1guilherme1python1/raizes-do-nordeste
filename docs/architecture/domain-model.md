# Modelo de domínio e persistência

## Visão geral

O modelo cobre cadastro, vínculos operacionais, catálogo, estoque, pedidos e pagamento mock síncrono; não há pagamento real.

- [Diagrama entidade-relacionamento — fonte editável](diagrams/domain-erd.mmd) · [visual SVG](diagrams/domain-erd.svg)
- [Diagrama de classes de domínio — fonte editável](diagrams/domain-classes.mmd) · [visual SVG](diagrams/domain-classes.svg)
- [Sequência de criação e liberação — fonte editável](diagrams/order-reservation-sequence.mmd) · [visual SVG](diagrams/order-reservation-sequence.svg)
- [Casos de uso — fonte editável](diagrams/use-cases.mmd) · [visual SVG](diagrams/use-cases.svg)

O detalhe das restrições V1–V7 e do fluxo crítico está na [conferência TASK-011](../task-011-conferencia.md).

## Entidades e integridade

| Entidade | Responsabilidade | Integridade principal |
|---|---|---|
| `Usuario` | Identidade, credenciais e perfil | código e e-mail normalizado únicos; credenciais completas ou ausentes |
| `UsuarioUnidade` | Escopo operacional do funcionário | vínculo usuário/unidade único com integridade referencial |
| `Unidade` | Contexto operacional | código único |
| `Produto` | Cadastro global | SKU único |
| `ProdutoUnidade` | Preço e disponibilidade comercial local | produto/unidade único; preço não negativo |
| `Estoque` | Saldos físico e reservado | um por produto/unidade; `0 ≤ reservado ≤ físico`; versão otimista |
| `MovimentacaoEstoque` | Alteração auditável e idempotente | responsável, unidade, chave/hash; quantidade e saldos válidos |
| `Pedido` | Unidade, cliente opcional, criador, canal, estado, total e vencimento | total não negativo; moeda BRL; chave idempotente |
| `ItemPedido` | Quantidade e preço histórico | produto da mesma unidade; subtotal = preço × quantidade |
| `Pagamento` | Uma tentativa de pagamento | número único por pedido; transação única; resultado coerente com status |

## Estados

Os estados persistíveis incluem `AGUARDANDO_PAGAMENTO`, `CANCELADO`, `EXPIRADO` e os estados operacionais. A sequência implementada após pagamento aprovado é:

`AGUARDANDO_PAGAMENTO → CONFIRMADO → EM_PREPARO → PRONTO → ENTREGUE`

`CANCELADO` e `EXPIRADO` são terminais nesta versão; pagamento após vencimento não confirma o pedido.

O schema suporta estado `PENDENTE` e múltiplas tentativas históricas, mas o fluxo mock grava resultado síncrono `APROVADO` ou `RECUSADO` e aceita uma única tentativa efetiva por pedido. Replay da mesma chave retorna o pagamento anterior.

Recusa, abandono após o prazo e cancelamento liberam a reserva na mesma transação da mudança de estado.

## Relações operacionais

O pedido pertence a uma unidade. Seus itens apontam para `ProdutoUnidade` da mesma unidade por FKs compostas. O estoque não é filho do pedido: o caso de uso o reserva/libera explicitamente e os movimentos referenciam pedido/item. Pagamentos formam tentativas independentes.

## Estoque e concorrência

A disponibilidade é `quantidade_fisica - quantidade_reservada`. A API de estoque aceita `ENTRADA`/`SAIDA`; pedidos usam internamente `RESERVA`/`LIBERACAO`. Saldo, histórico e pedido são atômicos, `@Version` detecta perdas concorrentes e chaves persistentes impedem duplicação.

## Contratos internos de pedidos

O estoque expõe reserva, liberação e baixa transacionais identificadas por pedido/item. Reserva aumenta somente `quantidade_reservada`; liberação a reduz; aprovação mock baixa físico e reservado.

## Limites intencionais

Papéis e credenciais existem; endereço, categoria, promoção, entrega logística, estorno e dados de adquirente não foram modelados. As questões restantes estão em [mvp.md](mvp.md).
