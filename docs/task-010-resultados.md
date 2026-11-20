# TASK-010 — evidências da coleção

## Ambiente

PostgreSQL `16.10-alpine3.22`, aplicação Java 21 com o JAR construído pela suíte anterior e contendo a V7, Newman `6.2.1`; base isolada no projeto Docker `raizes-task010`, porta HTTP 18082. As contas, credenciais, SKU, unidade e pedidos foram sintéticos. A construção normal com `docker compose up --build` falhou porque o estágio de build não resolveu `repo.maven.apache.org`; a aplicação foi iniciada com o JAR previamente compilado em contêiner separado na rede do projeto. Nenhuma regra de negócio foi alterada nesta task.

## Resultado observado

A coleção foi executada duas vezes, cada vez com identificador de rodada próprio. Na execução final: **31 requisições, 44 verificações, zero falhas**. Health retornou `200`/`UP`; cadastro e login sintéticos, criação de unidade, produto, oferta, estoque, conta interna e vínculo passaram.

| Cenário | Resultado efetivo |
|---|---|
| Aprovação | `201`, pagamento `APROVADO`, pedido `CONFIRMADO`; PATCH para `EM_PREPARO`, `PRONTO` e `ENTREGUE` retornou `200`. Estoque físico/reservado passou de `10/0` para `8/0`. |
| Repetição idempotente | `200`, mesmo identificador do pagamento; chave com cenário diferente retornou `409`. |
| Recusa | `201`, `RECUSADO`, pedido `CANCELADO`, motivo `RECUSA_SIMULADA`; físico/reservado `8/2` antes da recusa e `8/0` depois. |
| Negativos | Sem JWT `401`; cliente consultando pedido alheio `403`; cliente alterando estoque `403`; salto de status `409`; estoque insuficiente `409`; canal inválido `400`. |
| Privacidade | Resposta de cadastro não contém senha nem hash. |

## Auditoria consultada no PostgreSQL

Para a segunda rodada, a consulta descrita no README retornou `CRIAR_USUARIO_INTERNO` e uma única `ASSOCIAR_USUARIO_UNIDADE`, apesar de duas chamadas de associação `204`. Ambos os registros tinham origem `USUARIO`, o UUID do administrador sintético como ator e o UUID do funcionário criado como alvo; a associação continha também a unidade criada e timestamp. Nenhum endpoint de auditoria foi criado.

O relatório JSON do Newman foi gravado em `/tmp/raizes-task010-newman.json` e não é versionado. O ambiente exportado em `/tmp/raizes-task010-result-env.json` contém tokens e senhas sintéticos: não deve ser compartilhado. A coleção não exercita expiração ou concorrência; o resultado anterior de 81 testes de integração não é contado como execução da coleção.

## Complemento executado na TASK-011

A coleção foi ampliada com os IDs 30–32 e executada novamente: **34 requisições, 48 asserções, zero falhas**. Campo obrigatório ausente retornou `422`; produto inexistente e unidade inexistente retornaram `404`. Essa é uma execução distinta da TASK-010, realizada contra o ambiente isolado com JAR pré-compilado. A [conferência](task-011-conferencia.md) apresenta a contagem por categoria e as limitações do roteiro disponível.
