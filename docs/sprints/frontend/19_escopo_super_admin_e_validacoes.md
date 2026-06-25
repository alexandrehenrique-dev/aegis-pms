# Sprint 19 — Escopo SUPER_ADMIN, ciclo de vida de usuários e validações de formulário

> Pré-requisito: Sprint 06 (auth real via proxy backend), Sprint 07 (user management mock existente).
> Companion no backend: `docs/sprints/backend/15_dominio_users.md` (soft delete + restore + listagem por papel) e ADRs 0018, 0019, 0020.

## Contexto

Três gaps encontrados na auditoria de cenários pós-Sprint 18:

**Gap 1 — SUPER_ADMIN vê rotas de produto que não deveria acessar.** `core/permissions/roles.ts` não tem `roleBlockedRoutePrefixes["super_admin"]` definido — o SUPER_ADMIN vê `/content`, `/pages`, `/assets`, `/forms`, `/analytics`, `/knowledge` na navbar e pode navegar até essas rotas. ADR-0018 define que o SUPER_ADMIN é operador de plataforma: gerencia tenants, produtos, usuários e infraestrutura, mas não acessa conteúdo de produtos de clientes (LGPD). Para seus próprios produtos (aqueles em que tem `ProductAssignment`), acessa via fluxo normal.

**Gap 2 — Ciclo de vida de usuário incompleto.** `UserDetailPanel.tsx` e `UserTable.tsx` têm apenas `block` e `resend-invite` — não existem `DELETE` (remover do tenant) nem `restore` (reativar). ADR-0020 define soft delete: remoção não é permanente, conta pode ser restaurada. O status `"removido"` não existe no frontend (só `"ativo"`, `"bloqueado"`, `"convidado"`). A listagem de usuários para `PRODUCT_MANAGER` deve mostrar apenas usuários com quem compartilha produtos (ADR-0019) — hoje o filtro de UI é client-side/mock sem essa distinção.

**Gap 3 — Validações apenas no backend.** Formulários críticos (convite de usuário, criação de tenant, criação de produto, atribuição de produto) têm validação na submissão via mensagem de erro de API, mas sem validação inline no form — UX ruim e sem paridade com as validações Jakarta que o backend aplica.

Padrão de mercado validado: Contentful separa explicitamente o papel de Organization Admin (infraestrutura) do Space Editor (conteúdo), sem sobreposição. Webflow separa "Workspace" de "Site". Ambos têm validação inline nos formulários críticos. Aegis segue esse padrão com `SUPER_ADMIN` (plataforma) vs papéis de produto.

## Objetivo

1. Restringir a navegação do `SUPER_ADMIN` a rotas de plataforma.
2. Adicionar `DELETE` e `restore` no ciclo de vida de usuário (UI + serviço).
3. Diferenciar visualmente usuários `removidos` de `bloqueados`.
4. Adicionar validação inline nos formulários críticos.

## Tarefas

### A. Navegação do SUPER_ADMIN — `core/permissions/roles.ts`

Adicionar `roleBlockedRoutePrefixes["super_admin"]`:

```ts
export const roleBlockedRoutePrefixes: Record<string, string[]> = {
  super_admin: ["/content", "/pages", "/assets", "/forms", "/analytics", "/knowledge"],
  // ... outros papéis existentes
};
```

Atualizar `roleVisibleNav["super_admin"]` para incluir apenas:

```ts
roleVisibleNav["super_admin"] = [
  "/dashboard",
  "/products",        // metadados: nome, tipo, status, módulos — sem "Entrar no produto"
  "/settings",
  "/audit",
];
```

Em `ProductCard.tsx` (ou equivalente na listagem de produtos): para `SUPER_ADMIN`, substituir o botão "Entrar no produto" por "Gerenciar" — que abre a aba de módulos/usuários/configurações do produto, sem entrar no fluxo de content/pages/assets.

> **Nota importante:** esta restrição é de UI/UX. O backend enforça a restrição real via `ProductAccessResolver` (ADR-0018). Se o SUPER_ADMIN tentar acessar diretamente `/content/...` com um `productId` em que não tem `ProductAssignment`, o backend retorna `403 PRODUCT_CONTENT_ACCESS_DENIED`. O frontend deve tratar esse 403 exibindo uma mensagem de acesso negado (não um 404 genérico), porque o SUPER_ADMIN sabe que o produto existe.

