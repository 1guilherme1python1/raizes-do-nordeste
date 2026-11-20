# Raízes do Nordeste — Back-End

Back-end do projeto acadêmico Raízes do Nordeste. A aplicação usa Java 21, Spring Boot, PostgreSQL e Maven, organizada como monólito modular por funcionalidade. Oferece JWT, unidades, catálogo, estoque auditável, pedidos multicanal e pagamento mock.

## Pré-requisitos

Para o fluxo recomendado, basta ter:

- Docker 24 ou superior;
- Docker Compose v2 ou superior.

JDK e Maven locais não são necessários. O build multi-stage e o Maven Wrapper executam essas ferramentas dentro do Docker.

## Configuração

Copie `.env.example` para `.env` e altere os valores locais:

```bash
cp .env.example .env
```

O arquivo `.env` não deve ser versionado. As variáveis aceitas são:

| Variável | Finalidade | Padrão de desenvolvimento |
|---|---|---|
| `POSTGRES_DB` | Nome do banco | `raizes_nordeste` |
| `POSTGRES_USER` | Usuário do banco | `raizes_app` |
| `POSTGRES_PASSWORD` | Senha local | `local_development_only` |
| `POSTGRES_PORT` | Porta publicada do PostgreSQL | `5432` |
| `APP_PORT` | Porta publicada da aplicação | `8080` |
| `JWT_SECRET` | Segredo HS256 em Base64, com pelo menos 256 bits | Obrigatório; sem padrão |
| `JWT_ISSUER` | Emissor exigido no token | `raizes-do-nordeste-api` |
| `JWT_AUDIENCE` | Audiência exigida no token | `raizes-do-nordeste-clients` |
| `DEV_ADMIN_ENABLED` | Habilita bootstrap de ADMIN apenas no perfil `dev` | `false` |
| `DEV_ADMIN_EMAIL` | E-mail do ADMIN opcional de desenvolvimento | vazio |
| `DEV_ADMIN_PASSWORD` | Senha do ADMIN opcional de desenvolvimento | vazio |
| `DEV_ADMIN_DISPLAY_NAME` | Nome exibido do ADMIN de desenvolvimento | `Administrador de Desenvolvimento` |

Os valores padrão servem somente para desenvolvimento local. Ambientes compartilhados devem fornecer credenciais próprias por variáveis de ambiente.

Gere um segredo JWT local sem registrá-lo no Git:

```bash
openssl rand -base64 32
```

Copie a saída para `JWT_SECRET` no `.env`. A aplicação rejeita segredo ausente, Base64 inválido ou com menos de 256 bits.

Para criar opcionalmente um administrador no perfil `dev`, configure `DEV_ADMIN_ENABLED=true`, e-mail e senha. A rotina não sobrescreve usuários existentes e não é ativada fora do perfil `dev`.

## Executar com Docker Compose

```bash
docker compose up --build -d
docker compose ps
```

Se a porta 8080 já estiver reservada no host, escolha outra porta publicada, por exemplo:

```bash
APP_PORT=18080 docker compose up --build -d
```

O Compose espera o health check do PostgreSQL antes de iniciar a aplicação. O volume nomeado `postgres_data` preserva os dados entre reinicializações.

Para parar os contêineres sem apagar o banco:

```bash
docker compose down
```

## Logs

```bash
docker compose logs -f application
docker compose logs -f database
```

## Testes e build

Os testes unitários seguem o padrão `*Test` e são executados pelo Surefire. Os testes de integração seguem `*IT`, são executados pelo Failsafe durante `verify` e usam um PostgreSQL descartável criado pelo Testcontainers. H2 não é usado como substituto do PostgreSQL.

Sem Java ou Maven no host, execute a suíte completa compartilhando o socket Docker com o contêiner Maven:

```bash
docker run --rm \
  --add-host=host.docker.internal:host-gateway \
  -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal \
  -e JWT_SECRET="$(openssl rand -base64 32)" \
  -v "$PWD:/workspace" \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -w /workspace \
  maven:3.9.11-eclipse-temurin-21-alpine \
  ./mvnw clean verify
```

O banco do Testcontainers tem ciclo de vida restrito à suíte e não usa o banco nem o volume do Compose. A variável `TESTCONTAINERS_HOST_OVERRIDE` é necessária neste ambiente Docker Desktop; em instalações Docker Engine nativas ela pode não ser necessária. Para executar somente testes unitários, use `./mvnw test`. O build da imagem compila os testes, mas usa `-DskipITs` porque o estágio de build não recebe acesso ao socket Docker; a suíte completa deve ser executada separadamente antes do build.

