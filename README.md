# Raízes do Nordeste — Back-End

Back-end do projeto acadêmico Raízes do Nordeste. A aplicação usa Java 21, Spring Boot, PostgreSQL e Maven, organizada como monólito modular por funcionalidade. A versão atual oferece autenticação JWT, gestão de unidades, catálogo por unidade e estoque auditável.

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

| Método e caminho | Acesso |
|---|---|
| `POST /api/v1/pedidos` | JWT; exige `Idempotency-Key` |
| `GET /api/v1/pedidos` e `GET /api/v1/pedidos/{id}` | JWT; limitado ao cliente ou às unidades vinculadas |
| `POST /api/v1/pedidos/{id}/cancelamento` | Dono, funcionário vinculado autorizado ou `ADMIN` |

Clientes criam pedidos nos canais `APP`, `WEB`, `PICKUP` e `TOTEM`; atendentes vinculados usam `BALCAO` ou `TOTEM`. Gerentes são limitados às unidades vinculadas e `ADMIN` possui escopo global. A aplicação calcula preços em BRL, reserva estoque atomicamente e libera a reserva em cancelamento ou após `${ORDER_RESERVATION_DURATION:-PT15M}`. A forma `MOCK` apenas prepara a futura integração: nenhum pagamento é executado nesta versão.

As opções `ORDER_EXPIRATION_ENABLED`, `ORDER_EXPIRATION_FIXED_DELAY` e `ORDER_EXPIRATION_BATCH_SIZE` controlam o processador de expiração. O horário usa UTC e um `Clock` injetável nos testes.

## Estrutura arquitetural

O estado atual e as decisões do monólito modular estão em [`docs/architecture`](docs/architecture/README.md). O [modelo de domínio](docs/architecture/domain-model.md) inclui diagramas ER e de classes em Mermaid e SVG. Pacotes de funcionalidades só são criados quando contêm implementação real.

Quando uma funcionalidade for implementada, ela deve ficar abaixo do pacote raiz por feature, separando apenas as camadas necessárias:

```text
feature/
├── api
├── application
├── domain
└── infrastructure
```

O pacote `shared` contém apenas infraestrutura e contratos técnicos compartilhados. `unidades`, `catalogo`, `estoque` e `pedidos` possuem as camadas necessárias; pagamentos permanece apenas preparado no modelo.

## Banco e migrations

O Flyway é o único responsável pela evolução do schema. A V1 cria `app`, a V2 cria o modelo persistente, a V3 adiciona autenticação, a V4 adiciona vínculos e auditoria de estoque e a V5 adiciona o ciclo de reserva/idempotência de pedidos. O Hibernate usa `ddl-auto=validate`.

## Situação do Git neste ambiente

O caminho `.git` existe como diretório vazio e somente leitura, portanto este diretório ainda não é reconhecido como repositório. Nenhuma tentativa destrutiva de correção é feita automaticamente. Consulte [`docs/architecture/environment.md`](docs/architecture/environment.md) para o diagnóstico e a correção segura.