### B. Serviço de usuários — `core/users/services/usersService.ts`

Adicionar os métodos novos:

```ts
// Soft delete — remove usuário do tenant (ADR-0020)
export const removeUser = async (tenantId: string, userId: string): Promise<void> => {
  await api.delete(`/tenants/${tenantId}/users/${userId}`);
};

// Restauração de usuário removido ou bloqueado (ADR-0020)
export const restoreUser = async (tenantId: string, userId: string): Promise<UserSummary> => {
  const { data } = await api.post(`/tenants/${tenantId}/users/${userId}/restore`);
  return data;
};
```

Adicionar `"removido"` como valor válido de `status` em `UserSummary` (e no tipo correspondente em `core/users/types/user.types.ts`):

```ts
export type UserStatus = "ativo" | "bloqueado" | "convidado" | "removido";
```

Enquanto o backend não estiver disponível, adicionar mock para esses dois endpoints em `usersService.mock.ts` (ou equivalente), retornando os estados esperados: `removeUser` → `void`; `restoreUser` → `UserSummary` com `status: "ativo"`.

### C. `UserDetailPanel.tsx` — ações de remoção e restauração

Adicionar:

- Botão "Remover do tenant" (estilo destrutivo, visível apenas quando `user.status !== "removido"`):
  - Abre um modal de confirmação com o nome do usuário
  - Ao confirmar, chama `removeUser(tenantId, userId)`
  - Atualiza o estado local; exibe toast "Usuário removido. Você pode restaurar o acesso a qualquer momento."
  - Usuário bloqueado também pode ser removido (remoção é mais definitiva que bloqueio)

- Botão "Restaurar acesso" (visível apenas quando `user.status === "removido"` ou `user.status === "bloqueado"`):
  - Sem modal de confirmação — ação não destrutiva
  - Chama `restoreUser(tenantId, userId)`
  - Exibe toast: "Acesso restaurado. Um e-mail de redefinição de senha foi enviado ao usuário."

- Regra de "não se trancar para fora": desabilitar o botão "Remover do tenant" e "Bloquear" quando `user.id === currentUser.id` **ou** quando o usuário é o único `TENANT_ADMIN`/`SUPER_ADMIN` ativo no tenant. Tooltip explicativo: "Não é possível remover o último administrador ativo."

### D. `UserTable.tsx` — status visual e filtro

**Status `"removido"`**: adicionar badge/chip visual distinto de `"bloqueado"`:

| Status | Cor sugerida | Ícone |
|---|---|---|
| `ativo` | verde | — |
| `convidado` | azul/amarelo | envelope |
| `bloqueado` | laranja | lock |
| `removido` | vermelho/cinza | user-x |

**Filtro de listagem por papel (ADR-0019)**: o filtro client-side atual não distingue o escopo do `PRODUCT_MANAGER`. Com o backend real ativo (Sprint de toggle mock↔real), a listagem já virá filtrada pelo backend — nenhum filtro client-side adicional é necessário. No modo mock, o `usersService.mock.ts` deve simular o filtro: usuários que retornam para `PRODUCT_MANAGER` são apenas os que têm ao menos um `ProductAssignment` em comum com o caller simulado. Documentar esse comportamento com um comentário no mock para facilitar a Sprint de toggle.

### E. Validação inline nos formulários críticos

Implementar validação de campo antes da submissão (sem precisar aguardar resposta da API). Usar a biblioteca de validação já presente no projeto (verificar se é `react-hook-form`, `yup`, `zod`, ou validação manual — adaptar ao padrão existente).

**`InviteUserDrawer.tsx`** (ou equivalente):
- `email`: obrigatório, formato válido (`RFC 5322` básico)
- `name`: obrigatório, mínimo 2 caracteres
- `role`: obrigatório, deve ser um dos papéis válidos do catálogo
- `allowedProducts`: opcional, mas se preenchido deve corresponder a produto existente (validar no blur)

