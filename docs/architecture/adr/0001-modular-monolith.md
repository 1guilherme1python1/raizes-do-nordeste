# ADR-0001 — Monólito modular por funcionalidade

- Status: Aceita
- Data: 2026-11-07

## Contexto

O produto precisa crescer por capacidades de negócio, mas ainda não apresenta escala ou independência operacional que justifique serviços distribuídos.

## Decisão

Manter um único artefato Spring Boot e um único processo, organizado por módulos de funcionalidade. Dentro de cada módulo, separar API, Application, Domain e Infrastructure somente quando houver código com essas responsabilidades. Dependências entre módulos passam por contratos explícitos de aplicação.

## Justificativa

A estrutura preserva fronteiras e testabilidade sem introduzir rede, observabilidade distribuída e consistência eventual prematuramente.

## Consequências

O deploy e as transações continuam simples. Em contrapartida, as fronteiras dependem de disciplina e futuramente de testes arquiteturais. Não se criam packages vazios para simular modularidade.
