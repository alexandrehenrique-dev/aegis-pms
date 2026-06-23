# Sprint 09 — Camada de serviços, fluxo Super Admin (Tenant → Produto → Usuário) e build para o backend

> Pré-requisito: Sprint 01 (refatoração concluída — **já é a estrutura real do repositório hoje**: `src/app`, `src/core`, `src/domains`, `src/shared`). Não depende da Sprint 02 estar finalizada para começar (os services podem ser construídos contra mocks e trocados depois, conforme Sprint 07), mas os **contratos** devem espelhar os endpoints já definidos em `docs/sprints/sprint-02-fundacao-backend-gpt/`.
>
> **Esta sprint substitui, na prática, as Sprints 03 e 04** (que foram escritas referenciando um `App.tsx` monolítico que não existe mais no repositório — ver nota de obsolescência no topo daqueles dois arquivos). Use esta sprint como fonte da verdade para os caminhos reais.

## Contexto

Levantamento do código atual (`frontend/src`) confirma:

- A refatoração da Sprint 01 já está aplicada: `src/app/` (bootstrap, rotas em `app/routes/index.tsx`, guards `RequireAuth`/`RequireRole`/`PermGate`, layout `AppShell`), `src/core/` (auth, config, permissions, tenants, products), `src/domains/{dashboard,products,content,assets,forms,analytics,knowledge,settings,users,audit}` (cada um com `pages/` e `mocks/`), `src/shared/` (componentes reutilizáveis).
- **Não existe nenhuma camada de `services/` ou `contracts/`.** Todo dado é mock estático importado diretamente nos componentes (`domains/<dominio>/mocks/*.mocks.ts`).
- **Não existe fluxo de criação de Tenant.** O único lugar relacionado é `domains/settings/pages/TenantSettings.tsx`, que **edita** um tenant já existente — não cria um novo. `core/auth` tem `TenantSelectScreen` (`/select-tenant`), mas é um seletor pós-login, não uma tela de gestão.
- **Criação de Produto já existe** (`domains/products/pages/CreateProductForm.tsx`, rota `/products/new`), mas não está conectada a um fluxo de Tenant recém-criado — assume-se sempre um tenant já existente.
- **Não existe nenhuma forma de atribuir um produto a um usuário específico.** O que existe é convite de usuário (`domains/users/pages/InviteUserDrawer.tsx`), que convida para o tenant/produtos em geral, não um fluxo dedicado "criei este produto, agora atribua-o a este usuário".
- **Não existe uma tela "central" do Super Admin que liste todos os tenants da plataforma.** `DashboardGlobal` (`/dashboard`) é descrita como hub, mas não lista tenants; `TenantSelectScreen` é o seletor pós-login, não uma tela de gestão/administração.
- Não existe `.env`/`.env.example` no frontend, nem nenhuma variável apontando para API ou Keycloak em `vite.config.ts`.
- Não existe `scripts/` na raiz do repositório — **já criado nesta sessão**: `scripts/build-frontend-for-backend.sh`, acionável via `npm run build:backend` dentro de `frontend/` (ver seção E abaixo).
- **Já criado nesta sessão**: credencial mock de Super Admin em `core/auth/mocks/users.ts` — `super-admin@byop.io` / `senha123` (mesmo padrão de senha dos demais usuários mock), com acesso a todos os tenants (`t1`, `t2`, `t3`) e respectivos produtos. Use essa conta para simular o fluxo de login como Super Admin enquanto as telas desta sprint ainda não existem no backend real.
- **Já criado nesta sessão**: `scripts/gitflow-finish-sprint.sh <branch>` — automatiza o fechamento de qualquer branch de sprint (merge `--no-ff` em `develop`, push, e remoção da branch só no remoto). Use-o ao final desta sprint (ver "Comandos de Git" abaixo) e também nas próximas.

## Objetivo

1. Criar a camada `services/` + `contracts/` em todos os domínios, preparando o terreno para a integração real com o backend, sem quebrar nenhuma tela hoje mockada.
2. Implementar o fluxo que falta: Super Admin cria um Tenant → cria um Produto dentro dele → atribui esse Produto a um usuário — com navegação de volta clara para a tela central de gestão de tenants em qualquer ponto do fluxo.
3. Preparar o frontend para o Keycloak (porta **8282**, conforme decisão vigente — ver nota de correção em `docs/sprints/sprint-02-fundacao-backend-gpt/`).
4. Confirmar/usar o script de build que entrega o `dist/` para o backend servir.
5. Gerar (ou atualizar, se já gerado) o relatório de endpoints esperados em `docs/trace/`.

## Tarefas

### A. Camada de serviços e contratos (todos os domínios)

