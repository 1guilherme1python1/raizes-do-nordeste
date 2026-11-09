# ADR-0003 — Integração com PostgreSQL via Testcontainers

- Status: Aceita
- Data: 2026-11-07

## Contexto

H2 em modo de compatibilidade não reproduz integralmente dialeto, tipos, constraints e comportamento do PostgreSQL. O banco de desenvolvimento também não oferece isolamento suficiente para testes automatizados.

## Decisão

Testes `*IT` serão executados pelo Maven Failsafe e criarão PostgreSQL efêmero via Testcontainers. Testes `*Test` permanecem unitários e são executados pelo Surefire. Não haverá fallback silencioso para H2.

## Justificativa

Cada execução recebe banco real, limpo, com migration automática e sem tocar o volume de desenvolvimento.

## Consequências

A suíte de integração requer Docker acessível. Execução do Maven dentro de contêiner requer o socket Docker e, no Docker Desktop, a configuração do host publicado. A suíte é mais lenta que testes unitários e por isso mantém nomenclatura e fase separadas.
