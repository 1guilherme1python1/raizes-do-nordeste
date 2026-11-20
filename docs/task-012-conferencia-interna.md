# TASK-012 — conferência interna, não é documento de submissão

Fontes: os dois PDFs oficiais presentes na raiz, em especial a Tabela Consolidada do roteiro Back-End (página 18 do PDF), e o estado local do repositório. Situação nesta rodada: inspeção estática e verificação anônima de links; nenhum teste Maven ou Newman foi executado.

| Requisito da tabela | Evidência disponível | Situação |
|---|---|---|
| Análise, requisitos, multicanalidade e privacidade | README, documentação de arquitetura e inventário de privacidade | Pendente de conferência e redação pessoal no documento acadêmico; fidelização e campanhas são propostas. |
| Modelagem: casos de uso, DER e classes ou sequência | `architecture/diagrams/`, com fontes Mermaid e SVG; ressalvas do DER em `task-011-conferencia.md` | Arquivos locais confirmados; revisar com o roteiro. O ator externo de pagamento solicitado no roteiro não está representado no diagrama de casos de uso; o gateway implementado é mock interno. |
| API, persistência, fluxo de pedido e pagamento, canais e JWT/RBAC | Código em `src/main`, migrations V1–V7, README e ADR 0008 | Implementação presente localmente; execução da revisão de entrega e conferência completa do OpenAPI pendentes. Pagamento real não implementado. |
| Segurança, privacidade e auditoria | Código, V7 e `architecture/privacy-audit.md` | Controles presentes no código; definição de retenção, descarte e bases legais permanece proposta para aprovação. Não equivale a conformidade integral com a LGPD. |
| Plano de testes e coleção | `task-010-resultados.md`, `task-011-conferencia.md`, `postman/` | Evidências históricas distintas: 81 testes Maven na TASK-009; na TASK-011, 34 requisições e 48 asserções Newman, sem falhas relatadas. Não são execução desta rodada. A coleção está organizada como lista plana; conferir a organização em pastas solicitada pelo roteiro. |
| Documento, README, repositório e acesso | README e arquivos locais; repositório público consultado anonimamente | Pendente publicar uma revisão autorizada com as alterações, validá-la por clone limpo, conferir Swagger e elaborar o PDF com os dados reais. |

## Publicação e validação

- HEAD local e `main` pública consultada: `41028817eb43dfbc8c7eedfe5eec57bc134a868f`. A árvore local contém alterações rastreadas e arquivos não rastreados posteriores a essa revisão, inclusive V6, V7, pagamentos e coleção.
- Página pública do repositório e README público retornaram HTTP 200 em consulta anônima. A URL da coleção em `main` retornou HTTP 404. Isso não demonstra publicação da entrega atual.
- `.env` e `target/` estão ignorados; `git ls-files` não listou `.env`, conteúdo de `target/` ou arquivos `*.log`. Uma busca limitada por padrões comuns de segredos não identificou ocorrências; uma revisão completa dos arquivos efetivamente selecionados continua necessária antes da publicação.
- Não foram realizados clone de revisão final, build limpo, execução do `verify`, Newman ou conferência local do Swagger nesta rodada. O build normal registrado na TASK-011 e o JAR pré-compilado usado na TASK-010 são históricos e não substituem essas validações.
- A revisão pública atual não representa a entrega local. Nenhum commit, push, alteração de mounts ou envio ao AVA foi realizado nesta rodada.

## Autoria e uso de IA

Os PDFs oficiais limitam o uso de IA a apoio como revisão, organização e esclarecimento de conceitos e vedam a geração da solução, código final e respostas prontas. A conversa registra pedidos ao Codex para implementar código e produzir documentação e testes. Isso deve ser declarado fielmente, incluindo os prompts e os trechos aproveitados; a simples declaração não garante que a entrega seja aceita. Solicitar orientação ao professor sobre o material já produzido e revisar o trabalho pessoalmente antes de qualquer submissão. Este checklist não é uma declaração de autoria nem substitui essa orientação.

## Pendências da entrega acadêmica

- Informar RU e demais dados reais de capa; confirmar as normas ABNT indicadas no AVA.
- Produzir e revisar pessoalmente o documento único com capa, sumário, seções exigidas, diagramas legíveis e referências; conferir os links. Nome definitivo: `<RU>_Projeto_Back_End.PDF`.
- Documentar a ferramenta Codex, os prompts efetivamente utilizados e os trechos aproveitados sem inventar histórico; confirmar com o professor a aceitabilidade do uso ocorrido.
- Conferir as omissões de respostas e parâmetros no OpenAPI antigo com a API em execução; não inventar respostas.
- Após autorização explícita e publicação da revisão correta, clonar sem reutilizar JAR ou volume e seguir somente o README: build e Compose, V1–V7, `verify` com PostgreSQL 16.10 via Testcontainers e Newman. Registrar cada resultado separadamente e conferir acesso anônimo e Swagger local.
