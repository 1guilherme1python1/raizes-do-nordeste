# Raízes do Nordeste — Back-End

Back-end do projeto acadêmico Raízes do Nordeste (UNINTER 2026). A aplicação utiliza **Java 21**, **Spring Boot 3.5.16**, **PostgreSQL 16** e **Maven**, organizada como monólito modular por funcionalidade. Oferece autenticação JWT, controle de acesso RBAC, gestão de unidades, catálogo local, estoque auditável com bloqueio concorrente, pedidos multicanal e pagamento simulado (*mock gateway*).

---

## Passo a Passo para Subir o Projeto (Guia Rápido)

Para colocar toda a solução no ar rapidamente, execute os 4 passos abaixo no terminal:

```bash
# 1. Clone o repositório (caso ainda não esteja na pasta)
git clone https://github.com/1guilherme1python1/raizes-do-nordeste.git
cd raizes-do-nordeste

# 2. Crie o arquivo de ambiente a partir do exemplo
cp .env.example .env

# 3. Gere e configure a chave obrigatória de JWT (Base64 com 256 bits)
# No Linux / macOS / Git Bash:
export NOVO_JWT_SECRET=$(openssl rand -base64 32)
# Ou via Python:
# export NOVO_JWT_SECRET=$(python3 -c "import secrets, base64; print(base64.b64encode(secrets.token_bytes(32)).decode())")

# Insira a chave gerada no .env (substituindo a linha JWT_SECRET=)
sed -i "s|^JWT_SECRET=.*|JWT_SECRET=${NOVO_JWT_SECRET}|" .env

# 4. Inicie o banco e a aplicação via Docker Compose
docker compose up --build -d
```

Verifique a saúde dos contêineres:
```bash
docker compose ps
```
Ambos os serviços (`database` e `application`) devem reportar o status `Up (healthy)`.

---

## 1. Requisitos do Sistema

- **Docker:** Versão 24.0 ou superior;
- **Docker Compose:** Versão v2 ou superior;
- *(Opcional, apenas se for executar fora do Docker):* JDK 21 e Maven 3.9+.

> **Nota:** Não é necessário ter Java ou Maven instalados no host para o fluxo padrão. O build multi-stage do Docker e o Maven Wrapper (`./mvnw`) executam tudo isoladamente dentro dos contêineres.

---

## 2. Instalação de Dependências

- **Via Docker (Recomendado):** O Dockerfile multi-stage realiza a instalação automática de todas as dependências Maven durante o build (`docker compose up --build`).
- **Localmente no Host (Opcional):**
  ```bash
  ./mvnw clean dependency:resolve
  ```

---

## 3. Configuração de Variáveis de Ambiente (.env)

Copie o `.env.example` para `.env`:
```bash
cp .env.example .env
```

| Variável | Finalidade | Padrão / Obrigatório |
|---|---|---|
| `POSTGRES_DB` | Nome do banco de dados | `raizes_nordeste` |
| `POSTGRES_USER` | Usuário da aplicação no banco | `raizes_app` |
| `POSTGRES_PASSWORD` | Senha de desenvolvimento local | `local_development_only` |
| `POSTGRES_PORT` | Porta publicada do PostgreSQL | `5432` |
| `APP_PORT` | Porta publicada da API no host | `8080` (ou `8081`) |
| `JWT_SECRET` | Segredo HS256 em Base64 (mínimo 256 bits / 32 bytes) | **Obrigatório; sem padrão** |
| `JWT_ISSUER` | Emissor exigido no token JWT | `raizes-do-nordeste-api` |
| `JWT_AUDIENCE` | Audiência exigida no token JWT | `raizes-do-nordeste-clients` |
| `DEV_ADMIN_ENABLED` | Cria automaticamente um admin no perfil `dev` | `true` ou `false` |
| `DEV_ADMIN_EMAIL` | E-mail do usuário ADMIN inicial | Ex: `admin@teste.com` |
| `DEV_ADMIN_PASSWORD` | Senha do usuário ADMIN inicial | Ex: `Senha@Forte123` |
| `DEV_ADMIN_DISPLAY_NAME` | Nome exibido do ADMIN | `Administrador de Desenvolvimento` |

