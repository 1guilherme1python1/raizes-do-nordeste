# Segurança JWT e RBAC

## Estratégia implementada

A API é stateless e autentica access tokens JWT assinados com HS256. O segredo é fornecido exclusivamente por `JWT_SECRET`, em Base64, e precisa representar ao menos 256 bits. A aplicação falha no startup se ele estiver ausente, inválido ou curto. O token dura 30 minutos e valida assinatura, algoritmo, emissor, audiência e expiração.

Senhas são persistidas apenas como hash BCrypt. O JWT identifica o usuário pelo UUID no `sub` e contém somente a lista de perfis no claim `roles`; e-mail, nome e senha não entram no token. Não há refresh token, revogação ou gateway de identidade nesta versão.

CSRF está desabilitado apenas porque a API não usa cookies nem sessão para autenticação. HTTP Basic e formulário estão desabilitados. Health e documentação são públicos; somente o endpoint `health` do Actuator está exposto.

## Matriz efetiva nesta versão

| Endpoint | Público | CLIENTE | GERENTE | ATENDENTE | COZINHA | ADMIN |
|---|---:|---:|---:|---:|---:|---:|
| `POST /api/v1/auth/login` | Sim | Sim | Sim | Sim | Sim | Sim |
| `POST /api/v1/usuarios/registro` | Sim | Sim | Sim | Sim | Sim | Sim |
| `GET /api/v1/usuarios/me` | Não | Sim | Sim | Sim | Sim | Sim |
| `POST /api/v1/usuarios` | Não | Não | Não | Não | Não | Sim |
| Consultas de unidades/produtos | Não | Sim | Sim | Sim | Sim | Sim |
| Alteração de unidades/produtos e vínculos | Não | Não | Não | Não | Não | Sim |
| Oferta e estoque de unidade vinculada | Não | Não | Sim | Não | Não | Sim global |
| Cardápio por unidade | Sim | Sim | Sim | Sim | Sim | Sim |
| Criar pedido | Não | Próprio; APP/WEB/PICKUP/TOTEM | Unidade vinculada | Vinculada; BALCAO/TOTEM | Não | Sim global |
| Consultar pedidos | Não | Somente próprios | Unidade vinculada | Unidade vinculada | Unidade vinculada | Sim global |
| Cancelar pedido pendente | Não | Somente próprio | Unidade vinculada | Unidade vinculada | Não | Sim global |
| Health e Swagger/OpenAPI | Sim | Sim | Sim | Sim | Sim | Sim |

O registro público sempre cria `CLIENTE`. A role `GERENTE` é insuficiente isoladamente: o caso de uso também exige um registro em `usuario_unidade`. Somente `ADMIN` cria ou remove vínculos.

## Persistência e compatibilidade

A V3 adiciona e-mail normalizado, hash e perfil ao usuário. O conjunto é integral: ou os três campos existem ou todos são nulos. Isso preserva registros legados da V2 sem inventar credenciais; registros legados não são autenticáveis até uma futura política de migração.

E-mails são normalizados com remoção de espaços externos e conversão para minúsculas. Um índice parcial único garante concorrência segura no PostgreSQL. Usuários inativos não conseguem realizar novo login. Como não há revogação nesta task, um token já emitido continua válido até expirar.

## Administrador de desenvolvimento

O bootstrap só existe no perfil `dev` e requer `DEV_ADMIN_ENABLED=true`, `DEV_ADMIN_EMAIL` e `DEV_ADMIN_PASSWORD`. Ele cria um ADMIN apenas se o e-mail ainda não existir e nunca altera um usuário existente. A opção é desabilitada por padrão e não se aplica a outros perfis.

## Riscos e pendências

- rotação e armazenamento gerenciado do segredo fora do ambiente local;
- revogação, refresh token e invalidação por desativação/troca de perfil;
- rate limiting, bloqueio por tentativas e recuperação de senha;
- definição futura das transições posteriores ao pagamento para ATENDENTE e COZINHA;
- auditoria de operações administrativas;
- política CORS antes de integração com front-end.
