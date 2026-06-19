# Sprint 09 — Autenticação e Entrada na Plataforma

## Context

O Aegis PMS tem 50+ telas implementadas via máquina de estados local (`useState<Screen>`), mas abre diretamente no dashboard global sem nenhuma barreira de autenticação. Não existe login, recuperação de senha, seleção de tenant nem gerenciamento de sessão. O React Router está instalado mas não é utilizado — a reorganização de navegação com rotas URL-based está reservada para a Sprint 10. Esta sprint foca exclusivamente em criar a camada de autenticação e os fluxos de entrada na plataforma, mantendo coerência com a arquitetura de estado atual e a identidade visual Aegis (white-first, premium, inspirada em Linear/Stripe).

---

## Approach

Manter a máquina de estados existente e **adicionar as telas de auth como novos valores no type `Screen`**. Criar um contexto React (`AuthContext`) para gerenciar o usuário autenticado, tenant ativo e permissões. O app inicializa em `"login"` e só avança para `"tenantSelect"` → `"global"` após autenticação bem-sucedida. Sprint 10 depois reorganizará rotas URL; aqui o estado é suficiente e consistente.

---

## Screens a Implementar

### Novos valores no `type Screen`:
```
"login" | "forgotPassword" | "forgotPasswordSent" | "setPassword" | "tenantSelect"
```

### Componentes a criar em `src/app/components/`:

| Arquivo | Propósito |
|---|---|
| `auth/AuthLayout.tsx` | Wrapper split-screen (painel esquerdo Aegis brand + direito formulário) |
| `auth/LoginScreen.tsx` | Email + senha + loading state + erro inline |
| `auth/ForgotPasswordScreen.tsx` | Input de email para recuperação |
| `auth/ForgotPasswordSentScreen.tsx` | Confirmação "verifique seu e-mail" |
| `auth/SetPasswordScreen.tsx` | Nova senha (para reset e convites) |
| `auth/TenantSelectScreen.tsx` | Cards de tenants disponíveis pós-login |

---

## Auth State

Adicionar em `App.tsx` (ou arquivo separado `src/app/context/AuthContext.tsx`):

```ts
type AuthUser = {
  id: string;
  name: string;
  email: string;
  role: 'super_admin' | 'tenant_admin' | 'product_manager' | 'editor' | 'viewer';
  avatar: string;
}

type AuthState = {
  isAuthenticated: boolean;
  user: AuthUser | null;
  currentTenant: Tenant | null;
}
```

**Mock users para o protótipo:**
- `admin@byop.io` / `senha123` → Super Admin → acesso a todos os tenants
- `editor@byop.io` / `senha123` → Editor → apenas BYOP
- `viewer@byop.io` / `senha123` → Viewer → apenas BYOP

Inicialização do app: `currentScreen = "login"` (em vez de `"global"`)

---

## Design Spec

### AuthLayout (split-screen)
- **Painel esquerdo** (40% width, desktop only, hidden mobile): fundo `#0F3D2E` (Aegis green), logo Aegis + tagline "Product OS", quote inspiracional ou preview da plataforma, rodapé com versão
- **Painel direito** (60% desktop, 100% mobile): fundo `#FAFAF8`, formulário centralizado com max-width 400px

### LoginScreen
- Logo Aegis no topo (mobile only — desktop logo fica no painel esquerdo)
- Título: "Entrar na plataforma"
- Subtítulo: "Acesse sua conta para continuar"
- Campo Email (type="email", autoFocus, validação inline)
- Campo Senha (type="password", toggle de visibilidade com ícone Eye)
- Link "Esqueci minha senha" → `go("forgotPassword")`
- Botão primário "Entrar" com estado "Entrando..." durante submit (200ms minimum display)
- Erro inline para credenciais inválidas: "E-mail ou senha incorretos. Tente novamente."
- Motion: fade-in da tela (200ms), shake no formulário em caso de erro

### ForgotPasswordScreen
- Título: "Recuperar acesso"
- Instrução: "Informe seu e-mail e enviaremos um link de recuperação."
- Campo email
- Botão "Enviar link"
- Link voltar para login
- Submissão vai para `go("forgotPasswordSent")`

