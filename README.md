# Raízes do Nordeste — Back-End

Projeto de Back-End desenvolvido para o **Projeto Multidisciplinar (Trilha Back-End)** do **Centro Universitário Internacional UNINTER** (2026).

- **Estudante:** Guilherme Bom Despacho Lemos Vieira
- **RU:** 4697578
- **Polo de Apoio:** Rio Branco - AC
- **Orientadora:** Prof. Me. Luciane Yanase Kanashiro
- **Relatório Oficial em PDF:** [4697578_Projeto_Back_End.PDF](4697578_Projeto_Back_End.PDF)

---

## 🛠️ Tecnologias Utilizadas

- **Linguagem & Framework:** Java 21 LTS e Spring Boot 3.5.16
- **Banco de Dados:** PostgreSQL 16 com migrações versionadas via **Flyway (V1 a V7)**
- **Segurança & Autenticação:** Spring Security 6 com OAuth2 Resource Server (JWT stateless assinado com HS256) e senhas criptografadas com BCrypt
- **Concorrência de Estoque:** Bloqueio pessimista (`PESSIMISTIC_WRITE`) no banco de dados para reserva atômica de pedidos
- **Resiliência:** Chaves de idempotência (`Idempotency-Key`) em pagamentos e movimentações de estoque
- **Conteinerização:** Docker e Docker Compose multi-stage

---

## 🚀 Como Executar a Aplicação

A aplicação está totalmente conteinerizada, não sendo necessário instalar Java ou Maven na máquina local.

### 1. Clonar o Repositório e Configurar o Ambiente
```bash
git clone https://github.com/1guilherme1python1/raizes-do-nordeste.git
cd raizes-do-nordeste
cp .env.example .env
```

Abra o arquivo `.env` e configure as variáveis principais:
- `JWT_SECRET`: Chave secreta de no mínimo 32 caracteres (256 bits) para assinatura dos tokens JWT.
- `DEV_ADMIN_ENABLED=true`: Habilita a carga automática do usuário administrador inicial no perfil `dev`.
- `DEV_ADMIN_EMAIL=admin@teste.com`: E-mail de login do administrador inicial.
- `DEV_ADMIN_PASSWORD=local_dev_password`: Senha de login do administrador inicial.

### 2. Subir os Contêineres
```bash
docker compose up --build -d
```

O Docker Compose inicializa o PostgreSQL 16, aguarda a verificação de saúde (`pg_isready`), executa as migrações Flyway e disponibiliza a aplicação na porta `8080`.

Para verificar se os contêineres estão saudáveis:
```bash
docker compose ps
```

Para visualizar os logs da aplicação:
```bash
docker compose logs -f application
```

Para parar os serviços:
```bash
docker compose down
```

---

## 📖 Documentação da API (Swagger / OpenAPI)

Com os contêineres em execução, a documentação interativa e contratos REST podem ser acessados em:

- **Swagger UI Interativo:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **Especificação OpenAPI JSON:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- **Health Check da Aplicação:** [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

---

## 🧪 Como Executar os Testes

### 1. Coleção Postman (API End-to-End)
A coleção oficial com **34 requisições e 48 asserções automatizadas (zero falhas)** está localizada em:
- Arquivo da Coleção: [`postman/raizes_nordeste_postman_collection.json`](postman/raizes_nordeste_postman_collection.json)
- Ambiente de Exemplo: [`postman/raizes-local.example.postman_environment.json`](postman/raizes-local.example.postman_environment.json)

A coleção é organizada em 6 pastas temáticas cobrindo todos os fluxos da aplicação:
1. `Auth`: Autenticação e auto-cadastro de clientes;
2. `Produtos`: Cadastro global de produtos, filiais e oferta local de cardápio;
3. `Estoque`: Entradas físicas auditadas com chave de idempotência;
4. `Pedidos`: Abertura de pedidos multicanal (`APP`, `TOTEM`, `BALCAO`, `WEB`) e reserva de estoque;
5. `Pagamento`: Simulação mock (aprovação, recusa, replay idempotente) e transição de preparo da cozinha;
6. `Erros`: Cenários negativos com validação de payload padronizado (`ApiError`).

Para executar via **Newman CLI**:
```bash
npx --yes newman@6.2.1 run postman/raizes_nordeste_postman_collection.json \
  -e postman/raizes-local.example.postman_environment.json \
  --env-var baseUrl=http://localhost:8080 \
  --env-var "adminEmail=admin@teste.com" \
  --env-var "adminPassword=local_dev_password"
```

### 2. Testes de Integração (JUnit 5 com Testcontainers)
A aplicação possui 81 testes de integração automatizados rodando contra contêineres reais de PostgreSQL 16:
```bash
./mvnw clean test
```

---

## 📂 Documentação e Entregáveis do Projeto

- **Relatório Oficial em PDF (29 páginas, Normas ABNT):** [4697578_Projeto_Back_End.PDF](4697578_Projeto_Back_End.PDF)
- **Documento Acadêmico em Markdown:** [docs/DOCUMENTO_ACADEMICO_UNINTER.md](docs/DOCUMENTO_ACADEMICO_UNINTER.md)
- **Diagramas de Arquitetura (SVG e Mermaid):** [docs/architecture/diagrams/](docs/architecture/diagrams/)
  - Figura 1: Diagrama de Casos de Uso (com os 5 atores obrigatórios)
  - Figura 2: Diagrama Entidade-Relacionamento (DER físico em PostgreSQL)
  - Figura 3: Diagrama de Classes de Domínio
  - Figura 4: Diagrama de Sequência (Criação, Reserva e Liquidação Mock)
- **Registros de Decisões Arquiteturais (ADRs 0001 a 0008):** [docs/architecture/adr/](docs/architecture/adr/)
