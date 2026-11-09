# Persistência

## Decisões vigentes

- PostgreSQL é o banco relacional da aplicação e dos testes de integração.
- O Flyway é a única autoridade para criar ou alterar schemas, tabelas, índices e constraints.
- Hibernate usa `ddl-auto=validate`: uma divergência entre mapeamentos futuros e schema deve impedir a inicialização.
- Migrations aplicadas não são editadas; correções são novas migrations versionadas.
- O schema atual é `app`; a V2 mantém um schema único e propriedade lógica das tabelas por módulo.
- Credenciais são externas e não entram no repositório.

## Modelo vigente

A V2 cria o modelo inicial e a V3 adiciona credenciais e perfil. A V4 cria `usuario_unidade` e acrescenta à movimentação unidade, responsável, chave idempotente e hash canônico da requisição. A unicidade `(unidade_id, idempotency_key)` torna a garantia persistente. Identificadores são UUID, instantes são `TIMESTAMPTZ`/`Instant` e valores monetários são `NUMERIC(12,2)`/`BigDecimal`.

`produto` é o cadastro global. `produto_unidade` define preço e disponibilidade comercial em uma unidade. Chaves estrangeiras compostas impedem que estoque ou item de pedido misturem unidades. Itens guardam preço unitário e subtotal como fotografia histórica.

`estoque` mantém saldo físico e reservado, com `@Version` para concorrência otimista. Saldo e movimento são gravados na mesma transação. Conflitos de versão causam rollback e `409`; operações mutáveis não são repetidas automaticamente. O hash idempotente inclui unidade, responsável, produto, tipo, quantidade e motivo.

A V5 associa movimentos de reserva/liberação ao pedido e item, adiciona versão, vencimento, criador e chave/hash idempotente ao pedido. Um índice parcial atende a busca de pedidos pendentes vencidos. Pedido, itens e todas as reservas são confirmados ou revertidos juntos.

`pagamento` representa uma tentativa, não o pagamento agregado: a unicidade `(pedido_id, numero_tentativa)` permite várias tentativas ordenadas e impede duplicidade. O identificador de transação também é único.

## Fronteiras

Cada módulo será proprietário lógico de suas tabelas e repositories. Outros módulos não poderão consultar essas tabelas diretamente. O uso de um banco físico único não elimina essa fronteira.

Transações devem começar nos casos de uso de aplicação. Transações distribuídas não fazem parte da solução. Para o MVP, pedido, estoque e pagamento estão no mesmo processo e banco, mas a ordem entre reserva, tentativa de pagamento e confirmação ainda precisa de decisão explícita.

## Riscos a controlar

- dependência acidental entre tabelas de módulos por acesso direto;
- migrations incompatíveis ou dependentes de ordem implícita;
- diferenças entre PostgreSQL e bancos em memória;
- execução de testes contra banco de desenvolvimento por configuração herdada;
- valores padrão de desenvolvimento usados em ambientes compartilhados.

O teste da TASK-003 elimina o risco de H2 mascarar diferenças de dialeto: ele recebe a URL e as credenciais do contêiner isolado por `@ServiceConnection`.
