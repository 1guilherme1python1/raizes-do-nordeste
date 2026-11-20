# Privacidade, auditoria e fidelização — TASK-009

Este documento é um inventário técnico, não uma declaração de conformidade com a LGPD. O responsável pelo projeto deve validar finalidades, hipóteses legais, prazos e procedimentos com assessoria adequada. As [perguntas frequentes da ANPD](https://www.gov.br/anpd/pt-br/acesso-a-informacao/perguntas-frequentes) descrevem hipóteses distintas de tratamento; consentimento não é exigência universal.

## Dados existentes e propostas para aprovação

| Dado e finalidade | Persistência e exposição autorizada | Retenção/descarte propostos — não implementados |
|---|---|---|
| Nome de exibição, e-mail normalizado, UUID, código, perfil e estado ativo: identificação e autenticação | `app.usuario`; respostas de cadastro, login e `/usuarios/me` mostram dados da própria conta; criação administrativa acessível a ADMIN | Definir prazo para conta inativa, exportação, eliminação ou anonimização após avaliação das relações e obrigações aplicáveis. |
| Senha: autenticação | Recebida no cadastro/login; somente hash BCrypt em `app.usuario`; nem senha nem hash fazem parte de `UsuarioResponse` | Definir ciclo de troca e descarte do hash ao encerrar conta; backups também precisam de política. |
| UUID de cliente/criador, unidade, itens, canal, estado, horários e valor: execução e acompanhamento do pedido | `app.pedido`, `app.item_pedido`; cliente acessa os próprios pedidos; equipe operacional consulta pedidos de unidades vinculadas e ADMIN tem escopo global | Definir prazo e viabilidade de pseudonimização/anonimização, inclusive restrições de chaves estrangeiras e preservação de registros necessários. |
| Resultado mock, valor, horário, solicitante e identificador simulado: execução e rastreabilidade | `app.pagamento`; resposta de pagamento tem identificador, status e motivo seguro, para cliente titular, atendente/gerente vinculados e ADMIN | Definir prazo separado para registros operacionais; esta implementação não processa cartão ou transação financeira real. |
| Ator, alvo, unidade, ação, estado/motivo e data: rastreabilidade operacional | `pedido_transicao`, `movimentacao_estoque`, `auditoria_administrativa`; não há endpoint público para trilhas administrativas; histórico de estoque é restrito a gerente vinculado ou ADMIN | Definir controles de acesso ao banco, integridade, prazo e eventual descarte ou pseudonimização. |
| JWT: autenticação da API | Retornado no login; `sub` e roles no token; não é gravado nas novas trilhas | Token expira em 30 minutos; revogação e rotação operacional são pendências. |

**Hipóteses a avaliar, não decisões jurídicas:** para dados necessários à execução de pedidos e programa de relacionamento solicitado pelo titular, avaliar execução contratual; para trilhas de segurança, avaliar obrigação legal aplicável ou legítimo interesse com teste de balanceamento. Para comunicação segmentada, avaliar a hipótese apropriada à finalidade e, quando for consentimento, sua obtenção específica e revogação. A existência de uma opção não dispensa análise do caso concreto.

**Controles existentes:** hash BCrypt, JWT assinado com validade, RBAC, escopo por unidade para operações e consultas de pedidos/pagamentos, validação no servidor e histórico transacional de estoque. **Lacunas:** não há política de retenção nem descarte automático, canal de atendimento ao titular, anonimização, gestão de backups, revogação antecipada de JWT, controle de acesso/retencão do log de infraestrutura comprovados, ou proteção contra toda exposição eventual de corpos por configurações externas. Os testes não substituem revisão jurídica nem avaliação de segurança.

## Matriz de auditoria

| Ação | Registro existente ou novo | Evidência de teste | Pendência |
|---|---|---|---|
| Criar pedido | `pedido.criado_por_usuario_id`, unidade e data | testes de criação/replay de pedidos | pedidos legados podem não ter criador. |
| Cancelar, expirar, pagar e avanço | `pedido_transicao` com ator/origem, estados e data; pagamento com solicitante e resultado | testes de pagamento, replay, expiração e concorrência | validar retenção e acesso ao banco. |
| Entrada/saída/reserva/liberação/baixa | `movimentacao_estoque` com ator ou origem implícita, unidade, quantidade e data; pedido/item nos movimentos de pedido | testes de estoque e de pedidos | movimentos legados podem ter ator nulo. |
| Criar conta interna | V7 `auditoria_administrativa`, ator ADMIN ou origem SISTEMA no bootstrap | `cadastroInternoAuditadoNaoRetornaCredenciais` | retenção e acesso ao banco pendentes de decisão. |
| Criar/remover vínculo | V7 `auditoria_administrativa`, ator, usuário alvo e unidade; associação repetida não produz evento | `auditoriaAdministrativaRegistraAtorAlvoUnidadeERejeitaRepeticaoOuNegacao`; `auditoriaReverteComOperacao` | retenção e acesso ao banco pendentes de decisão. |

Os nomes de testes acima descrevem a cobertura adicionada; consulte o relatório de execução para saber se passaram no ambiente atual. As novas trilhas não contêm senha, hash de senha, JWT, segredo ou corpo de requisição e participam da mesma transação da mudança. Não há endpoint público de auditoria administrativa.

## Fidelização e promoções — proposta, não implementada

Uma futura adesão ao programa poderia permitir ao participante consultar saldo e histórico, acumular pontos em eventos de pedido elegíveis e resgatar benefícios. Crédito e resgate deveriam usar um livro de movimentações idempotente, identificador do pedido/operação, regras de saldo e transação atômica; não há endpoints, tabelas ou pontos implementados.

A adesão ao programa deve ser separada da autorização para campanhas segmentadas. Quando a hipótese escolhida para determinada campanha for consentimento, um futuro mecanismo deve registrar titular, finalidade específica, versão do texto, data, origem e revogação; uma revogação deve impedir novos usos para essa finalidade, sem automaticamente encerrar a adesão ao programa. É necessário definir acesso, conservação da prova e atendimento de direitos.

Decisões abertas: momento do crédito (pagamento, entrega ou prazo posterior), prazo de expiração, prevenção de crédito duplicado, consequências de cancelamento/estorno, saldo negativo, política de benefícios, titularidade, retenção e análise jurídica da base de cada finalidade. Nada disso foi implementado nesta task.
