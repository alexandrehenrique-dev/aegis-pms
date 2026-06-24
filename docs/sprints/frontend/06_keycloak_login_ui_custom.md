# Sprint 06 — Integração Auth real (proxy backend) preservando a UI de login atual

> Pré-requisito: Sprint 02 backend, etapas 01–06 concluídas (Keycloak configurado + **auth proxy** `POST /api/v1/auth/login|logout|refresh|forgot-password` disponíveis — ver `docs/sprints/backend/06_auth_proxy_smtp_e_convite.md`).

## Contexto e decisão arquitetural

**O frontend não chama o Keycloak diretamente.** Esta decisão foi formalizada na etapa 06 do backend (`06_auth_proxy_smtp_e_convite.md`): o backend expõe um proxy BFF de autenticação — o frontend envia `{ username, password }` para `POST /api/v1/auth/login` e recebe o token de volta, sem nunca saber que o Keycloak existe ou qual é a sua URL.

Isso simplifica substancialmente esta sprint: **não é necessário Authorization Code + PKCE, nem redirect para o Keycloak, nem tema customizado na tela de login nativa do Keycloak** para o fluxo de login principal. A tela de login atual do React é a tela de login definitiva — ela já está correta em aparência, só precisa ser ligada aos endpoints de auth do backend.

O tema customizado Keycloak (`infra/keycloak/themes/aegis/`) ainda é necessário — mas **somente para as telas que o Keycloak serve nativamente**: a tela de "Definir senha" (acionada pelo link do e-mail de convite) e a tela de "Redefinir senha" (link "Esqueci minha senha"). Essas telas aparecem quando o usuário clica no link do e-mail, não no fluxo de login normal.

## Objetivo

Ligar o formulário de login atual ao auth proxy do backend, implementar refresh automático de token, logout real, e o fluxo "Esqueci minha senha" — tudo sem o frontend conhecer a URL do Keycloak.

## Tarefas

### 1. Serviço de autenticação em `core/auth/authService.ts`

Substituir as chamadas mock por chamadas reais aos endpoints do backend:

```ts
// POST /api/v1/auth/login
login(username: string, password: string): Promise<AuthTokenResponse>

// POST /api/v1/auth/logout
logout(refreshToken: string): Promise<void>

// POST /api/v1/auth/refresh
refresh(refreshToken: string): Promise<AuthTokenResponse>

// POST /api/v1/auth/forgot-password
forgotPassword(email: string): Promise<void>
```

`AuthTokenResponse`: `{ accessToken, refreshToken, expiresIn, tokenType }`.

Armazenar `accessToken` e `refreshToken` em memória (nunca `localStorage` — artefatos Cowork/Claude não suportam). Em produção, usar `sessionStorage` ou `httpOnly cookie` (decisão de hardening futuro — por ora, variável em módulo singleton é suficiente para conectar o fluxo).

### 2. Interceptor HTTP — renovação automática de token

Criar `core/auth/authInterceptor.ts` (ou wrapper equivalente do `fetch`/`axios`):
- Se uma resposta é `401`, tentar `refresh(refreshToken)` uma vez.
- Se o refresh der certo, repetir a request original com o novo token.
- Se o refresh também der `401`, redirecionar para `/login` e limpar tokens armazenados.

### 3. Tela de login — ligar ao authService

Em `LoginPage.tsx` (ou equivalente atual), substituir a submissão mock:
- Chamar `authService.login(username, password)`.
- Em caso de erro `401`: exibir mensagem "Credenciais inválidas".
- Em caso de sucesso: salvar tokens, chamar `GET /api/v1/me` para obter o `role`, redirecionar para o fluxo de seleção de tenant existente.

### 4. Link "Esqueci minha senha"

O link "Esqueci minha senha" já existe na tela de login. Ligar a um modal/tela que coleta o e-mail e chama `authService.forgotPassword(email)`. Exibir sempre a mensagem genérica: "Se o e-mail estiver cadastrado, você receberá as instruções em breve."

O link que o usuário recebe por e-mail (gerado pelo Keycloak) aponta para a tela de redefinição do Keycloak — que usa o tema `aegis` configurado na etapa 06 do backend. **Esta sprint não precisa criar o tema** — ele já é entregue pela etapa 06 do backend. Esta sprint só precisa garantir que, após o usuário redefinir a senha via Keycloak, ele é redirecionado de volta para a tela de login do Aegis (configurar `redirectUri` se necessário no `POST /auth/forgot-password`).

### 5. Logout

Ao clicar em "Sair" (qualquer ponto da UI): chamar `authService.logout(refreshToken)`, limpar tokens em memória, redirecionar para `/login`.

### 6. Tema Keycloak (login de telas nativas — convite e redefinição)

> Este item é responsabilidade de infraestrutura (entregue junto com a etapa 06 do backend em `infra/keycloak/themes/aegis/`). Se ainda não estiver entregue quando esta sprint for executada, criá-lo aqui.

Estrutura mínima necessária para que a tela de "Definir senha" e "Redefinir senha" usem a identidade visual Aegis:
```
infra/keycloak/themes/aegis/login/
├── theme.properties        (parent=keycloak; import=common/keycloak)
├── login.ftl               (HTML do formulário de login — idêntico à tela React, em FreeMarker)
├── login-update-password.ftl   (tela de "Definir minha senha" — para o convite)
├── login-reset-password.ftl    (tela de "Esqueci minha senha" — form de e-mail)
└── resources/
    └── css/
        └── login.css       (mesmos tokens visuais: --color-primary=#4f46e5, fonte sistema)
```

## Critérios de aceite

- [ ] `POST /api/v1/auth/login` com credenciais corretas redireciona para o fluxo de seleção de tenant.
- [ ] `POST /api/v1/auth/login` com credenciais erradas exibe mensagem de erro na tela.
- [ ] Token é renovado automaticamente via interceptor antes de expirar; sem interrupção de sessão ao usuário.
- [ ] Logout limpa tokens e redireciona para `/login`.
- [ ] "Esqueci minha senha" chama `POST /api/v1/auth/forgot-password` e exibe mensagem genérica.
- [ ] O frontend **em nenhum momento** faz fetch para `localhost:8282` ou qualquer URL do Keycloak — todas as chamadas de auth vão para `localhost:8080/api/v1/auth/*`.
- [ ] Fluxo pós-login (tenant → produto → dashboard) alimentado por dados reais de `GET /api/v1/me`.

## Comandos de Git

```bash
git checkout develop && git pull origin develop
git checkout -b sprint/06-auth-real-proxy
git commit -m "feat(auth): authService ligado ao proxy backend (login/logout/refresh/forgot-password)"
git commit -m "feat(auth): interceptor de renovação automática de token"
git commit -m "feat(auth): fluxo pos-login ligado a /api/v1/me real"
git push -u origin sprint/06-auth-real-proxy
git checkout develop && git merge --no-ff sprint/06-auth-real-proxy && git push origin develop
```
