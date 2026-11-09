# Modelo de domínio e persistência

## Visão geral

O modelo cobre cadastro, vínculos operacionais, catálogo, estoque e ciclo inicial do pedido. Pagamento continua somente modelado.

- [Diagrama entidade-relacionamento — fonte editável](diagrams/domain-erd.mmd) · [visual SVG](diagrams/domain-erd.svg)
- [Diagrama de classes de domínio — fonte editável](diagrams/domain-classes.mmd) · [visual SVG](diagrams/domain-classes.svg)
- [Sequência de criação e liberação — fonte editável](diagrams/order-reservation-sequence.mmd) · [visual SVG](diagrams/order-reservation-sequence.svg)

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

Os estados persistíveis incluem `AGUARDANDO_PAGAMENTO`, `CANCELADO` e `EXPIRADO`, além dos estados posteriores já preparados. A TASK-007 implementa somente as transições do estado inicial para cancelado ou expirado. A sequência nominal futura é:

`AGUARDANDO_PAGAMENTO → CONFIRMADO → EM_PREPARO → PRONTO → ENTREGUE`

`CANCELADO` e `EXPIRADO` são terminais nesta versão. Confirmações tardias devem ser rejeitadas quando pagamentos forem implementados.

Cada tentativa de pagamento começa `PENDENTE` e termina em `APROVADO` ou `RECUSADO`. Estados finais não retornam a `PENDENTE`; uma repetição cria outro `Pagamento` com número de tentativa superior. O modelo permite isso, mas não decide quantas tentativas são aceitas nem seu efeito automático sobre pedido e reserva.

Uma futura recusa de pagamento ainda exige decisão. Hoje o abandono após o prazo configurado e o cancelamento liberam a reserva na mesma transação da mudança de estado.

## Relações operacionais

O pedido pertence a uma unidade. Seus itens apontam para `ProdutoUnidade` da mesma unidade por FKs compostas. O estoque não é filho do pedido: o caso de uso o reserva/libera explicitamente e os movimentos referenciam pedido/item. Pagamentos formam tentativas independentes.

## Estoque e concorrência

A disponibilidade é `quantidade_fisica - quantidade_reservada`. A API de estoque aceita `ENTRADA`/`SAIDA`; pedidos usam internamente `RESERVA`/`LIBERACAO`. Saldo, histórico e pedido são atômicos, `@Version` detecta perdas concorrentes e chaves persistentes impedem duplicação.

## Contratos internos de pedidos

O estoque expõe reserva e liberação transacionais identificadas por pedido/item. Reserva aumenta somente `quantidade_reservada`; liberação a reduz. A confirmação/baixa física pertence à futura integração de pagamento.

## Limites intencionais

Não foram modelados papéis, credenciais, endereço, categoria, promoção, entrega, estorno ou dados de adquirente. A ausência evita fixar requisitos que ainda não foram aprovados. As questões funcionais restantes estão em [mvp.md](mvp.md).