### ⚠️ Regra Crítica sobre o JWT_SECRET
A aplicação utiliza o **Spring Security com OAuth2 Resource Server**. Por requisitos criptográficos da RFC 7519 e do algoritmo HMAC-SHA256:
- O segredo **NÃO pode ser uma senha em texto comum** (ex: `minhasenha`).
- Deve ser uma sequência binária de **no mínimo 32 bytes (256 bits) codificada em Base64**.
- Para gerar em qualquer terminal:
  - **Linux / Mac:** `openssl rand -base64 32`
  - **Windows PowerShell:** `python -c "import secrets, base64; print(base64.b64encode(secrets.token_bytes(32)).decode())"`

---

## 4. Banco de Dados e Migrations (Flyway)

A criação do schema e a evolução das tabelas são **100% automáticas**:
1. O Docker Compose sobe o PostgreSQL 16 e aguarda a checagem de saúde (`pg_isready`).
2. Ao iniciar, a aplicação Spring Boot aciona o **Flyway**, que aplica em ordem estrita as migrações **`V1` a `V7`** localizadas em `src/main/resources/db/migration`:
   - `V1`: Schema `app`;
   - `V2`: Entidades base (unidades, produtos, estoque, pedidos);
   - `V3`: Autenticação e perfis RBAC;
   - `V4`: Vínculos operacionais e idempotência de estoque;
   - `V5`: Ciclo de reservas e expiração temporal;
   - `V6`: Pagamento mock e histórico;
   - `V7`: Tabela de auditoria administrativa.
3. O Hibernate opera configurado com `ddl-auto=validate`, garantindo que o banco de dados real esteja sempre em total conformidade com as entidades JPA.

> **Dica de Troubleshooting (Senha do PostgreSQL):** O PostgreSQL persiste os dados no volume Docker `postgres_data`. Se você alterar `POSTGRES_PASSWORD` no `.env` após o banco já ter sido criado uma vez, o Postgres não alterará a senha antiga do volume. Caso receba erro de autenticação (`password authentication failed`), basta resetar o volume executando:
> ```bash
> docker compose down -v
> docker compose up --build -d
> ```

---

## 5. Como Iniciar e Parar a API

- **Iniciar com compilação:**
  ```bash
  docker compose up --build -d
  ```
- **Acompanhar logs da aplicação em tempo real:**
  ```bash
  docker compose logs -f application
  ```
- **Acompanhar logs do banco de dados:**
  ```bash
  docker compose logs -f database
  ```
- **Parar a aplicação sem apagar dados:**
  ```bash
  docker compose down
  ```

---

## 6. Acesso à Documentação da API (Swagger / OpenAPI)

Com os contêineres ativos, acesse diretamente no navegador:

- **Swagger UI Interativo:** <http://localhost:8080/swagger-ui.html> *(ou na porta configurada em `APP_PORT`)*
- **Especificação OpenAPI JSON:** <http://localhost:8080/v3/api-docs>
- **Health Check do Spring Actuator:** <http://localhost:8080/actuator/health>

## 7. Como Executar os Testes

A aplicação possui validação em três níveis (unitário, integração e ponta a ponta com Postman):

### 7.1 Testes Unitários e de Integração (Java / Maven)
- **Apenas testes unitários:**
  ```bash
  ./mvnw test
  ```
