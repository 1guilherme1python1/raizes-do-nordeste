# Arquitetura do Raízes do Nordeste

## Estado atual

O projeto é uma única aplicação Spring Boot conectada a um único PostgreSQL. Estão implementados JWT/RBAC, usuários, unidades, catálogo, estoque e pedidos multicanal com reserva transacional. Pagamento ainda não possui endpoint nem execução.

O código real está limitado a:

- aplicação Spring Boot em `br.com.raizesdonordeste`;
- configuração de segurança em `shared.infrastructure.security`;
- configuração de PostgreSQL, JPA e Flyway;
- migrations `V1`–`V5`, incluindo ciclo de reserva e idempotência de pedidos;
- APIs e casos de uso para usuários, unidades, catálogo, estoque e pedidos;
- testes unitários e de integração com PostgreSQL descartável, inclusive concorrência.

## Direção arquitetural proposta

O sistema evoluirá como monólito modular, organizado por funcionalidade. Cada módulo continuará no mesmo processo e artefato, mas terá fronteiras explícitas e dependências controladas. A divisão API, Application, Domain e Infrastructure é uma regra de organização interna, não uma autorização para criar camadas vazias.

Os módulos, fluxo de MVP e regras de dependência estão em [modules.md](modules.md) e [mvp.md](mvp.md). O modelo de domínio e seus diagramas estão em [domain-model.md](domain-model.md). Persistência, segurança e estratégia de testes estão em [persistence.md](persistence.md), [security.md](security.md) e [testing.md](testing.md).

## Registro das decisões

- [ADR-0001 — Monólito modular por funcionalidade](adr/0001-modular-monolith.md)
- [ADR-0002 — Flyway como autoridade do schema](adr/0002-flyway-schema-authority.md)
- [ADR-0003 — Integração com PostgreSQL via Testcontainers](adr/0003-postgresql-integration-tests.md)
- [ADR-0004 — JWT e RBAC](adr/0004-jwt-rbac.md)
- [ADR-0005 — Modelo persistente inicial do MVP](adr/0005-mvp-domain-persistence.md)
- [ADR-0006 — Escopo por unidade e idempotência de estoque](adr/0006-unit-scope-stock-idempotency.md)
- [ADR-0007 — Pedido, reserva e idempotência transacional](adr/0007-order-reservation-idempotency.md)

“Aceita” significa implementada ou já determinada pelas diretrizes. “Proposta” exige validação do arquiteto antes da implementação funcional.