### ForgotPasswordSentScreen
- Ícone de envelope (MailCheck do lucide-react)
- Título: "Verifique seu e-mail"
- Texto: "Enviamos um link de recuperação para [email]. Se não encontrar, confira sua caixa de spam."
- Botão "Voltar ao login"

### SetPasswordScreen
- Título: "Definir nova senha"
- Campo nova senha + confirmar senha
- Indicador de força de senha (weak/medium/strong via barra colorida)
- Botão "Salvar senha" → após sucesso, `go("login")` com toast "Senha definida com sucesso"

### TenantSelectScreen
- Tela cheia sem sidebar (pré-app-shell)
- Header simples com avatar do usuário + nome + botão logout
- Título: "Selecione seu espaço de trabalho"
- Grid de cards de tenants (nome, plano, número de produtos, logo/inicial)
- Card hover com borda Aegis green
- Clique → define `currentTenant` no auth state → `go("global")`
- Usuários com acesso a um único tenant: skip automático direto para `go("global")`

---

## Fluxo Completo

```
App inicia → go("login")
  ↓ submit correto
go("tenantSelect")
  ↓ tenant selecionado (ou único tenant → auto)
go("global") → AppShell normal

Login → "Esqueci minha senha" → go("forgotPassword")
  ↓ submit email
go("forgotPasswordSent")
  → botão volta → go("login")

Logout (header) → limpa auth state → go("login")
```

---

## Mudanças em `App.tsx`

1. **Adicionar ao `type Screen`**: `"login" | "forgotPassword" | "forgotPasswordSent" | "setPassword" | "tenantSelect"`
2. **Adicionar auth state**: `const [authState, setAuthState] = useState<AuthState>({ isAuthenticated: false, user: null, currentTenant: null })`
3. **Inicializar**: `const [currentScreen, setCurrentScreen] = useState<Screen>("login")`
4. **Lógica de render**: Se `!authState.isAuthenticated` E screen não é auth → redireciona para login (proteção de rotas via estado)
5. **Login mock**: Comparar credenciais com array de mockUsers, setar authState, chamar `go("tenantSelect")`
6. **Header**: Exibir nome do usuário autenticado e tenant ativo; botão de logout
7. **AppShell**: Só renderizar se `authState.isAuthenticated && authState.currentTenant !== null`

---

## Reuse de Código Existente

- Usar `Button` de `src/app/components/ui/button.tsx` para todos os CTAs
- Usar `Input` de `src/app/components/ui/input.tsx` para campos de formulário
- Usar `Label` de `src/app/components/ui/label.tsx`
- Usar `Badge` para indicar role do usuário no TenantSelectScreen
- Usar `motion` de `motion/react` para animações de entrada (já em uso no App.tsx)
- Usar `toast` de `sonner` para feedback de ações (já em uso)
- Usar `lucide-react` para ícones: `Eye`, `EyeOff`, `Mail`, `MailCheck`, `Lock`, `LogOut`, `Building2`, `ChevronRight`
- Reaproveitar o `Tenant` type e array `tenants` já definidos em App.tsx

---

## Não está no escopo desta sprint

- React Router / URL routing (Sprint 10)
- RBAC enforcement nas telas de produto (Sprint 13)
- Fluxo de convite por e-mail (Sprint 13)
- OAuth / SSO / providers externos
- Persistência de sessão (localStorage/cookie) além do estado React

---

## Verification

1. App deve abrir na tela de Login (não no dashboard)
2. Credenciais inválidas mostram erro inline sem navegar
3. Login com `admin@byop.io/senha123` → TenantSelect → selecionar BYOP → Dashboard Global
4. Login com `editor@byop.io/senha123` → skip TenantSelect (único tenant) → Dashboard diretamente
5. Botão logout no header → volta para login, limpa estado
6. Fluxo "Esqueci senha" completo (3 telas) navegável
7. AuthLayout responsivo: split-screen em desktop ≥ 768px, single-column no mobile
8. Todos os estados de loading com feedback visual ("Entrando...", spinner no botão)
9. Identidade visual: painel esquerdo verde Aegis, formulário em off-white, tipografia Inter