- **Suíte completa de integração (com PostgreSQL real via Testcontainers):**
  ```bash
  # Compartilhando o Docker socket com o container Maven:
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

### 7.2 Coleção Postman e Newman (API End-to-End)

A coleção [postman/raizes_nordeste_postman_collection.json](postman/raizes_nordeste_postman_collection.json) está organizada em **6 pastas temáticas** conforme exigido pelo roteiro acadêmico:
1. `Auth`: Health check, login de admin e cadastro de clientes;
2. `Produtos`: Gestão de filiais, vínculos, produtos e ofertas;
3. `Estoque`: Entradas idempotentes de estoque;
4. `Pedidos`: Criação de pedidos com validação e reserva atômica;
5. `Pagamento`: Simulação mock (aprovação, recusa, replay e transições operacionais);
6. `Erros`: Cenários negativos (401, 403, 404, 409 e 422).

**Como executar via Newman CLI:**
```bash
export DEV_ADMIN_ENABLED=true APP_PORT=18082 POSTGRES_PORT=15434
export COMPOSE_PROJECT_NAME="raizes-testes-$(date +%s)"
docker compose -p "$COMPOSE_PROJECT_NAME" up --build -d

npx --yes newman@6.2.1 run postman/raizes_nordeste_postman_collection.json \
  -e postman/raizes-local.example.postman_environment.json \
  --env-var baseUrl=http://localhost:18082 \
  --env-var "adminEmail=admin@teste.com" \
  --env-var "adminPassword=Volmatavs@1" \
  --env-var "testPassword=Volmatavs@1"

docker compose -p "$COMPOSE_PROJECT_NAME" down -v
```

No Postman, execute a coleção inteira, na ordem, com o ambiente selecionado. No Newman, acrescente `--export-environment` apontando para arquivo privado temporário se precisar recuperar `funcionarioId`, `adminId` e `unidadeId` da rodada; esse arquivo contém tokens e senhas sintéticos e não deve ser versionado ou compartilhado.

Para verificar a auditoria sem endpoint público, use o `funcionarioId` produzido na mesma rodada no SQL abaixo. A criação da conta e a associação devem aparecer com o `adminId`; a associação repetida deve ter somente um registro, com a `unidadeId` da rodada:

```sql
SELECT acao, origem, ator_id, usuario_alvo_id, unidade_id, ocorrido_em
FROM app.auditoria_administrativa
WHERE usuario_alvo_id = '<funcionarioId da execução>'
ORDER BY ocorrido_em;
```

Execute a consulta no banco isolado, por exemplo com `docker compose -p "$COMPOSE_PROJECT_NAME" exec database psql -U raizes_app -d raizes_nordeste`. O nome novo cria um volume independente; escolha portas livres. A coleção não cobre expiração temporal ou concorrência; esses cenários são exercitados na suíte de integração, separadamente. A suíte completa Newman concluiu 34 requisições e 48 validações com 100% de sucesso.

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

A arquitetura do sistema segue o padrão de **Monólito Modular orientado por Funcionalidades** (*vertical slicing*). As principais escolhas técnicas estão documentadas nos [Registros de Decisões Arquiteturais (ADRs)](docs/architecture/adr/), cobrindo persistência, isolamento, concorrência e segurança.

Os diagramas de modelagem conceitual (Casos de Uso, DER, Classes e Sequência) estão disponíveis em formato SVG e Mermaid na pasta [`docs/architecture/diagrams/`](docs/architecture/diagrams/). O detalhamento formal do fluxo crítico, governança de dados e conformidade com a LGPD pode ser consultado no [relatório acadêmico](docs/DOCUMENTO_ACADEMICO_UNINTER.md).

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

## Documentação Acadêmica e Entregáveis

O relatório acadêmico completo com a modelagem conceitual, decisões arquiteturais (ADRs), especificação de endpoints e conformidade com a LGPD está disponível em [docs/DOCUMENTO_ACADEMICO_UNINTER.md](docs/DOCUMENTO_ACADEMICO_UNINTER.md) e no arquivo oficial em PDF [4697578_Projeto_Back_End.PDF](4697578_Projeto_Back_End.PDF).
