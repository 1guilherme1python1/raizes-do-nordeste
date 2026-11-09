# ADR-0004 — JWT e RBAC

- Status: Aceita, substituída parcialmente pela decisão da TASK-005
- Data: 2026-11-07

## Contexto

O sistema precisa autenticar diferentes atores e limitar operações por papel e unidade, sem Keycloak ou serviço externo nesta fase.

## Decisão

Usar access token JWT stateless de 30 minutos, assinado com HS256 e segredo externo de pelo menos 256 bits, e RBAC com os papéis ADMIN, GERENTE, ATENDENTE, COZINHA e CLIENTE. Não implementar refresh token nesta etapa. Regras futuras de propriedade e unidade complementarão os papéis.

## Justificativa

JWT curto permite validação stateless das requisições e RBAC expressa responsabilidades estáveis. HS256 foi determinado pela TASK-005, substituindo a proposta assimétrica inicial para este estágio do projeto.

## Consequências

O emissor precisa proteger e rotacionar o segredo compartilhado. Alterações de papel ou desativação não invalidam automaticamente um access token já emitido. Refresh token, revogação, escopo por unidade e auditoria permanecem decisões posteriores.
