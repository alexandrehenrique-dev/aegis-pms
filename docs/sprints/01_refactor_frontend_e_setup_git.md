# Sprint 01 — Refatoração arquitetural do frontend + setup de Git

> Copie este arquivo inteiro e cole em um agente (GPT/Claude/Codex) com acesso de leitura/escrita ao repositório `aegis-pms`. Execute as seções na ordem.

## Contexto (leia antes de agir)

- O repositório `aegis-pms` tem hoje apenas a branch `main`. Não existem `release` nem `develop`.
- O frontend (`frontend/`) foi gerado via Figma Make: React 18 + Vite 6 + TailwindCSS 4 + Radix UI + MUI + `motion` + `react-dnd` + `react-router` (instalado, não usado). Já passou por 11 sprints de refinamento documentadas em `docs/implementation/figma-make-sprints-refinamento-v2.md` (Sprints 09–19) — autenticação, sidebar/navegação, theming claro/escuro/auto, simulador de papéis, drag-and-drop, Knowledge Graph, feedback, microinterações e auditoria de jornada.
- **`docs/adr/ADR-0011-frontend-framework-react.md` já decidiu: o frontend continua React.** `docs/implementation/001` propunha Angular, mas isso foi formalmente superado — não reabra essa decisão.
- O problema real do frontend não é o framework, é a **organização do código**: praticamente toda a aplicação vive em um único arquivo, `frontend/src/app/App.tsx` (~1030 linhas), com uma máquina de estados `useState<Screen>` controlando 58 telas, em vez de roteamento e separação por domínio — o que `docs/implementation/011_aegis_pms_frontend_architecture_blueprint.md` já especifica e o código atual não segue.
- Existem artefatos do processo Figma Make que não pertencem ao produto final e devem ser removidos.

### Inventário de telas atuais (`type Screen` em `App.tsx`, linha 35)

```
global | products | create | product | detail | empty | modules |
content | contentList | workflow | editor | preview | publish | versions | compare |
assets | assetUpload | assetDetail | assetMeta | assetTags | assetUsage | assetPicker |
forms | formList | formBuilder | formPreview | submissions | submissionDetail | formAnalytics | formPublication |
analytics | productHealth | contentAnalytics | formAnalyticsView | trafficChannels | reports | trends | analyticsStates |
knowledge | graphCanvas | relationships | entityDetails | entitySearch | orphans | knowledgeInsights |
settings | productSettings | tenantSettings | usersMgmt | inviteUser | userDetail | permissionMatrix | roles | accessPreview |
auditTimeline | auditDetail | securityIntegrations
```

Mais as telas de autenticação (login, seleção de tenant, seleção de produto, convite, recuperação de senha — controladas por `authUser`/estado próprio, fora do union `Screen`).

**Confirmado nesta análise: existe tela de criação de produto (`create` → "Novo Produto"), mas não existe nenhuma tela de criação de tenant** — apenas `tenantSettings`, que configura um tenant já existente. Esta sprint não cria a tela (isso é a Sprint 03); esta sprint só garante que a nova arquitetura tenha um lugar óbvio para ela.

## Objetivo

1. Migrar `App.tsx` de arquivo único para a arquitetura por domínio de `implementation/011`, sem quebrar nenhuma funcionalidade hoje existente.
2. Remover artefatos do processo Figma Make que não são produto.
3. Criar `release` e `develop`, com `release` recebendo o estado atual (pré-refatoração) e `develop` recebendo o frontend já refatorado.

## Tarefas

### A. Arquitetura alvo

Criar em `frontend/src/`:

```
app/
├── providers/        # ThemeProvider, AuthProvider, etc. (extrair de App.tsx)
├── routes/           # definição de rotas React Router v7
├── layouts/           # AppShell (Header+Sidebar+Workspace), AuthLayout
├── guards/            # PermGate, RequireAuth, RequireRole (já existem como componentes soltos — mover aqui)
└── bootstrap/         # main.tsx chama isto

core/
├── auth/              # login, tenant-select, product-select, invite, recuperação de senha
├── permissions/        # roles, capabilities, ViewAsRole/simulador (Sprint 13 do Figma Make)
├── tenants/            # estado de tenant atual, switcher
├── products/           # estado de produto atual, switcher
├── notifications/       # toasts (sonner), feedback modal (Sprint 15)
├── analytics/           # telemetria de eventos (placeholder)
└── config/             # leitura de env/flags (usado pela Sprint 07)

domains/
├── dashboard/          # Screen "global"
├── products/           # products, create, product, detail, empty, modules
├── content/            # content, contentList, workflow, editor, preview, publish, versions, compare
├── assets/             # assets, assetUpload, assetDetail, assetMeta, assetTags, assetUsage, assetPicker
├── forms/              # forms, formList, formBuilder, formPreview, submissions, submissionDetail, formAnalytics, formPublication
├── analytics/          # analytics, productHealth, contentAnalytics, formAnalyticsView, trafficChannels, reports, trends, analyticsStates
├── knowledge/          # knowledge, graphCanvas, relationships, entityDetails, entitySearch, orphans, knowledgeInsights
├── settings/           # settings, productSettings, tenantSettings, permissionMatrix, roles, securityIntegrations
├── users/              # usersMgmt, inviteUser, userDetail, accessPreview
└── audit/              # auditTimeline, auditDetail

shared/
├── components/         # mover frontend/src/app/components/ui (~50 shadcn) e ImageWithFallback para aqui
├── hooks/
├── utils/
├── constants/          # nav, modules, tenants/products mock arrays (até a Sprint 04 migrar para API)
├── types/              # Screen, ProductStatus, ModuleState, AuthUser, etc.
└── validations/

mocks/                  # dados hoje hardcoded em App.tsx (tenants, products, modules, timeline) — ver Sprint 07 para o toggle mock/real
```

