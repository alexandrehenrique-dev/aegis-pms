# Sprint 21 — Ativação de convite e reset de senha reais (conectar ao backend da etapa 28)

> **Pré-requisito:** etapa 28 do backend (`28_tokens_de_ativacao_e_reset_de_senha.md`) concluída e endpoints disponíveis em `http://localhost:8080`.
>
> **Motivação:** `InviteScreen.tsx`, `ResetPasswordScreen.tsx` e `ForgotPasswordScreen.tsx` foram implementados na Sprint 10 (UI completa, incluindo estados de token inválido/expirado/já usado). Estão 100% mock — nenhum chama o backend. Esta sprint troca os mocks por chamadas reais aos endpoints da etapa 28, sem modificar a UX existente.
>
> **Branch:** `sprint/21-ativacao-e-reset-real`

## A. O que já existe (não tocar)

| Arquivo | Status |
|---|---|
| `src/app/screens/auth/InviteScreen.tsx` | UI completa — estados: `valid`, `expired`, `revoked`, `used`, `loading` |
| `src/app/screens/auth/ResetPasswordScreen.tsx` | UI completa — estados: `valid`, `invalid`, `expired`, `loading` |
| `src/app/screens/auth/ForgotPasswordScreen.tsx` | UI completa — rate limiting manual (3 tentativas no mock), navega para `/forgot-password/sent` |
| `src/app/screens/auth/ForgotPasswordSentScreen.tsx` | Tela de confirmação — sem mudanças |
| `src/app/routes/index.tsx` | Rotas `/invite`, `/reset-password`, `/forgot-password`, `/forgot-password/sent` |
| `src/app/screens/auth/LoginScreen.tsx` | Link "Ativar convite" → `/invite`; "Esqueci minha senha" → `/forgot-password` |

**Regra:** nenhuma dessas telas muda de UX. A única mudança é trocar `setTimeout` e estado hardcoded por chamadas reais.

## B. Serviços a criar

Criar `src/domains/auth/services/authActivationService.ts`:

```ts
import api from '@/infra/api'; // instância axios já configurada

// B.1 — Validar token de convite (carrega dados do InviteScreen)
export async function validateInviteToken(token: string): Promise<InviteTokenData> {
  const { data } = await api.post('/auth/invite/validate', { token });
  return data;
}

// B.2 — Ativar conta (submit do InviteScreen)
export async function activateAccount(token: string, password: string): Promise<void> {
  await api.post('/auth/activate', { token, password });
}

// B.3 — Solicitar reset de senha (submit do ForgotPasswordScreen)
export async function requestPasswordReset(email: string): Promise<void> {
  await api.post('/auth/reset-password/request', { email });
}

// B.4 — Confirmar reset de senha (submit do ResetPasswordScreen)
export async function confirmPasswordReset(token: string, password: string): Promise<void> {
  await api.post('/auth/reset-password/confirm', { token, password });
}

// Tipos
export type InviteTokenData = {
  userName: string;
  userEmail: string;
  tenantName: string;
  productNames: string[];
  role: string;
  inviterName: string;
  expiresAt: string;
};

export type TokenError = 'TOKEN_NOT_FOUND' | 'TOKEN_EXPIRED' | 'TOKEN_ALREADY_USED';
```

**Mapeamento de erros HTTP → estado de UI:**

| Status | `error` body | Estado da tela |
|---|---|---|
| 404 | `TOKEN_NOT_FOUND` | `"revoked"` (convite) / `"invalid"` (reset) |
| 410 | `TOKEN_EXPIRED` | `"expired"` |
| 400 | `TOKEN_ALREADY_USED` | `"used"` (convite) / `"invalid"` (reset) |
| 422 | `WEAK_PASSWORD` | Inline error na UI (sem mudar estado) |
| 429 | — | Inline rate-limit message (ForgotPasswordScreen) |

## C. `InviteScreen.tsx` — conectar ao backend

