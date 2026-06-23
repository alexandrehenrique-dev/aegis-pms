# Sprint 04 — Integração frontend-backend com contratos

> ⚠️ **Obsoleta — substituída pela Sprint 09.** O diagnóstico abaixo ("zero `fetch`/`axios` em `App.tsx`") refere-se a um arquivo monolítico que não existe mais — a Sprint 01 já refatorou o frontend para `src/app/`, `src/core/`, `src/domains/`, `src/shared/`, cada domínio com seus próprios `mocks/`. O objetivo conceitual (camada `services/` + `contracts/` tipados, nenhum componente de UI chamando rede direto) continua válido e correto, mas o desenho concreto — caminhos de arquivo, lista de domínios, integração com o toggle mock/real da Sprint 07 — está em `docs/sprints/09_servicos_integracao_fluxo_super_admin_e_build.md`. Use a Sprint 09 como fonte da verdade; mantenha este arquivo apenas como registro histórico do diagnóstico original.

> Pré-requisito: Sprint 01 (arquitetura), Sprint 02 (backend rodando) concluídas.

## Contexto

Hoje o frontend é **100% mockado**: nenhuma chamada de rede real existe em `App.tsx` (confirmado — zero ocorrências de `fetch`, `axios`, `import.meta.env`, `process.env` no arquivo). Toda ação assíncrona é simulada com `setTimeout`. `implementation/011` já exige uma camada de serviços (`Nunca colocar fetch dentro de componentes`) e contratos (`Toda comunicação passa por Contracts. Nunca consumir API diretamente da UI.`) — esta sprint implementa exatamente isso.

## Objetivo

Cada domínio passa a ter uma camada `services/` que fala com a API real do backend (Sprint 02) através de `contracts/` tipados, sem que nenhum componente de UI chame rede diretamente.

## Tarefas

1. Criar `shared/services/apiClient.ts`: wrapper único de HTTP (fetch ou axios) com base URL configurável (ver Sprint 07 para o toggle mock/real), injeção de header `Authorization: Bearer <token>` (token vem de `core/auth`, ligado à Sprint 06/Keycloak), e tratamento de erro em camadas conforme `implementation/011` ("ERROR STRATEGY": Campo → Componente → Página → Aplicação).
2. Para cada domínio (`dashboard`, `products`, `content`, `assets`, `forms`, `analytics`, `knowledge`, `settings`, `users`, `audit`): criar `services/<dominio>Service.ts` com as chamadas REST correspondentes aos endpoints já definidos em `implementation/001` (Features 011-016 cobrem `/api/v1/me`, tenant/membership/product, catálogo de módulos, Knowledge Graph).
3. Criar `contracts/requests` e `contracts/responses` por domínio com os tipos TypeScript espelhando o contrato JSON do backend (alinhado a ADR-0008, JSON Contracts).
4. Substituir, domínio por domínio, os arrays mockados hoje hardcoded em `App.tsx` (`tenants`, `products`, `modules`, `timeline`) por chamadas via `services/`, mantendo um fallback de mock (ver Sprint 07) quando a API não responde.
5. Tratar paginação, loading (skeleton — já existe `SkeletonCard`/`SkeletonLines`) e erro parcial (já existe `PartialErrorWidget`) reaproveitando os componentes existentes, agora alimentados por dados reais.

## Critérios de aceite

- [ ] Nenhum componente de UI importa `fetch`/`axios` diretamente — toda chamada passa por `services/`.
- [ ] Todo shape de request/response tem tipo em `contracts/`.
- [ ] Dashboard, lista de produtos, catálogo de módulos e timeline carregam dados reais do backend da Sprint 02 quando ele está no ar.
- [ ] Estados de loading e erro parcial continuam visualmente idênticos aos mockados.

## Comandos de Git

```bash
git checkout develop && git pull origin develop
git checkout -b sprint/04-integracao-contratos
git commit -m "feat(shared): adiciona apiClient com injecao de auth e tratamento de erro em camadas"
git commit -m "feat(dominios): adiciona services e contracts por dominio"
git commit -m "refactor(frontend): substitui arrays mockados por chamadas reais via services"
git push -u origin sprint/04-integracao-contratos
git checkout develop && git merge --no-ff sprint/04-integracao-contratos && git push origin develop
```
