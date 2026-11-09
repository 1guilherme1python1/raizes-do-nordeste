# Estratégia de testes

## Convenções

| Tipo | Nome | Executor Maven | Dependências externas |
|---|---|---|---|
| Unitário | `*Test` | Surefire em `test` | Nenhuma |
| Integração | `*IT` | Failsafe em `verify` | PostgreSQL descartável via Testcontainers |

O teste unitário `EmailNormalizerTest` valida a normalização usada como identidade de login. O teste `DomainPersistenceIT` comprova:

- inicialização do contexto Spring;
- conexão cujo produto reportado é PostgreSQL;
- execução automática do Flyway;
- aplicação bem-sucedida das migrations V1–V5;
- validação do schema pelo Hibernate durante a inicialização;
- persistência de unidade, produto, oferta por unidade, estoque, movimento, pedido, itens e múltiplas tentativas de pagamento;
- consulta por canal do pedido;
- rejeição de código de unidade duplicado, reserva superior ao saldo físico e pagamento sem pedido.

`SecurityIntegrationIT` executa os doze cenários T01–T12 com MockMvc, filtros Spring Security reais, JWT assinado, BCrypt e PostgreSQL Testcontainers. Não usa `@WithMockUser` nem substitui o decoder.

`EstoqueTest` cobre entrada, saída no limite disponível, proteção da reserva e quantidades inválidas. `OperationsIntegrationIT` cobre T01–T16: unidades, produtos, oferta, cardápio, saldos, RBAC por vínculo, idempotência persistente e duas saídas concorrentes.

`PedidoIntegrationIT` cobre T01–T24 da TASK-007 com API e serviços reais: snapshots de preço, autorização, rollback integral, idempotência, cancelamento, expiração via `Clock` mutável, filtros e concorrência. Não usa `Thread.sleep`.

## Isolamento

O PostgreSQL 16.10 é criado pelo Testcontainers com credenciais efêmeras e porta aleatória. `@ServiceConnection` substitui a conexão configurada da aplicação apenas no teste. O contêiner e seus dados são descartados ao final e não compartilham o volume `postgres_data` do Compose.

Não se usa H2 para simular PostgreSQL. Falha de acesso ao Docker deve falhar explicitamente a suíte, não selecionar outro banco.

## Execução

Use `./mvnw test` para unitários e `./mvnw verify` para a suíte completa. Neste host sem JDK/Maven, o comando Docker completo está no README. A execução dentro de contêiner precisa do socket Docker; no Docker Desktop, também precisa de `TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal`.

O Dockerfile executa `verify -DskipITs`: isso mantém compilação e testes unitários no build da imagem, mas evita entregar o socket Docker ao BuildKit. A validação de integração é uma etapa separada e obrigatória antes da imagem.

## Evolução

- regras de domínio: testes unitários sem Spring;
- repositories e migrations: integração com PostgreSQL;
- segurança HTTP e contratos REST: testes de slice ou aplicação conforme o risco;
- fluxo do MVP: integração cobrindo transação, estados e falhas do pagamento mock;
- fronteiras modulares: testes arquiteturais quando houver pelo menos dois módulos reais.
