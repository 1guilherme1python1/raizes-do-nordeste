# ADR 0007 — Pedido, reserva e idempotência transacional

## Status

Aceita em 2026-11-08.

## Decisão

A criação do pedido, seus itens, os snapshots de preço e as reservas de estoque formam uma única transação PostgreSQL. O preço é sempre lido da oferta da unidade e calculado no servidor em BRL. O pedido nasce em `AGUARDANDO_PAGAMENTO` e a reserva vence, por padrão, após 15 minutos.

`Idempotency-Key` é persistida por solicitante e unidade com hash SHA-256 do comando canônico. Um advisory lock transacional serializa repetições simultâneas, inclusive entre instâncias. A mesma chave e conteúdo recupera o pedido; conteúdo diferente retorna conflito.

Cancelamento e expiração bloqueiam o pedido, revalidam o estado e liberam a reserva na mesma transação. A expiração usa `Clock`, busca lotes pequenos e processa cada pedido em transação independente. Movimentos de `RESERVA` e `LIBERACAO` referenciam pedido e item.

## Consequências

- não há reserva parcial quando qualquer item falha;
- concorrência é protegida por versão do estoque e unicidade persistente;
- múltiplas instâncias podem executar o scheduler sem liberar duas vezes;
- a TASK-007 não confirma pagamento nem reduz o saldo físico.
