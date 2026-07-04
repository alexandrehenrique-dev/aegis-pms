# Sprint de Integração 01 — Autenticação real e remoção de credenciais do bundle

> **Pré-requisito:** etapa 06 do backend (`06_auth_proxy_smtp_e_convite.md`) implementada — endpoints `POST /api/v1/auth/login`, `/refresh`, `/logout` existem e funcionam.
>
> **Bloqueia:** todas as outras sprints de integração. Sem login real nenhuma chamada autenticada pode ser testada de ponta a ponta.
>
> **Problemas críticos que esta sprint resolve:**
> 1. `LoginScreen.tsx` autentica contra `mockUsers` sem verificar `IS_API_MODE` — qualquer pessoa com `admin@byop.io/senha123` "entra" na aplicação em produção.
> 2. `keycloakConfig.ts` usa `=== "real"` enquanto `apiMode.ts` usa `=== "api"` — com `VITE_API_MODE=api` o painel de contas demo **aparece em produção**.
> 3. `mockUsers` com senhas hardcoded são importados incondicionalmente pelo `LoginScreen` — estão no bundle de produção mesmo que a UI os esconda.
> 4. `AuthContext` nunca chama `GET /api/v1/me` — TenantSelect e ProductSelect dependem de dados mockados mesmo em modo API.
> 5. `AuthEnvBadge` renderiza "staging" hardcoded em todas as telas de auth.
>
> **Branch:** `integration/01-autenticacao-real`

---

## A. Corrigir o toggle de modo (root cause do bug crítico nº 2)

### A.1 — Unificar em `IS_API_MODE`

`frontend/src/core/config/keycloakConfig.ts` define `getApiMode()` que retorna `"real"` quando `VITE_API_MODE === "real"`. Porém `apiMode.ts` usa `=== "api"`. Com `VITE_API_MODE=api` (o valor de produção), `getApiMode()` devolve `"mock"` → o painel de contas demo aparece em produção.

**Atenção:** `keycloakConfig.ts` controla o fluxo de autenticação do Keycloak (proxy BFF vs mock). Não alterar a lógica interna — apenas corrigir o valor que ela lê:

```ts
// frontend/src/core/config/keycloakConfig.ts
// ANTES:
export function getApiMode(): ApiMode {
  const raw = import.meta.env.VITE_API_MODE;
  return raw === "real" ? "real" : "mock";
}

// DEPOIS:
export function getApiMode(): ApiMode {
  const raw = import.meta.env.VITE_API_MODE;
  return raw === "api" ? "real" : "mock";  // "api" é o valor canônico de apiMode.ts
}
```

Verificar todos os callers de `getApiMode()` — se algum compara com `"real"`, adaptar.

### A.2 — Atualizar `.env.example`

```env
# Modo de integração: "mock" (dev padrão) ou "api" (integração/prod)
VITE_API_MODE=mock
```

Garantir que `.env.production` (ou o script de build de prod) seta `VITE_API_MODE=api`.

---

## B. Integrar `LoginScreen` com `POST /api/v1/auth/login`

### B.1 — `authService.ts` (novo ou existente)

Criar/atualizar `frontend/src/core/auth/services/authService.ts`:

```ts
import { IS_API_MODE } from '../../../infra/apiMode';
import { apiClient } from '../../../shared/services/apiClient';
import { mockUsers } from '../mocks/users';  // APENAS para modo mock

export type LoginRequest = { username: string; password: string };
export type LoginResponse = { accessToken: string; refreshToken: string; expiresIn: number };

export const authService = {
  async login(req: LoginRequest): Promise<LoginResponse> {
    if (IS_API_MODE) {
      return apiClient.post<LoginResponse>('/auth/login', req);
    }
    // Modo mock: valida contra mockUsers locais
    const user = mockUsers.find(u => u.email === req.username && u.password === req.password);
    if (!user) throw new Error('Credenciais inválidas');
    return { accessToken: `mock-token-${user.id}`, refreshToken: `mock-refresh-${user.id}`, expiresIn: 3600 };
  },

  async refresh(refreshToken: string): Promise<LoginResponse> {
    if (IS_API_MODE) return apiClient.post<LoginResponse>('/auth/refresh', { refreshToken });
    return { accessToken: `mock-token-refreshed`, refreshToken, expiresIn: 3600 };
  },

  async logout(): Promise<void> {
    if (IS_API_MODE) await apiClient.post<void>('/auth/logout', {});
  },
};
```