O `InviteScreen.tsx` atual simula os estados com `tokenState` prop e `setTimeout`. Substituir por:

```ts
// 1. Ler token da URL
const [searchParams] = useSearchParams();
const token = searchParams.get('token');

// 2. No useEffect de montagem — validar token
useEffect(() => {
  if (!token) { setScreenState('revoked'); return; }
  
  setScreenState('loading');
  validateInviteToken(token)
    .then((data) => {
      setInviteData(data);   // popula userName, tenantName, productNames, role, inviterName
      setScreenState('valid');
    })
    .catch((err) => {
      const code: TokenError = err?.response?.data?.error;
      if (code === 'TOKEN_EXPIRED') setScreenState('expired');
      else if (code === 'TOKEN_ALREADY_USED') setScreenState('used');
      else setScreenState('revoked');
    });
}, [token]);

// 3. No submit do formulário de senha
const handleSubmit = async () => {
  if (!token) return;
  setLoading(true);
  try {
    await activateAccount(token, password);
    // Navegar para /login com toast de sucesso
    navigate('/login', { state: { toast: 'Conta ativada! Faça login para continuar.' } });
  } catch (err) {
    const code = err?.response?.data?.error;
    if (code === 'WEAK_PASSWORD') {
      setPasswordError('A senha deve ter ao menos 8 caracteres, incluindo letras e números.');
    } else if (code === 'TOKEN_EXPIRED') {
      setScreenState('expired');
    } else {
      setPasswordError('Ocorreu um erro. Tente novamente.');
    }
  } finally {
    setLoading(false);
  }
};
```

**Campos do `inviteData` para exibir na UI (já existem placeholders na tela atual):**
- `inviteData.tenantName` → nome do workspace
- `inviteData.productNames.join(', ')` → produtos
- `inviteData.role` → papel
- `inviteData.inviterName` → quem convidou
- `inviteData.userEmail` → e-mail pré-preenchido (somente leitura)

**Toast de sucesso no `LoginScreen`:** verificar se `location.state?.toast` existe e exibir com o componente de notificação já em uso no projeto. Se não existir, criar `useLocationToast()` hook mínimo:

```ts
const location = useLocation();
useEffect(() => {
  if (location.state?.toast) {
    showToast(location.state.toast, 'success');
    // limpar state para não re-exibir no reload
    window.history.replaceState({}, '', location.pathname);
  }
}, []);
```

## D. `ForgotPasswordScreen.tsx` — conectar ao backend

```ts
// Substituir o setTimeout mock por:
const handleSubmit = async (e: FormEvent) => {
  e.preventDefault();
  if (attempts >= 3) return; // rate limit local — já existe na UI
  
  setLoading(true);
  try {
    await requestPasswordReset(email);
    // sempre navegar para /sent — backend retorna 200 mesmo se e-mail não existe (enumeração)
    navigate('/forgot-password/sent');
  } catch (err) {
    if (err?.response?.status === 429) {
      setError('Muitas tentativas. Aguarde alguns minutos antes de tentar novamente.');
    } else {
      // Não revelar se e-mail existe ou não — navegar para /sent mesmo em erro
      navigate('/forgot-password/sent');
    }
  } finally {
    setLoading(false);
  }
};
```

> O rate limiting de 3 tentativas que a UI já faz pode ser mantido como UX extra, mas o backend também limita (3 por 15 min) e retornará 429 se ultrapassado.

## E. `ResetPasswordScreen.tsx` — conectar ao backend