## Endpoints de infraestrutura

- Health check: <http://localhost:8080/actuator/health>
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

## Coleção Postman — TASK-010

Importe [a coleção](postman/raizes-task010.postman_collection.json) e [o ambiente de exemplo](postman/raizes-local.example.postman_environment.json). A execução sequencial cria contas, unidade, produto e estoque sintéticos com identificador novo a cada rodada. Configure `baseUrl`, `adminEmail`, `adminPassword` e `testPassword` no ambiente local do Postman; não compartilhe valores de senha nem tokens exportados.

Para reproduzir em Compose isolado, configure variáveis temporárias no shell para `JWT_SECRET` (Base64 de pelo menos 32 bytes), `DEV_ADMIN_EMAIL` e `DEV_ADMIN_PASSWORD`, usando credenciais sintéticas exclusivas do teste. Em seguida:

```bash
export DEV_ADMIN_ENABLED=true APP_PORT=18082 POSTGRES_PORT=15434
export TASK010_PROJECT="raizes-task010-$(date +%s)"
docker compose -p "$TASK010_PROJECT" up --build -d
npx --yes newman@6.2.1 run postman/raizes-task010.postman_collection.json \
  -e postman/raizes-local.example.postman_environment.json \
  --env-var baseUrl=http://localhost:18082 \
  --env-var "adminEmail=$DEV_ADMIN_EMAIL" --env-var "adminPassword=$DEV_ADMIN_PASSWORD" \
  --env-var "testPassword=$DEV_ADMIN_PASSWORD"
```

No Postman, execute a coleção inteira, na ordem, com o ambiente selecionado. No Newman, acrescente `--export-environment` apontando para arquivo privado temporário se precisar recuperar `funcionarioId`, `adminId` e `unidadeId` da rodada; esse arquivo contém tokens e senhas sintéticos e não deve ser versionado ou compartilhado.

Para verificar a auditoria sem endpoint público, use o `funcionarioId` produzido na mesma rodada no SQL abaixo. A criação da conta e a associação devem aparecer com o `adminId`; a associação repetida deve ter somente um registro, com a `unidadeId` da rodada:

```sql
SELECT acao, origem, ator_id, usuario_alvo_id, unidade_id, ocorrido_em
FROM app.auditoria_administrativa
WHERE usuario_alvo_id = '<funcionarioId da execução>'
ORDER BY ocorrido_em;
```

Execute a consulta no banco isolado, por exemplo com `docker compose -p "$TASK010_PROJECT" exec database psql -U raizes_app -d raizes_nordeste`. O nome novo cria um volume independente; escolha portas livres. A coleção não cobre expiração temporal ou concorrência; esses cenários são exercitados na suíte de integração, separadamente. Resultados observados na execução desta tarefa estão em [evidências TASK-010](docs/task-010-resultados.md).

## Autenticação e usuários

| Método e caminho | Acesso |
|---|---|
| `POST /api/v1/auth/login` | Público |
| `POST /api/v1/usuarios/registro` | Público; sempre cria `CLIENTE` |
| `GET /api/v1/usuarios/me` | Bearer JWT válido |
| `POST /api/v1/usuarios` | Somente `ADMIN` |

O login retorna um access token HS256 válido por 30 minutos. Envie-o como `Authorization: Bearer <token>`. Os perfis disponíveis são `ADMIN`, `GERENTE`, `ATENDENTE`, `COZINHA` e `CLIENTE`.

## Unidades, catálogo e estoque

| Método e caminho | Acesso |
|---|---|
| `POST /api/v1/unidades` | `ADMIN` |
| `GET /api/v1/unidades` e `GET /api/v1/unidades/{id}` | JWT válido |
| `PATCH /api/v1/unidades/{id}` | `ADMIN` |
| `PUT/DELETE /api/v1/unidades/{unidadeId}/usuarios/{usuarioId}` | `ADMIN` |
| `POST /api/v1/produtos` e `PATCH /api/v1/produtos/{id}` | `ADMIN` |
| `GET /api/v1/produtos` e `GET /api/v1/produtos/{id}` | JWT válido |
| `PUT /api/v1/unidades/{unidadeId}/produtos/{produtoId}` | `ADMIN` ou `GERENTE` vinculado |
| `GET /api/v1/unidades/{unidadeId}/cardapio` | Público |
| `GET /api/v1/unidades/{unidadeId}/estoque` | `ADMIN` ou `GERENTE` vinculado |
| `POST/GET /api/v1/unidades/{unidadeId}/estoque/movimentacoes` | `ADMIN` ou `GERENTE` vinculado |