### B.2 — `LoginScreen.tsx` — substituir autenticação mock direta

Atualmente `handleLogin` (linha ~33) acessa `mockUsers` diretamente, sem `IS_API_MODE`. Substituir:

```ts
// LoginScreen.tsx — handleLogin
const handleLogin = async () => {
  setLoading(true);
  try {
    const { accessToken, refreshToken } = await authService.login({ username: email, password });
    // Armazenar tokens e avançar para /me
    await authContext.initSession(accessToken, refreshToken);
  } catch (err) {
    setError('Credenciais inválidas');
  } finally {
    setLoading(false);
  }
};
```

### B.3 — Esconder painel de contas demo em produção

O painel de contas de demonstração (`LoginScreen.tsx` linhas ~85-95) deve aparecer APENAS em dev:

```tsx
{import.meta.env.DEV && (
  <DemoAccountsPanel accounts={mockUsers} onSelect={(u) => { setEmail(u.email); setPassword(u.password); }} />
)}
```

`import.meta.env.DEV` é `true` em `vite dev` e `false` em `vite build` — o painel some do bundle de produção automaticamente.

---

## C. `mockUsers` — isolar do bundle de produção

`frontend/src/core/auth/mocks/users.ts` contém senhas hardcoded. Atualmente é importado diretamente pelo `LoginScreen`. Com a mudança B.3, o import fica dentro do bloco `import.meta.env.DEV` — o Vite fará tree-shaking e o arquivo não entra no bundle de prod.

Confirmar com:
```bash
cd frontend && npm run build
# O arquivo dist/assets/index-*.js não deve conter "senha123" ou "admin@byop.io"
grep -r "senha123\|admin@byop" dist/assets/ && echo "FALHOU" || echo "OK"
```

---

## D. `AuthContext` — integrar `GET /api/v1/me`

Atualmente `AuthContext` recebe dados de tenants/produtos do login mock. Em modo API, após receber o `accessToken`:

### D.1 — `meService.ts`

```ts
// frontend/src/core/auth/services/meService.ts
import { IS_API_MODE } from '../../../infra/apiMode';
import { apiClient } from '../../../shared/services/apiClient';

export type MeResponse = {
  subject: string;
  name: string;
  email: string;
  roles: string[];  // ex: ["AEGIS_SUPER_ADMIN"]
  tenants: Array<{ id: string; key: string; name: string; plan: string; role: string }>;
};

export const meService = {
  async getMe(): Promise<MeResponse> {
    if (IS_API_MODE) return apiClient.get<MeResponse>('/me');
    // Modo mock: inferir do token mock
    const stored = sessionStorage.getItem('mock-user');
    return stored ? JSON.parse(stored) : Promise.reject(new Error('Não autenticado'));
  },
};
```

### D.2 — `authContext.initSession(accessToken, refreshToken)`

```ts
async initSession(accessToken: string, refreshToken: string) {
  // Salvar tokens
  sessionStorage.setItem('access_token', accessToken);
  sessionStorage.setItem('refresh_token', refreshToken);

  // Configurar o apiClient para usar o token
  apiClient.setAuthTokenProvider(() => sessionStorage.getItem('access_token') ?? '');

  if (IS_API_MODE) {
    // Buscar dados reais do usuário
    const me = await meService.getMe();
    setAuthUser({ subject: me.subject, name: me.name, email: me.email, roles: mapRoles(me.roles) });
    setAvailableTenants(me.tenants);
  }
  // Modo mock: dados já estavam no mockUsers, não precisa de /me
}
```

### D.3 — `mapRoles` — converter roles Keycloak para roles internas

```ts
// frontend/src/core/auth/utils/roleMapper.ts
const ROLE_MAP: Record<string, UserRole> = {
  AEGIS_SUPER_ADMIN:      'super_admin',
  AEGIS_TENANT_ADMIN:     'tenant_admin',
  AEGIS_PRODUCT_MANAGER:  'product_manager',
  AEGIS_EDITOR:           'editor',
  AEGIS_VIEWER:           'viewer',
};

export function mapRoles(keycloakRoles: string[]): UserRole[] {
  return keycloakRoles.flatMap(r => ROLE_MAP[r] ? [ROLE_MAP[r]] : []);
}
```

### D.4 — `apiClient.ts` — token provider

