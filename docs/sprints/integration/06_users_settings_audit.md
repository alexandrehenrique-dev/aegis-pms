# Sprint de Integração 06 — Users, Settings e Audit

> **Pré-requisito:** Sprint 03 concluída (tenants e produtos reais no contexto).
>
> **Foco:** gestão de usuários (onde `email` substitui `userId` nos paths — crítico para bloquear/restaurar usuários), settings por produto e log de auditoria.
>
> **Branch:** `integration/06-users-settings-audit`

---

## A. `usersService.ts` — `email` vs `userId` nos paths

### A.1 — Diagnóstico

Todos os métodos de ação do `usersService` usam `email` nos paths (`/tenants/{id}/users/${email}/block`, etc.). O backend espera o **Keycloak subject** (`userId` — string UUID-like que o Keycloak usa como `sub` no JWT), não o email.

Isso acontece porque o mock foi construído com email como identificador principal dos usuários. O backend real (etapa 07/15) usa `subject` (string do Keycloak) como `userId`.

### A.2 — Impacto

| Operação | Path atual (errado) | Path correto |
|---|---|---|
| `resendInvite` | `/tenants/{id}/users/${email}/resend-invite` | `/tenants/{id}/users/${userId}/resend-invite` |
| `blockUser` | `/tenants/{id}/users/${email}/block` | `/tenants/{id}/users/${userId}/block` |
| `removeUser` | `/tenants/{id}/users/${email}` | `/tenants/{id}/users/${userId}` |
| `restoreUser` | `/tenants/{id}/users/${email}/restore` | `/tenants/{id}/users/${userId}/restore` |

### A.3 — Solução

A resposta de `GET /api/v1/tenants/{id}/users` deve incluir `userId` (subject Keycloak). Atualizar `UserSummary`:

```ts
// frontend/src/domains/users/contracts/responses.ts
export type UserSummary = {
  userId: string;    // Keycloak subject — usar nos paths de ação
  name: string;
  email: string;
  role: string;
  // ...
};
```

Atualizar os métodos para usar `userId` ao invés de `email`:

```ts
async blockUser(userId: string, tenantId: string): Promise<void> {
  if (IS_API_MODE) return apiClient.post(`/tenants/${tenantId}/users/${userId}/block`);
  // mock: encontrar por userId
  const u = usersStore.find(x => x.userId === userId);
  if (u) u.status = 'bloqueado';
}

async removeUser(userId: string, tenantId: string): Promise<void> {
  if (IS_API_MODE) return apiClient.delete(`/tenants/${tenantId}/users/${userId}`);
  const u = usersStore.find(x => x.userId === userId);
  if (u) u.status = 'removido';
}

async restoreUser(userId: string, tenantId: string): Promise<UserSummary> {
  if (IS_API_MODE) return apiClient.post<UserSummary>(`/tenants/${tenantId}/users/${userId}/restore`);
  const u = usersStore.find(x => x.userId === userId);
  if (!u) throw { status: 404 };
  u.status = 'ativo';
  return u;
}

async resendInvite(userId: string, tenantId: string): Promise<void> {
  if (IS_API_MODE) return apiClient.post(`/tenants/${tenantId}/users/${userId}/resend-invite`);
  const u = usersStore.find(x => x.userId === userId);
  if (u) u.inviteStatus = 'pendente';
}
```

### A.4 — `isLastActiveAdmin` — buscar do backend

A lógica mock de `isLastActiveAdmin` consulta `usersStore` local. Em modo API, este endpoint não existe no backend. Duas opções:

**Opção A (recomendada):** Implementar no backend (endpoint simples: `GET /tenants/{id}/users/admin-count`). Registrar como retrofit pendente e manter verificação client-side em modo mock.

**Opção B:** Fazer a verificação client-side em modo API também — a lista de usuários já foi carregada, contar os admins ativos na lista local.

### A.5 — Mock de usuários — adicionar `userId`

O mock `usersRows` precisa incluir um `userId` fake (ex.: `"kc-subj-001"`) para os testes em modo mock continuarem funcionando.

---

## B. `settingsService.ts` — paths do sistema global

Os métodos de settings globais (`listSettingCards`, `restoreDefaultRoles`, `createRole`, `restoreDefaultPermissions`, `savePermissions`, `generateAccessPreview`) apontam para `/settings/*`. Verificar se esses endpoints existem no backend e, se não existem, registrar como retrofits pendentes e manter mock.

**O que provavelmente existe no backend:**
- `GET /products/{id}/settings/security` → ✅ já implementado (etapa 19)
- `PUT /products/{id}/settings/security` → ✅ já implementado (etapa 19)

