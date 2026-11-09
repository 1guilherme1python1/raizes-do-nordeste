# ADR-0002 — Flyway como autoridade do schema

- Status: Aceita
- Data: 2026-11-07

## Contexto

A evolução reproduzível do PostgreSQL não pode depender da geração implícita de DDL pelo ORM.

## Decisão

Toda alteração de schema será feita por migrations Flyway versionadas. Hibernate permanecerá em `ddl-auto=validate`.

## Justificativa

Migrations explícitas são revisáveis, ordenadas e reproduzíveis; a validação do ORM detecta divergências sem modificar o banco.

## Consequências

Cada mudança de mapeamento exige migration correspondente. Migrations já compartilhadas não podem ser reescritas. Erros de ordem ou compatibilidade interrompem o startup e o build de integração.