`frontend/src/shared/services/apiClient.ts` já tem `setAuthTokenProvider` implementado (etapa 20). Verificar que está sendo chamado antes de qualquer request e que injeta `Authorization: Bearer <token>`:

```ts
// Garantir que o header é injetado em TODA chamada, não só quando IS_API_MODE
headers['Authorization'] = `Bearer ${tokenProvider()}`;
```

---

## E. Refresh automático de token

Interceptar respostas 401 do `apiClient`, tentar refresh, e retentar a chamada original:

```ts
// apiClient.ts — após receber resposta
if (response.status === 401 && !isRetry) {
  const newTokens = await authService.refresh(sessionStorage.getItem('refresh_token') ?? '');
  sessionStorage.setItem('access_token', newTokens.accessToken);
  sessionStorage.setItem('refresh_token', newTokens.refreshToken);
  return fetch(url, { ...options, headers: { ...options.headers, Authorization: `Bearer ${newTokens.accessToken}` } });
}
if (response.status === 401) {
  // Refresh também falhou — redirecionar para login
  sessionStorage.clear();
  window.location.href = '/login';
}
```

---

## F. `TenantSelectScreen` e `ProductSelectScreen` — alimentar via `/me`

Atualmente as telas de seleção dependem de `mockTenantsByUser`/`mockProductsByUser` de `AuthContext`. Após D.2, `availableTenants` virá do `/me` em modo API. Verificar que as telas consomem `availableTenants` do context (não importam diretamente dos mocks).

Se o `/me` não retornar produtos (só tenants), a `ProductSelectScreen` precisa chamar `GET /api/v1/products` com o tenant selecionado — ver Sprint 03.

---

## G. `AuthEnvBadge` — remover "staging" hardcoded

`frontend/src/core/auth/components/AuthChrome.tsx` (linhas ~28-33) renderiza "staging" fixo. Substituir por badge baseado em `import.meta.env`:

```tsx
function AuthEnvBadge() {
  if (import.meta.env.PROD) return null;                          // sem badge em prod
  const label = import.meta.env.VITE_API_MODE === 'api' ? 'api' : 'mock';
  return <span className="env-badge">{label}</span>;
}
```

---

## H. Logout real

`handleLogout` em `AuthContext` ou botão de logout deve chamar `authService.logout()` antes de limpar a sessão:

```ts
async logout() {
  try { await authService.logout(); } catch { /* ignora — limpa local de qualquer forma */ }
  sessionStorage.clear();
  setAuthUser(null);
  navigate('/login');
}
```

---

## I. `authActivationService` — verificação de modo

`frontend/src/core/auth/services/authActivationService.ts` SEMPRE chama a API real (sem verificar `IS_API_MODE`). Em dev mock isso quebra. Adicionar:

```ts
async validateInviteToken(token: string) {
  if (!IS_API_MODE) return { valid: true, email: 'dev@mock.local', tenantName: 'Dev Tenant' };
  return apiClient.get<InviteTokenData>(`/auth/invite/validate?token=${token}`);
},
```

---

## J. Critérios de aceite

- [ ] `npm run build` — `dist/assets/index-*.js` não contém `"senha123"` nem `"admin@byop.io"`.
- [ ] Com `VITE_API_MODE=api`: painel de contas demo **não aparece** na tela de login.
- [ ] Com `VITE_API_MODE=mock`: painel de contas demo **aparece** normalmente.
- [ ] `POST /api/v1/auth/login` com credenciais reais do Keycloak → token retornado → `apiClient` injeta o token → `GET /api/v1/me` retorna 200.
- [ ] `GET /api/v1/me` alimenta `AuthContext` — tenants disponíveis vêm do backend, não dos mocks.
- [ ] Após 401, token é renovado automaticamente via `/auth/refresh` sem o usuário perceber.
- [ ] Logout chama `POST /api/v1/auth/logout` e limpa sessão.
- [ ] `AuthEnvBadge` não aparece em `PROD=true`.
- [ ] `authActivationService` funciona em modo mock (mock local) e em modo API (chamada real).
- [ ] Modo mock completo (`VITE_API_MODE=mock`) continua funcionando exatamente como antes — sem regressão.

---

## K. Commit sugerido

```bash
git add frontend/src/core/auth/ frontend/src/shared/services/apiClient.ts frontend/src/infra/
git commit -m "feat(integration): autenticacao real via proxy BFF, remocao de credenciais do bundle de prod"
```