```ts
// 1. Ler token da URL
const [searchParams] = useSearchParams();
const token = searchParams.get('token');

// 2. Validar token na montagem
// ATENÇÃO: não existe um endpoint GET específico para validar o reset token.
// A validação ocorre no submit (POST /auth/reset-password/confirm).
// Para exibir estado "inválido" antes do submit, fazer uma validação lazy:
//   - Se não há token na URL → estado 'invalid' imediatamente
//   - Se há token → exibir formulário (estado 'valid') e lidar com erro no submit

useEffect(() => {
  if (!token) setScreenState('invalid');
}, [token]);

// 3. No submit
const handleSubmit = async () => {
  if (!token) return;
  setLoading(true);
  try {
    await confirmPasswordReset(token, password);
    navigate('/login', { state: { toast: 'Senha redefinida! Faça login para continuar.' } });
  } catch (err) {
    const code = err?.response?.data?.error;
    if (code === 'TOKEN_EXPIRED') setScreenState('expired');
    else if (code === 'WEAK_PASSWORD') setPasswordError('Senha fraca. Use ao menos 8 caracteres, letras e números.');
    else setScreenState('invalid');
  } finally {
    setLoading(false);
  }
};
```

## F. Variáveis de ambiente

Verificar que `VITE_API_BASE_URL` está configurado e que a instância `api` (axios) já usa `baseURL = VITE_API_BASE_URL + '/api/v1'`. Se não estiver, ajustar apenas na instância existente — não duplicar.

```env
# .env.local (dev)
VITE_API_BASE_URL=http://localhost:8080
```

## G. Critérios de aceite

- [ ] `LoginScreen` acessado com `location.state.toast` exibe o toast de sucesso e o limpa após exibição.
- [ ] `/invite` sem `?token=` na URL → exibe estado "convite inválido" (equivalente a `revoked`).
- [ ] `/invite?token=<token-válido>` → carrega dados do convite (nome, tenant, produtos, papel) do backend e exibe na tela.
- [ ] `/invite?token=<token-expirado>` → exibe estado "convite expirado".
- [ ] `/invite?token=<token-usado>` → exibe estado "convite já utilizado".
- [ ] Submit com senha válida em `/invite?token=<token-válido>` → ativa conta via backend, navega para `/login` com toast.
- [ ] Submit com senha fraca → exibe erro inline na tela de ativação, não muda estado.
- [ ] `/forgot-password` → submit com e-mail qualquer → navega para `/forgot-password/sent` (mesmo se não existe — não revelar enumeração).
- [ ] `/reset-password` sem `?token=` → estado "inválido".
- [ ] `/reset-password?token=<token-válido>` → exibe formulário; submit com senha válida → redireciona para `/login` com toast.
- [ ] `/reset-password?token=<token-expirado>` → submit → exibe estado "expirado" (server-side validation).
- [ ] Nenhum `setTimeout` mock permanece em produção (`grep -r "setTimeout" src/app/screens/auth/` → zero resultados nos fluxos de token).
- [ ] `npm run build` sem erros de tipo.

## H. Testes

```ts
// InviteScreen.test.tsx — exemplos de cenários
it('exibe loading enquanto valida token', ...)
it('exibe dados do convite após validação bem-sucedida', ...)
it('exibe estado expirado para TOKEN_EXPIRED', ...)
it('navega para /login com toast após ativação bem-sucedida', ...)
it('exibe erro de senha fraca inline sem mudar estado', ...)

// ForgotPasswordScreen.test.tsx
it('navega para /sent após submit (independente do e-mail existir)', ...)
it('exibe mensagem de rate limit para 429', ...)

// ResetPasswordScreen.test.tsx
it('exibe formulário quando token está na URL', ...)
it('exibe estado inválido quando não há token na URL', ...)
it('navega para /login após reset bem-sucedido', ...)
```

Usar `msw` (Mock Service Worker) para mockar as chamadas HTTP nos testes — não mockar `api` diretamente via jest.mock. Se `msw` ainda não estiver no projeto, adicionar:

```bash
npm install --save-dev msw
```

## I. Commit sugerido

```bash
git add src/domains/auth/ src/app/screens/auth/
git commit -m "feat(frontend): ativacao de convite e reset de senha conectados ao backend real"
```
