# Sprint 02 — Fundação operacional do backend

> ⚠️ **Este arquivo foi dividido.** A Sprint 02 é a única deste plano destinada ao **GPT** (não ao Claude/Cowork em modo agent) — por isso foi quebrada em 12 arquivos pequenos, um por etapa, feitos para colar um de cada vez em conversas do GPT, do zero (fundação do Docker Compose) até servir a aplicação React pelo Spring Boot (estratégia de SPA same-origin). Use a pasta **[`sprint-02-fundacao-backend-gpt/`](./sprint-02-fundacao-backend-gpt/00_indice_e_instrucoes.md)**, começando pelo `00_indice_e_instrucoes.md`.
>
> Resumo da divisão de trabalho entre agentes neste plano:
> - **Sprint 01** → executada por você com Claude/Cowork, em modo agent, neste próprio repositório.
> - **Sprint 02** → executada por você com o **GPT**, colando um arquivo por vez da pasta `sprint-02-fundacao-backend-gpt/` (etapas 01 a 12).
> - **Sprints 03 a 08** → de volta para Claude/Cowork, em modo agent.
>
> Pré-requisito: Sprint 01 concluída (frontend refatorado em `develop`).
>
> Conteúdo coberto pelas 12 etapas: estrutura do repo e `.env` · PostgreSQL dedicado (Aegis + Keycloak) · Keycloak persistente com realm/client/roles · backend Spring Boot base com Flyway · Resource Server + `/api/v1/me` · modelo core (Tenant/Membership/Product/Módulos) · Knowledge Graph MVP · health/status · build do React servido pelo Spring Boot (substitui Features 017/018 do roteiro original, que descreviam Angular — ver ADR-0011) · Dockerfile + Compose completo · seed inicial e seed do grafo · OpenAPI/testes/checklist final.