**O que provavelmente NÃO existe no backend ainda:**
- `GET /settings/cards` → manter mock
- `POST /settings/roles/restore-defaults` → manter mock
- `POST /settings/roles` → manter mock
- `PUT /settings/permissions` → manter mock
- `POST /settings/permissions/preview` → manter mock

Para os que não existem, adicionar guard explícito:

```ts
async restoreDefaultRoles(): Promise<void> {
  if (IS_API_MODE) {
    console.warn('[settingsService] endpoint /settings/roles/restore-defaults não implementado no backend — ignorando em modo API');
    return;
  }
  logApiCall('POST', '/api/v1/admin/roles/restore-defaults');
}
```

### B.1 — `getProductSettings` / `updateProductSettings` — já corretos

Estes dois métodos já estão corretos: `/products/${productId}/settings/security`. Verificar apenas que recebem o UUID e não o slug.

---

## C. `auditService.ts` — corrigir paths e remover POST

### C.1 — `listEvents()` — path errado

`GET /audit/events` não existe. O backend tem `GET /api/v1/tenants/{tenantId}/audit-events`.

```ts
async listEvents(tenantId: string): Promise<ListAuditEventsResponse> {
  if (IS_API_MODE) return apiClient.get<ListAuditEventsResponse>(`/tenants/${tenantId}/audit-events`);
  return auditStore;
}
```

Atualizar chamadores para passar `effectiveTenant.id`.

### C.2 — `recordEvent()` — remover (frontend nunca cria eventos de auditoria)

O backend cria eventos de auditoria automaticamente (etapa 30, Seção D.1-D.3 — via `AuditAspect` e interceptors). O frontend **nunca** deve criar eventos de auditoria manualmente.

```ts
// REMOVER completamente:
async recordEvent(event: Omit<AuditEvent, 'time'>): Promise<void> { ... }
```

Se a tela de auditoria tem um botão "Criar evento manualmente", remover o botão. Se havia propósito de teste, substituir por um comentário no código.

### C.3 — Mapeamento de `AuditEvent`

O backend retorna `AuditEvent` com campos como `actorSubject`, `action`, `targetType`, `targetId`, `timestamp` (ISO string), `traceId`. O frontend usa `actor`, `action`, `target`, `tenant`, `module`, `time`, `risk`. Criar mapper:

```ts
export function mapAuditEvent(dto: AuditEventDto): AuditEvent {
  return {
    actor: dto.actorSubject,
    action: dto.action,
    target: `${dto.targetType}:${dto.targetId}`,
    tenant: dto.tenantId,
    module: dto.module ?? '—',
    time: new Date(dto.timestamp).toLocaleTimeString('pt-BR'),
    risk: dto.risk ?? 'baixo',
  };
}
```

---

## D. `productAssignmentsService.ts` — path `/assignments` → `/users`

```ts
// ANTES:
async listAssignments(productId: string) { return apiClient.get(`/products/${productId}/assignments`); }
async assignUser(productId: string, ...) { return apiClient.post(`/products/${productId}/assignments`, ...); }

// DEPOIS:
async listAssignments(productId: string) { return apiClient.get(`/products/${productId}/users`); }
async assignUser(productId: string, ...) { return apiClient.post(`/products/${productId}/users`, ...); }
```

---

## E. Critérios de aceite

- [ ] `GET /api/v1/tenants/{UUID}/users` retorna lista de usuários reais do tenant.
- [ ] `POST /api/v1/tenants/{UUID}/users/invite` convida um usuário — aparece na lista como "pendente".
- [ ] `POST /api/v1/tenants/{UUID}/users/{userId}/block` bloqueia o usuário — status muda na lista.
- [ ] `DELETE /api/v1/tenants/{UUID}/users/{userId}` remove o usuário — desaparece da lista.
- [ ] Nenhuma chamada usa email no path onde deveria ser userId.
- [ ] `GET /api/v1/tenants/{UUID}/audit-events` retorna eventos reais — tela de auditoria exibe.
- [ ] `POST /api/v1/audit/events` não existe mais no `auditService` frontend.
- [ ] `GET /api/v1/products/{UUID}/settings/security` retorna configurações de segurança do produto.
- [ ] `PUT /api/v1/products/{UUID}/settings/security` salva configuração de Telegram — `botTokenMasked` aparece mascarado na UI após salvar.
- [ ] Modo mock não regrediu.
- [ ] `npm run typecheck` — zero erros.

---

## F. Commit sugerido

```bash
git add frontend/src/domains/users/ frontend/src/domains/settings/ frontend/src/domains/audit/
git commit -m "feat(integration): users (userId real), settings e audit integrados ao backend"
```
