# Sprint 06 — Integração Keycloak preservando a UI de login atual

> Pré-requisito: Sprint 02 (Keycloak persistente, realm/client/roles configurados).

## Contexto

Requisito explícito do usuário: a tela de login mockada atual (UI/UX) **deve continuar exatamente a mesma**; o Keycloak real não pode aparecer com sua tela nativa de login.

Existem duas formas de resolver isso. Esta sprint recomenda uma e documenta a outra para que a decisão final seja consciente:

### Opção recomendada — Tema customizado do Keycloak

O Keycloak suporta temas customizados (FreeMarker + CSS/JS) para a tela de login que ele mesmo serve. A SPA redireciona para o Keycloak (Authorization Code + PKCE — fluxo padrão e seguro para SPA), mas o Keycloak, usando um tema Aegis, **renderiza pixel a pixel a mesma tela de login atual**. O usuário nunca vê a tela "genérica" do Keycloak — vê a tela Aegis, só que servida pelo Keycloak.

Vantagens: mantém o fluxo OAuth2/OIDC padrão e seguro (Authorization Code + PKCE, sem a SPA tocar em senha), token nunca passa pela camada de UI customizada, suporta MFA/social login/recuperação de senha nativos do Keycloak sem trabalho extra.

### Opção alternativa — Resource Owner Password Credentials (ROPC) direto da SPA

A SPA mantém seu próprio formulário de login e troca usuário/senha diretamente pelo token endpoint do Keycloak (`grant_type=password`).

**Não recomendada.** O OAuth 2.0 Security Best Current Practice desaconselha ROPC para SPAs: a senha passa pelo código JavaScript da SPA (maior superfície de risco), não há PKCE, e o fluxo está sendo descontinuado em implementações mais novas de OAuth2. Documentar aqui apenas para registro — não implementar sem decisão explícita e consciente do risco.

Esta sprint assume a **opção recomendada** (tema customizado). Se o usuário decidir pela alternativa, ajustar as tarefas abaixo antes de executar.

## Objetivo

Login real via Keycloak, com a aparência idêntica à tela mockada atual, e sem expor a tela nativa do Keycloak.

## Tarefas

1. No backend/infra do Keycloak (Sprint 02), criar um tema customizado (`keycloak/themes/aegis/login/`) replicando o HTML/CSS da tela de login atual do React (cores, tipografia BYOP Violet, logo, layout — reaproveitar os tokens visuais documentados em `implementation/010_aegis_pms_visual_language_specification.md`).
2. Configurar o realm do Aegis para usar o tema `aegis` (`loginTheme: aegis`).
3. No frontend, em `core/auth`, implementar o fluxo Authorization Code + PKCE: redirecionar para o endpoint de autorização do Keycloak, capturar o `code` no retorno, troca por token via backend (ou diretamente, se a SPA for client público confiável — decisão a confirmar com Feature 007 de `implementation/001`, que já define o client frontend).
4. Repetir o mesmo tratamento visual para as telas adicionais que o Keycloak também serve nesse fluxo (recuperação de senha, ativação de conta, erro de sessão expirada) — devem usar o mesmo tema, não o padrão do Keycloak.
5. Após login bem-sucedido, seguir o fluxo já existente no frontend (seleção de tenant → seleção de produto → dashboard), agora alimentado por dados reais de `/api/v1/me`.
6. Tratar logout e expiração de sessão preservando a experiência (sem flash da tela nativa do Keycloak).

## Critérios de aceite

- [ ] Usuário final nunca vê a tela visual padrão do Keycloak em nenhum momento do fluxo de login, logout, recuperação de senha ou erro de sessão.
- [ ] Login real autentica contra o Keycloak (realm configurado na Sprint 02), não mais contra dados mockados.
- [ ] Token é armazenado e renovado corretamente (refresh token / silent renew).
- [ ] Fluxo de seleção de tenant/produto pós-login continua idêntico ao atual.

## Comandos de Git

```bash
git checkout develop && git pull origin develop
git checkout -b sprint/06-keycloak-login-custom
git commit -m "feat(keycloak): adiciona tema customizado aegis replicando a tela de login atual"
git commit -m "feat(auth): implementa authorization code + pkce contra keycloak real"
git commit -m "feat(auth): liga fluxo pos-login (tenant/produto) a dados reais de /api/v1/me"
git push -u origin sprint/06-keycloak-login-custom
git checkout develop && git merge --no-ff sprint/06-keycloak-login-custom && git push origin develop
```
