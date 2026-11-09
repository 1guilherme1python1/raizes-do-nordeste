# ADR 0006 — Escopo por unidade e idempotência de estoque

## Status

Aceita.

## Decisão

O vínculo operacional é persistido em `usuario_unidade`. `ADMIN` tem escopo global; `GERENTE` precisa de role e vínculo, verificados no caso de uso. Apenas `ADMIN` altera vínculos.

Cada movimentação persiste unidade, responsável, `Idempotency-Key` e SHA-256 do conteúdo canônico. A chave é única por unidade. Repetição com o mesmo hash recupera o resultado; hash diferente gera `409`.

Saldo e movimento pertencem à mesma transação. `Estoque.versao` mantém optimistic locking; conflitos causam rollback e não são repetidos automaticamente.

## Consequências

- a garantia sobrevive a reinícios e múltiplas instâncias;
- saídas concorrentes não podem sobrescrever saldo;
- registros legados de movimento permanecem válidos sem responsável/idempotência;
- reservas e baixas de pedidos exigirão contratos internos idempotentes próprios.