Movimentações exigem `Idempotency-Key` com até 100 caracteres. A mesma chave e conteúdo recupera o movimento original sem alterar novamente o saldo; conteúdo diferente retorna `409`. Uma oferta cria estoque zerado e só aparece no cardápio depois de uma entrada, enquanto unidade, produto e oferta estiverem ativos/disponíveis.

## Pedidos e reservas

O pagamento simulado é solicitado por `POST /api/v1/pedidos/{id}/pagamentos`, com JWT, `Idempotency-Key` e corpo `{"cenario":"APROVAR"}` ou `{"cenario":"RECUSAR"}`. A primeira chamada responde 201 e o replay idêntico 200. Consulte com `GET` no mesmo caminho. O cenário seleciona apenas o comportamento do simulador; valor e moeda vêm do pedido persistido. Não são coletados dados de cartão.

Após aprovação, usuários operacionais autorizados avançam o pedido com `PATCH /api/v1/pedidos/{id}/status` e corpo `{"status":"EM_PREPARO"}`, depois `PRONTO` e `ENTREGUE`. Cozinha vinculada pode preparar e marcar pronto; atendente vinculado pode entregar; gerente vinculado e administrador podem realizar os três passos. Clientes, atendentes e gerentes vinculados e administradores podem solicitar/consultar pagamento conforme seu escopo; cozinha não pode.

| Método e caminho | Acesso |
|---|---|
| `POST /api/v1/pedidos` | JWT; exige `Idempotency-Key` |
| `GET /api/v1/pedidos` e `GET /api/v1/pedidos/{id}` | JWT; limitado ao cliente ou às unidades vinculadas |
| `POST /api/v1/pedidos/{id}/cancelamento` | Dono, funcionário vinculado autorizado ou `ADMIN` |

Clientes criam pedidos nos canais `APP`, `WEB`, `PICKUP` e `TOTEM`; atendentes vinculados usam `BALCAO` ou `TOTEM`. Gerentes são limitados às unidades vinculadas e `ADMIN` possui escopo global. A aplicação calcula preços em BRL, reserva estoque atomicamente e libera a reserva em cancelamento ou após `${ORDER_RESERVATION_DURATION:-PT15M}`. A forma `MOCK` executa somente simulação local, sem provedor real.

As opções `ORDER_EXPIRATION_ENABLED`, `ORDER_EXPIRATION_FIXED_DELAY` e `ORDER_EXPIRATION_BATCH_SIZE` controlam o processador de expiração. O horário usa UTC e um `Clock` injetável nos testes.

## Estrutura arquitetural

A matriz de auditoria, o inventário de dados e a proposta não implementada de fidelização estão em [privacidade e auditoria](docs/architecture/privacy-audit.md). A V7 registra criação administrativa de contas e alterações de vínculo sem armazenar credenciais ou corpos de requisição.

O estado atual e as decisões do monólito modular estão em [`docs/architecture`](docs/architecture/README.md). O [modelo de domínio](docs/architecture/domain-model.md) inclui diagramas ER e de classes em Mermaid e SVG. Pacotes de funcionalidades só são criados quando contêm implementação real.

Quando uma funcionalidade for implementada, ela deve ficar abaixo do pacote raiz por feature, separando apenas as camadas necessárias:

```text
feature/
├── api
├── application
├── domain
└── infrastructure
```

O pacote `shared` contém infraestrutura e contratos técnicos compartilhados. `unidades`, `catalogo`, `estoque`, `pedidos` e `pagamentos` têm implementação funcional no processo único; o gateway de pagamento é apenas simulado.

## Banco e migrations

O Flyway é o único responsável pela evolução do schema. A V1 cria `app`, a V2 cria o modelo persistente, a V3 adiciona autenticação, a V4 adiciona vínculos e auditoria de estoque, a V5 adiciona reservas, a V6 registra pagamentos e transições e a V7 acrescenta auditoria administrativa. O Hibernate usa `ddl-auto=validate`.

## Conferência dos entregáveis

A [conferência TASK-011](docs/task-011-conferencia.md) diferencia cenários Postman executados, testes Java anteriores, modelos, propostas e pendências. Na TASK-010 o JAR pré-compilado foi uma contingência após falha de DNS, não uma prova de build reproduzível.
