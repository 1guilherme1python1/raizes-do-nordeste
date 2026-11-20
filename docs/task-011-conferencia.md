# TASK-011 — conferência dos entregáveis

## Limite da conferência

O roteiro acadêmico completo não foi encontrado nos arquivos do repositório nem nos anexos disponíveis. Os atores aqui representam os perfis efetivos `CLIENTE`, `ATENDENTE`, `COZINHA`, `GERENTE` e `ADMIN` e o processador agendado. Por isso, a correspondência integral com uma eventual lista externa de atores ou IDs do roteiro permanece pendente de cotejo com o documento original.

## Coleção executada

Os rótulos da coleção vão de 01 a 32, mas o rótulo 21 identifica três transições distintas. Assim há **32 rótulos numéricos e 34 requisições**. A classificação abaixo conta requisições, incluindo verificações de saldo como cenários positivos:

| Categoria | IDs | Quantidade | Evidência |
|---|---|---:|---|
| Preparação | 01–13 | 13 | Newman: health, login, contas, unidade, vínculo/replay, produto, oferta e carga. |
| Positivos | 14, 18, 19, 21 (três transições), 22–26 | 11 | Newman: aprovação, replay, entrega, recusa e saldos. |
| Negativos | 15–17, 20, 27–32 | 10 | Newman: 401, 403, 409, 400, 422 e 404. |

A execução Newman da TASK-011 realizou **34 requisições e 48 asserções, zero falhas**, contra a aplicação isolada da TASK-010. ID 30: campo `canalPedido` ausente, `422` e `validation_error`; ID 31: produto inexistente, `404`; ID 32: unidade inexistente, `404`. Os 31 pedidos/44 asserções da TASK-010 são uma execução anterior, anterior aos IDs 30–32. A coleção usa dados sintéticos novos; não usa os testes Maven como evidência. Relatório local não versionado: `/tmp/raizes-task011-newman.json`.

## Caso de uso crítico: criar pedido e pagar

**Pré-condições:** usuário autenticado e autorizado ao canal/unidade, unidade ativa, oferta e produto disponíveis, saldo disponível suficiente e chave idempotente. O pagamento exige reserva ainda válida e estado `AGUARDANDO_PAGAMENTO`.

**Fluxo principal:** o cliente ou funcionário autorizado solicita o pedido; o servidor valida a oferta, calcula o preço, persiste itens e reserva estoque numa transação. O solicitante autorizado pede o pagamento mock; a aplicação bloqueia o pedido, consulta o replay, valida o prazo, registra o resultado e, se aprovado, baixa físico e reservado e confirma o pedido. Perfis operacionais autorizados avançam por `EM_PREPARO`, `PRONTO` e `ENTREGUE`.

**Pós-condições:** na aprovação há pedido confirmado e uma baixa auditável; o fluxo operacional pode chegar a entregue. O replay válido retorna o pagamento anterior sem novo efeito. Dados são consistentes após commit.

**Exceções:** saldo insuficiente gera `409` e desfaz a criação/reserva parcial; recusa simulada é resultado `201`, cancela o pedido e libera a reserva sem reduzir o físico; pagamento após o vencimento gera `409` após persistir expiração e liberação; chaves conflitantes ou transições inválidas geram `409`. Retentativa de pagamento com nova chave não cria uma segunda tentativa efetiva.

## DER: restrições que não cabem nas cardinalidades do desenho

- `usuario_unidade` tem PK composta `(usuario_id, unidade_id)`; `produto_unidade` é único por produto/unidade; estoque é único por oferta. `0 ≤ quantidade_reservada ≤ quantidade_fisica`.
- O item de pedido usa FKs compostas para garantir pedido, oferta, produto e unidade coerentes. O banco impõe preço/subtotal não negativos e `subtotal = ROUND(preco_unitario × quantidade, 2)`; moeda do pedido é BRL.
- A chave de movimento é única por unidade; a chave de criação de pedido é única por criador/unidade; a chave de pagamento é única por pedido/solicitante. São restrições compostas, não unicidade global da coluna isolada.
- Pagamentos legados admitem várias tentativas; a V6 limita a uma tentativa efetiva com solicitante não nulo por pedido. O DER mostra a cardinalidade histórica ampla. Os campos de idempotência das V5/V6 admitem nulos para compatibilidade legada.
- A auditoria V7 exige ator para origem `USUARIO`, ator nulo para `SISTEMA`, e unidade apenas nas ações de vínculo. Movimentos ligados a pedido exigem simultaneamente pedido e item; as FKs individuais não garantem por si mesmas que o item pertença ao pedido referenciado.
- O banco não impõe ao menos um item por pedido; `1..*` é regra da API para novos pedidos. Documentos históricos podem divergir do fluxo atual.

Fontes e renderizações: [casos de uso](architecture/diagrams/use-cases.mmd) ([SVG](architecture/diagrams/use-cases.svg)), [classes](architecture/diagrams/domain-classes.mmd) ([SVG](architecture/diagrams/domain-classes.svg)), [DER](architecture/diagrams/domain-erd.mmd) ([SVG](architecture/diagrams/domain-erd.svg)) e [sequência](architecture/diagrams/order-reservation-sequence.mmd) ([SVG](architecture/diagrams/order-reservation-sequence.svg)). Nós tracejados de fidelização/campanhas são propostas não implementadas.

## Evidências e pendências

| Requisito | Evidência | Estado |
|---|---|---|
| MVP: autenticação, pedidos, reserva, mock, entrega | código em `src/main`, coleção Newman acima e [resultados TASK-010](task-010-resultados.md) | Implementado; não há pagamento real. |
| Auditoria administrativa | V7, testes `PedidoIntegrationIT` e consulta SQL da TASK-010 | Implementado; retenção ainda não definida. |
| Privacidade e fidelização | [inventário e proposta](architecture/privacy-audit.md) | Fidelização/campanhas, descarte e política jurídica são propostas, não funcionalidades. |
| Testes Java | TASK-009: 81 aprovados em PostgreSQL 16.10 | Evidência anterior, distinta do Newman; não foi repetida nesta task. |
| OpenAPI | JSON de `/v3/api-docs` examinado na aplicação isolada e novamente na imagem reconstruída; GET pedido agora lista `200/401/403/404`, cancelamento `200/401/403/404/409`, criação inclui `400` e o schema `ApiError` | Rotas e métodos confrontados com controllers; criação e pagamento exigem `Idempotency-Key`, filtros de listagem são `unidadeId`, `status`, `canalPedido` e paginação. As respostas de alguns endpoints anteriores não enumeram todos os erros possíveis; documentação exaustiva permanece pendente. |
| Build normal Compose | `docker compose -p raizes-task011 build application` terminou `BUILD SUCCESS` após as alterações OpenAPI, com `verify -DskipITs` no Dockerfile | Build normal bem-sucedido; testes de integração não são executados durante o build. |
| Documento acadêmico e PDF final | Roteiro completo não encontrado no workspace | Cotejo integral pendente; PDF não solicitado nesta task. |
