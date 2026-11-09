# ADR-0005 — Modelo persistente inicial do MVP

- Status: Aceita
- Data: 2026-11-08

## Contexto

O MVP precisa representar catálogo por unidade, estoque concorrente, pedidos multicanal e várias tentativas de pagamento sem antecipar casos de uso ou integrações externas.

## Decisão

Usar UUID, `Instant`/`TIMESTAMPTZ`, dinheiro em `BigDecimal`/`NUMERIC(12,2)` e enums persistidos como texto. Separar produto global de sua oferta por unidade. Manter saldo físico e reservado em estoque com versão otimista e registrar movimentos em entidade própria. Tratar pagamento como tentativa numerada de um pedido. Itens preservam preço e subtotal no momento do pedido.

Referências entre módulos são identificadores no domínio e chaves estrangeiras no banco; entidades JPA não atravessam módulos. Apenas o agregado pedido–itens possui associação JPA, lazy e sem `CascadeType.ALL`.

## Justificativa

O desenho preserva histórico de preço, integridade por unidade, auditoria de saldo e repetição de pagamento. A versão otimista detecta atualizações concorrentes sem impor bloqueio permanente.

## Consequências

Casos de uso futuros deverão atualizar estoque e registrar movimento na mesma transação, tratar conflito de versão e validar transições de estado. A V2 restringe moeda a BRL e os conjuntos iniciais de canais e estados; novas opções exigirão migration. A política temporal de reserva, baixa, liberação e cancelamento continua pendente de decisão do arquiteto.
