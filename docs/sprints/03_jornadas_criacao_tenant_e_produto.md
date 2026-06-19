# Sprint 03 — Jornadas de criação de Tenant e de Produto

> ⚠️ **Obsoleta — substituída pela Sprint 09.** Este documento assume um `App.tsx` monolítico (`type Screen`, linha 35) que não existe mais: a Sprint 01 já refatorou o frontend para `src/app/`, `src/core/`, `src/domains/`, `src/shared/`. O diagnóstico abaixo (criação de produto existe e é mockada; criação de tenant não existe; atribuição de produto a usuário não existe) continua correto em essência, mas o desenho da solução e os caminhos de arquivo estão em `docs/sprints/09_servicos_integracao_fluxo_super_admin_e_build.md`, que já reflete a estrutura real do repositório. Use a Sprint 09 como fonte da verdade; mantenha este arquivo apenas como registro histórico do diagnóstico original.

> Pré-requisito: Sprint 01 (arquitetura) e Sprint 02 (backend com modelo Tenant/Membership/Product) concluídas.

## Contexto

Análise do `App.tsx` atual (`type Screen`, linha 35) confirma:

- **Criação de produto já existe** como tela: `create` → label "Novo Produto", dentro do domínio `products`. É mockada (não persiste em backend real) mas o fluxo de UI existe.
- **Criação de tenant não existe em nenhum lugar do código.** O único screen relacionado a tenant é `tenantSettings`, que **configura** um tenant já existente — não cria um novo. Isso confirma o problema relatado: "tenho as telas mas não sei como criar um novo tenant".
- Para piorar, `docs/implementation/figma-make-sprints-refinamento-v2.md` (Sprint 18 — auditoria de jornada por persona) marca a jornada do Super Admin como concluída (✅) listando `Login → Dashboard Global → Criar Tenant → Configurar Tenant → Convidar Tenant Admin → Monitorar Ativação → Auditoria`, mas a evidência registrada no próprio feedback daquela sprint só liga "Configurar tenant BYOP" a `tenantSettings` — **o passo "Criar Tenant" nunca foi de fato implementado**, apenas declarado como pronto. Esta sprint corrige essa lacuna real.

## Objetivo

1. Criar a jornada completa de criação de tenant (nova, do zero).
2. Revisar a jornada de criação de produto (já existe) para garantir que ela cobre o necessário: nome, tipo, módulos iniciais, tenant ao qual pertence.
3. Conectar ambas ao backend real da Sprint 02 (`POST /api/v1/tenants`, `POST /api/v1/products` — endpoints mínimos já previstos em `implementation/001` Feature 012).

## Tarefas

### A. Criação de Tenant (nova)

- Adicionar ao domínio `domains/settings` (ou um novo `domains/tenants` se a equipe preferir isolar) uma tela `tenantCreate`, acessível apenas a `super_admin` (único papel que vê tenants no nível global, conforme simulador de papéis da Sprint 13 do Figma Make).
- Campos mínimos: nome do tenant, slug/identificador único, plano/tier (se aplicável), tenant admin inicial (e-mail para convite — reaproveitar o fluxo de `inviteUser` já existente).
- Após criação: redirecionar para `tenantSettings` do tenant recém-criado, com um estado vazio orientando os próximos passos (convidar usuários, criar primeiro produto).
- Adicionar a ação "Criar Tenant" nos Quick Actions do Dashboard Global do Super Admin (hoje os quick actions só linkam para `tenantSettings`, `inviteUser`, `usersMgmt`, `auditTimeline` — falta literalmente o link de criação).

### B. Revisão da criação de Produto

- Conferir que a tela `create` (Novo Produto) pede: nome, tipo de produto, tenant (se o usuário tiver acesso a mais de um), módulos iniciais habilitados (reaproveitar o catálogo de módulos — tela `modules`).
- Garantir que o produto criado aparece imediatamente na lista (`products`) com status `"Pendente"` ou `"Sem módulos"` conforme módulos selecionados — replicando o comportamento mockado hoje existente para "Conecta Talentos" (`status: "Sem módulos"`, `last: "Produto criado há 1 dia"`).

### C. Contrato com o backend

- Definir em `domains/settings/contracts` (tenant) e `domains/products/contracts` (produto) os shapes de request/response, alinhados aos endpoints mínimos de `implementation/001` Feature 012.
- Esta sprint pode seguir usando mock se a Sprint 04 (integração real) ainda não tiver sido executada — mas o contrato (tipos/schema) já deve ser definido nesta sprint para não ser redesenhado depois.

## Critérios de aceite

- [ ] Super Admin consegue criar um tenant do zero pela UI, sem editar código nem dados mockados manualmente.
- [ ] Fluxo de criação de tenant termina convidando um Tenant Admin.
- [ ] Tela de criação de produto pede tenant + módulos iniciais.
- [ ] Contratos de request/response documentados em `contracts/` de cada domínio.
- [ ] `docs/implementation/figma-make-sprints-refinamento-v2.md` recebe uma nota apontando que a lacuna da jornada Super Admin (Sprint 18) foi corrigida aqui.

## Comandos de Git

```bash
git checkout develop && git pull origin develop
git checkout -b sprint/03-jornadas-tenant-produto
# ... implementar A, B, C ...
git commit -m "feat(tenants): adiciona jornada de criacao de tenant"
git commit -m "feat(products): revisa jornada de criacao de produto com tenant e modulos iniciais"
git commit -m "docs(sprints): registra correcao da lacuna criar-tenant identificada no sprint 18 do figma make"
git push -u origin sprint/03-jornadas-tenant-produto
git checkout develop && git merge --no-ff sprint/03-jornadas-tenant-produto && git push origin develop
```
