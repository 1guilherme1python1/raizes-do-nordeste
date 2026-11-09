# Módulos e regras de dependência

## Módulos planejados

| Módulo | Responsabilidade planejada | Estado |
|---|---|---|
| `auth` | Login e emissão de access token JWT; refresh/revogação permanecem pendentes | API e application implementadas |
| `usuarios` | Identidade, credenciais, perfil e cadastro público/administrativo | API, application, domain e persistence |
| `unidades` | Cadastro, desativação e vínculo operacional de funcionários | API, application, domain e persistence |
| `catalogo` | Produto global, oferta, preço e cardápio por unidade | API, application, domain e persistence |
| `estoque` | Saldos, entradas/saídas, auditoria, concorrência e idempotência | API, application, domain e persistence |
| `pedidos` | Criação, consulta, cancelamento, expiração, preço e reserva | API, application, domain e persistence |
| `pagamentos` | Tentativas e resultados de pagamento | Domínio e persistência inicial; adapter pendente |
| `fidelidade` | Acúmulo e resgate de benefícios | Posterior ao MVP |
| `auditoria` | Registro de ações relevantes e rastreabilidade | Proposto |

Pagamentos ainda não possui caso de uso. As demais camadas existem apenas onde há comportamento entregue.

## Organização interna quando houver código

- `api`: contratos HTTP, validação sintática, autenticação do chamador e tradução de entrada/saída. Depende de `application`.
- `application`: casos de uso, transações e coordenação. Depende de `domain` e de contratos explícitos, não de adapters concretos.
- `domain`: regras e conceitos do módulo. Não depende de Spring, HTTP, JPA ou outros módulos.
- `infrastructure`: adapters de persistência, mensageria ou integrações. Implementa contratos exigidos por `application`/`domain`.

Só devem existir as camadas necessárias para a funcionalidade entregue.

## Regras entre módulos

1. Um módulo não acessa entidades JPA, repositories, tabelas ou classes `infrastructure` de outro módulo.
2. Comunicação síncrona ocorre por uma interface de aplicação pública e pequena do módulo provedor.
3. Eventos internos só serão introduzidos quando houver desacoplamento real a obter; não são requisito inicial.
4. Referências circulares são proibidas. Uma necessidade circular deve resultar em redesenho do caso de uso ou em coordenação por uma camada de aplicação claramente proprietária.
5. `shared` contém somente preocupações técnicas estáveis, como configuração transversal. Não recebe modelos de negócio genéricos.
6. DTOs HTTP não atravessam a fronteira da API; modelos de persistência não atravessam a infraestrutura.
7. Restrições poderão ser automatizadas com testes arquiteturais quando os primeiros módulos reais existirem.

## Dependências funcionais propostas

- `pedidos` consulta ofertas do `catalogo` e solicita reserva/baixa ao `estoque` por contratos de aplicação.
- uma task futura poderá fazer `pedidos` solicitar uma tentativa a `pagamentos`; a TASK-007 não executa pagamento.
- `auditoria` observa ou recebe informações explícitas dos casos de uso, sem controlar o fluxo principal.
- `auth` resolve identidade e papéis; `unidades` mantém o vínculo operacional `usuario_unidade` e aplica o escopo local.
- `fidelidade` reage apenas após a conclusão consistente do pedido e permanece fora do MVP inicial.