**Criação de tenant** (`CreateTenantModal.tsx` ou equivalente):
- `name`: obrigatório, 3-100 caracteres
- `slug`/`key`: obrigatório, apenas letras minúsculas, números e hífens (`/^[a-z0-9-]+$/`), 3-50 caracteres; mostrar preview em tempo real do slug gerado a partir do nome
- `plan`: obrigatório
- `initialAdminEmail`: obrigatório, formato de e-mail válido

**Criação de produto** (`CreateProductForm.tsx`/`CreateProductModal.tsx`):
- `name`: obrigatório, 3-100 caracteres
- `key`: obrigatório, padrão slug (`/^[a-z0-9-]+$/`), único por tenant (validar via API no blur — `GET /products?key=...` ou equivalente)
- `type`: obrigatório, um dos 7 tipos do catálogo (ADR-0017)
- `defaultLocale`: obrigatório

**Atribuição de produto** (`AssignProductUserModal.tsx` ou equivalente):
- `userId` XOR `inviteEmail` obrigatório — validar que exatamente um dos dois está preenchido; se ambos ou nenhum: mensagem de erro clara
- `inviteEmail`: formato válido se preenchido
- `role`: obrigatório

Para todos: erro inline abaixo do campo (não só toast), com a mensagem em português. Botão de submit desabilitado enquanto houver erros de validação.

**Paridade com backend (ADR-0019)**: as validações de formato/obrigatoriedade listadas acima espelham as anotações Jakarta Validation que o backend aplica. Não é necessário validar regras de negócio (e-mail duplicado, `key` único) com a mesma profundidade — essas continuam sendo validadas pelo backend (API retorna 409/400), e o frontend trata o erro exibindo a mensagem do backend abaixo do campo correspondente.

### F. Tratamento do 403 `PRODUCT_CONTENT_ACCESS_DENIED`

Em `api/http-client.ts` (ou equivalente onde interceptors de resposta são configurados), adicionar tratamento para `403` com `body.error === "PRODUCT_CONTENT_ACCESS_DENIED"`:

```ts
if (error.response?.status === 403 && error.response?.data?.error === "PRODUCT_CONTENT_ACCESS_DENIED") {
  // Não redirecionar para /403 genérico — exibir mensagem contextual
  toast.error("Você não tem acesso ao conteúdo deste produto. Solicite atribuição ao administrador.");
  return Promise.reject(error);
}
```

Este caso é diferente de um 403 genérico (sem permissão de papel) — o erro indica que o SUPER_ADMIN sabe que o produto existe mas não tem `ProductAssignment` para ele.

## Critérios de aceite

- [ ] SUPER_ADMIN não vê `/content`, `/pages`, `/assets`, `/forms`, `/analytics`, `/knowledge` na navbar.
- [ ] SUPER_ADMIN não consegue navegar manualmente para essas rotas (bloqueio por `roleBlockedRoutePrefixes`).
- [ ] Na listagem de produtos como SUPER_ADMIN, o botão "Entrar no produto" está substituído por "Gerenciar".
- [ ] `DELETE /tenants/{tenantId}/users/{userId}` (mock/real) funciona e atualiza o status do usuário para `"removido"` na UI.
- [ ] `POST .../restore` funciona e atualiza o status do usuário para `"ativo"` com toast de confirmação.
- [ ] Botão "Remover do tenant" mostra modal de confirmação antes de agir.
- [ ] Botão "Remover" é desabilitado quando o usuário é o único TENANT_ADMIN/SUPER_ADMIN ativo.
- [ ] Usuário com status `"removido"` exibe badge visual distinto de `"bloqueado"`.
- [ ] Formulário de convite exibe erro inline de e-mail inválido antes de submeter.
- [ ] Formulário de criação de tenant exibe erro inline de `key` inválida e preview do slug.
- [ ] Formulário de criação de produto valida `key` com padrão slug e tipo obrigatório.
- [ ] Formulário de atribuição de produto valida `userId` XOR `inviteEmail`.
- [ ] 403 `PRODUCT_CONTENT_ACCESS_DENIED` exibe toast contextual (não redirecionamento genérico).

## Branch sugerida

```bash
git checkout -b sprint/19-super-admin-escopo-e-validacoes
```

## Commit sugerido

```bash
git add src/
git commit -m "feat(frontend): escopo super_admin, ciclo de vida de usuario e validacoes inline"
```
