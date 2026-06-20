# Sprint 02 — Fundação operacional do backend

> ⚠️ **Este arquivo foi dividido.** A Sprint 02 é a única deste plano destinada ao **GPT** (não ao Claude/Cowork em modo agent) — por isso foi quebrada em 22 arquivos pequenos, um por etapa, feitos para colar um de cada vez em conversas do GPT, do zero (fundação do Docker Compose) até todos os domínios de produto que o frontend já usa (content, pages, assets, forms, analytics, users, audit, settings, dashboard) e o fluxo completo de Super Admin (tenants CRUD + ProductAssignment). Use a pasta **[`sprint-02-fundacao-backend-gpt/`](./sprint-02-fundacao-backend-gpt/00_indice_e_instrucoes.md)**, começando pelo `00_indice_e_instrucoes.md`.
>
> Resumo da divisão de trabalho entre agentes neste plano:
> - **Sprint 01** → executada por você com Claude/Cowork, em modo agent, neste próprio repositório.
> - **Sprint 02** → executada por você com o **GPT**, colando um arquivo por vez da pasta `sprint-02-fundacao-backend-gpt/` (etapas 01 a 22).
> - **Sprints 05 a 11** (03/04 obsoletas, substituídas pela 09) → de volta para Claude/Cowork, em modo agent.
>
> Pré-requisito: Sprint 01 concluída (frontend refatorado em `develop`).
>
> Conteúdo coberto pelas 22 etapas: **fundação (01-08)** — estrutura do repo e `.env` · PostgreSQL dedicado (Aegis + Keycloak) · Keycloak persistente com realm/client/roles · backend Spring Boot base com Flyway · Resource Server + `/api/v1/me` · modelo core (Tenant/Membership/Product/Módulos) · Knowledge Graph MVP · health/status. **Domínios de produto (09-17)** — CRUD completo de tenant + `ProductAssignment` · content (workflow editorial, agora com `summary`/`difficultyLevel`) · assets · forms/submissions · analytics · users · audit · settings/permissões · dashboard · extras do Knowledge Graph (layout persistente, nós órfãos, preview leve de nó) — especificação literal em `docs/trace/00_endpoints_esperados.md`. **Integração final e domínio adicional (18-22)** — build do React servido pelo Spring Boot (substitui Features 017/018 do roteiro original, que descreviam Angular — ver ADR-0011) · Dockerfile + Compose completo · seed inicial (tenants/produtos/usuários) e seed do grafo · domínio `pages` (páginas compostas por seções/blocos, adicionado pela Sprint 11 do frontend) · OpenAPI/testes/checklist final.