Cada domínio segue a sub-estrutura de `implementation/011`: `pages/ components/ services/ hooks/ store/ routes/ contracts/ mocks/ tests/`.

### B. Roteamento real

`react-router` (v7) já está no `package.json` mas não é usado — o app atual troca de tela via `useState<Screen>`. Substituir por rotas reais:

```
/login, /select-tenant, /select-product           → core/auth (fora do AppShell)
/dashboard                                         → domains/dashboard
/products, /products/new, /products/:id, /products/:id/modules → domains/products
/content, /content/:id/editor, /content/:id/preview, /content/:id/versions → domains/content
/assets, /assets/upload, /assets/:id              → domains/assets
/forms, /forms/new, /forms/:id/submissions        → domains/forms
/analytics, /analytics/health, /analytics/reports → domains/analytics
/knowledge, /knowledge/graph, /knowledge/entities/:id → domains/knowledge
/settings, /settings/product, /settings/tenant, /settings/permissions, /settings/roles → domains/settings
/users, /users/invite, /users/:id                 → domains/users
/audit, /audit/:id                                → domains/audit
```

Lazy loading obrigatório por domínio (`React.lazy` + `Suspense`), conforme `implementation/011` ("ROTEAMENTO" → "Lazy Loading obrigatório").

Preservar exatamente o comportamento visual/UX atual — esta sprint é refatoração estrutural, não redesign. Os componentes internos de cada tela podem ser movidos como estão; só a forma de navegar entre eles muda de `setScreen("x")` para navegação por rota.

### C. Remoção de artefatos do Figma Make

Remover (após confirmar que nenhuma informação relevante se perde — se houver, migrar antes para `docs/`):

- `frontend/plans/background-estou-construindo-zesty-clock.md` (plano de "Sprint 09" já obsoleto e substituído por `figma-make-sprints-refinamento-v2.md`)
- `frontend/src/imports/` inteiro (cópias dos roteiros de `docs/implementation/`, geradas pelo Figma Make só para uso interno da ferramenta)
- `frontend/guidelines/Guidelines.md` (guideline genérico do Figma Make, não específico do Aegis)
- `frontend/ATTRIBUTIONS.md` (atribuições de assets do Figma Make)
- `frontend/README.md` (boilerplate genérico) → substituir por um README real do frontend Aegis (stack, como rodar, estrutura de pastas conforme seção A)

Manter `frontend/src/app/components/ui/` (shadcn/ui — é biblioteca de componentes real, não artefato de processo), apenas realocado para `shared/components/ui/`.

### D. Não fazer nesta sprint

- Não conectar a nenhuma API real (isso é Sprint 04).
- Não criar tela de criação de tenant (isso é Sprint 03).
- Não tocar em Keycloak (Sprint 06).
- Não mudar o visual de nenhuma tela.

## Critérios de aceite

- [ ] `npm run dev` (ou `pnpm dev`) sobe o frontend e todas as 58 telas + fluxo de auth funcionam exatamente como antes.
- [ ] Nenhuma tela perdeu funcionalidade (theming, simulador de papéis, drag-and-drop do workflow, Knowledge Graph, feedback modal, etc. continuam operacionais).
- [ ] `frontend/src/app/App.tsx` não existe mais como arquivo monolítico (ou ficou reduzido a bootstrap/composição de providers + rotas).
- [ ] Estrutura de pastas corresponde à seção A.
- [ ] Artefatos da seção C foram removidos.
- [ ] `npm run build` gera `dist/` sem erros.

## Comandos de Git (executar nesta ordem, respeitando `AGENTS.md`)

```bash
# 1. Garantir que main está limpa e atualizada
git checkout main
git pull origin main

# 2. Criar release a partir do estado atual (pré-refatoração) e publicá-la
git checkout -b release
git push -u origin release

# 3. Criar develop a partir do mesmo ponto
git checkout main
git checkout -b develop
git push -u origin develop

# 4. Criar a branch de trabalho da sprint a partir de develop
git checkout develop
git checkout -b sprint/01-refactor-frontend

# 5. ... executar as tarefas A-C, com commits Conventional Commits ...
git add -A
git commit -m "refactor(frontend): migra App.tsx monolitico para arquitetura por dominio (011)"
git add -A
git commit -m "chore(frontend): remove artefatos do figma make (plans, imports, guidelines, attributions)"
git add -A
git commit -m "feat(frontend): adiciona roteamento real com react-router substituindo maquina de estados"

# 6. Push da branch de trabalho e merge para develop (PR ou merge direto, conforme processo da equipe)
git push -u origin sprint/01-refactor-frontend
git checkout develop
git merge --no-ff sprint/01-refactor-frontend
git push origin develop

# NUNCA: git checkout main && git merge ... (merge para main é decisão humana, fora do escopo desta sprint)
# NUNCA: commitar diretamente em develop/release/main sem passar por sprint/01-refactor-frontend
```

Resultado esperado ao final: `release` contém o snapshot pré-refatoração (Figma Make "puro"); `develop` contém o frontend refatorado; `main` permanece intocada até decisão humana de promover.