1. Criar `frontend/src/shared/services/apiClient.ts`: wrapper único de HTTP (`fetch`), com:
   - `baseURL` lida de `import.meta.env.VITE_API_BASE_URL` (default `http://localhost:8080/api/v1` em dev).
   - Injeção de `Authorization: Bearer <token>` — por enquanto lendo de um `AuthContext`/`TokenProvider` ainda mockado (token fake), preparado para a Sprint 06 (Keycloak real) apenas substituir a fonte do token, não a forma de uso.
   - Tratamento de erro em camadas (Campo → Componente → Página → Aplicação), reaproveitando os componentes de erro que já existem (`PartialErrorWidget`, skeletons `SkeletonCard`/`SkeletonLines`).

2. Para cada domínio existente — `dashboard`, `products`, `content`, `assets`, `forms`, `analytics`, `knowledge`, `settings`, `users`, `audit` — e para os dois novos conceitos desta sprint — `tenants` (gestão, não apenas seleção) e `productAssignments` (atribuição de produto a usuário) — criar:
   - `domains/<dominio>/services/<dominio>Service.ts` (ou `core/tenants/services/tenantsService.ts` para tenants, já que `core/tenants` já existe como pasta).
   - `domains/<dominio>/contracts/requests.ts` e `contracts/responses.ts` com os tipos TypeScript dos shapes — espelhando exatamente os campos já definidos nos endpoints do backend (ver `docs/trace/` para a lista consolidada).
   - Por enquanto, a implementação do service **lê dos mocks existentes** (`mocks/*.mocks.ts`) por trás da mesma interface que devolverá dados reais depois (a troca mock↔real fica formalmente a cargo da Sprint 07, mas a interface já nasce pronta para isso).

3. Substituir, domínio por domínio, os imports diretos de `mocks/*.mocks.ts` dentro de componentes de página por chamadas ao `service` correspondente. Nenhum componente de UI deve importar um arquivo de `mocks/` diretamente depois desta sprint — só o `service` do próprio domínio importa o mock.

### B. Tela central do Super Admin (gestão de todos os tenants)

1. Criar `domains/tenants/pages/TenantsManagement.tsx` (novo domínio `domains/tenants/`, já que hoje só existe `core/tenants` para lógica de seleção — manter ambos: `core/tenants` para o contexto de tenant ativo da sessão, `domains/tenants` para a UI de administração global).
2. Nova rota `/admin/tenants`, protegida por `RequireRole` apenas para `super_admin` (ver `core/permissions/roles.ts`).
3. Tela lista todos os tenants (reaproveitar o shape já usado em `TenantOption`: `id`, `name`, `plan`, `productCount`, `lastAccess`, `status`), com ação primária **"Criar Tenant"**.
4. Adicionar item "Tenants" na navegação lateral (`app/layouts/AppShell.tsx`), visível somente para `super_admin`, apontando para `/admin/tenants` — esta passa a ser a "tela inicial do Super Admin" sempre acessível em um clique, de qualquer lugar do app.
5. `DashboardGlobal` ganha um card/atalho "Gestão de Tenants" linkando para `/admin/tenants` (sem remover nada do que já existe ali).

### C. Fluxo guiado: Criar Tenant → Criar Produto → Atribuir a Usuário

1. **Criar Tenant** — nova rota `/admin/tenants/new` → `domains/tenants/pages/CreateTenantForm.tsx` (`super_admin` apenas).
   - Campos: nome, slug/identificador único, plano/tier, e-mail do Tenant Admin inicial (reaproveita o componente de convite já usado em `InviteUserDrawer`).
   - Ao salvar (via `tenantsService.create`): vai para o passo 2 já com o `tenantId` em contexto — não retorna à listagem ainda.
2. **Criar Produto (no contexto do tenant recém-criado)** — reaproveitar `domains/products/pages/CreateProductForm.tsx` (rota `/products/new`), agora aceitando um parâmetro `tenantId` (via query string ou estado de navegação do `react-router`). Quando vier do fluxo do Super Admin, o seletor de tenant do formulário já vem preenchido e bloqueado (não editável), evitando erro de atribuir o produto ao tenant errado.
3. **Atribuir Produto a um Usuário** — nova tela `domains/users/pages/AssignProductUserForm.tsx`, nova rota `/admin/tenants/:tenantId/products/:productId/assign-user` (acessível também a partir de um produto já existente, fora do wizard, para uso recorrente).
   - Permite: selecionar um usuário já existente do tenant (busca/autocomplete) **ou** convidar um novo (reaproveitando o componente de convite de `InviteUserDrawer`), e escolher o papel dele nesse produto especificamente.
   - Ao concluir, mostra um resumo do que foi criado (tenant + produto + usuário atribuído) com duas ações: "Ir para o Produto" e **"Voltar para Gestão de Tenants"**.
4. Cada uma das três telas do wizard exibe, no topo, um link/breadcrumb fixo "← Voltar para Gestão de Tenants" (`/admin/tenants`) — assim o Super Admin nunca fica "perdido" no meio do fluxo, mesmo sem terminar o wizard.

### D. Preparação para Keycloak (porta 8282)

1. Criar `frontend/.env.example`:
   ```env
   VITE_API_MODE=mock
   VITE_API_BASE_URL=http://localhost:8080/api/v1
   VITE_KEYCLOAK_URL=http://localhost:8282
   VITE_KEYCLOAK_REALM=aegis
   VITE_KEYCLOAK_CLIENT_ID=aegis-web
   ```
   (`VITE_API_MODE` é formalizado na Sprint 07 — aqui só garantimos que a variável já existe e tem um default seguro.)
2. Em `core/config`, criar `getKeycloakConfig()` lendo essas variáveis — ainda sem implementar o fluxo de login real (isso é Sprint 06); só disponibilizando a configuração para quando a Sprint 06 for executada.
3. Não codificar a porta em nenhum lugar do código — sempre via env, justamente porque já mudou uma vez (de 8181 para 8282) nesta mesma sessão de planejamento.

### E. Script de build para o backend servir o SPA

Já criado nesta sessão (não recriar, apenas validar quando o backend existir):
- `scripts/build-frontend-for-backend.sh` na raiz do repositório: instala dependências, builda o frontend (`vite build`) e copia `frontend/dist/` para `backend/src/main/resources/static/`, criando o diretório se ainda não existir.
- `frontend/package.json` ganhou o script `"build:backend": "bash ../scripts/build-frontend-for-backend.sh"`.
- Quando a Sprint 02 (backend, via GPT) chegar na etapa 09 daquele plano, usar `npm run build:backend` no lugar dos comandos manuais de `cp` — é a mesma lógica, só que versionada e reaproveitável.

### F. Relatório final de endpoints esperados

Gerar (ou confirmar atualizado, se esta sprint mudou algo) o relatório em `docs/trace/00_endpoints_esperados.md`, cobrindo:
- Todo endpoint já necessário pelas telas existentes em cada domínio.
- Os novos endpoints introduzidos por esta sprint (gestão de tenants, atribuição de produto a usuário).
- Gaps identificados entre o que `implementation/001`/Sprint 02 (GPT) já cobre e o que os domínios `content`, `assets`, `forms`, `analytics`, `users`, `audit`, `settings` ainda vão precisar (hoje sem endpoint nenhum definido).

## Critérios de aceite

- [ ] Todos os domínios têm `services/` e `contracts/`; nenhum componente de página importa `mocks/*.mocks.ts` diretamente.
- [ ] `apiClient.ts` existe, com base URL configurável e injeção de `Authorization`.
- [ ] Super Admin acessa `/admin/tenants` a partir da navegação lateral, em um clique, de qualquer tela.
- [ ] Super Admin consegue: criar um tenant → criar um produto nesse tenant → atribuir o produto a um usuário (existente ou convidado) — tudo pela UI, sem editar mocks manualmente.
- [ ] Em qualquer etapa desse fluxo existe um link visível "Voltar para Gestão de Tenants".
- [ ] `frontend/.env.example` existe com as variáveis do Keycloak na porta 8282.
- [ ] `npm run build:backend` builda o frontend e copia para `backend/src/main/resources/static/` (testável mesmo com `backend/` ainda não existindo, pois o script cria o diretório).
- [ ] `docs/trace/00_endpoints_esperados.md` existe e está atualizado.

## Comandos de Git

```bash
git checkout develop && git pull origin develop
git checkout -b sprint/09-servicos-fluxo-super-admin

git commit -m "feat(shared): adiciona apiClient com base url configuravel e injecao de auth"
git commit -m "feat(dominios): adiciona services e contracts em todos os dominios, substituindo imports diretos de mocks"
git commit -m "feat(tenants): adiciona tela central de gestao de tenants para super admin (/admin/tenants)"
git commit -m "feat(tenants): adiciona fluxo criar tenant -> criar produto -> atribuir usuario, com navegacao de volta"
git commit -m "feat(config): adiciona .env.example com variaveis do keycloak na porta 8282"
git commit -m "docs(trace): gera relatorio de endpoints esperados"

git push -u origin sprint/09-servicos-fluxo-super-admin
```

Ao final, finalize a sprint no gitflow usando o script já pronto em `scripts/gitflow-finish-sprint.sh` — ele faz merge `--no-ff` em `develop`, dá push, e apaga a branch da sprint **somente no remoto** (a branch local permanece intacta):

```bash
./scripts/gitflow-finish-sprint.sh sprint/09-servicos-fluxo-super-admin
```
