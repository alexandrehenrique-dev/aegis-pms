# Etapa 06 — Auth Proxy, SMTP e fluxo de convite

> Cole este arquivo inteiro numa conversa nova do GPT. Pré-requisito: etapa 05 concluída (Resource Server e `/api/v1/me` funcionando).

## Contexto fixo

Até a etapa 05, o backend valida tokens JWT emitidos pelo Keycloak mas não tem endpoints de autenticação próprios. A validação da etapa 05 ainda usa `grant_type=password` diretamente contra o Keycloak para obter o token de teste — isso funciona em `curl`/Postman, mas **o frontend nunca deve chamar o Keycloak diretamente**: URLs e segredos do Keycloak ficariam expostos no código do SPA, violando o princípio de que o frontend só conhece a URL do backend Aegis.

Esta etapa resolve três lacunas de forma integrada:

1. **Auth proxy (BFF)** — endpoints no backend que o frontend chama para login/logout/refresh, sem nunca saber a URL do Keycloak.
2. **SMTP** — Keycloak precisa enviar e-mails (convite, redefinição de senha). Sem SMTP configurado, o fluxo de convite da etapa 15 nunca dispara o e-mail.
3. **Template de e-mail** — o e-mail de convite que o Keycloak envia deve ter a identidade visual Aegis, não o template genérico do Keycloak.

## Decisão arquitetural — por que BFF (Resource Owner Password via backend), não Authorization Code + PKCE direto da SPA

O OAuth 2.0 Security Best Current Practice recomenda Authorization Code + PKCE para SPAs. A recomendação é válida e a Sprint 06 do frontend (`06_keycloak_login_ui_custom.md`) documenta essa opção. Aqui adotamos a alternativa — proxy via backend — pelos seguintes motivos explícitos:

- O frontend Aegis **não pode conhecer a URL do Keycloak**: isso é um requisito de design, não uma preferência. Com Auth Code, o redirect e a troca de código acontecem parcialmente no browser, expondo o `authorization_endpoint` do Keycloak ao SPA.
- Aegis PMS é uma ferramenta interna (não uma plataforma pública de consumo). O risco principal do ROPC (phishing via tela de login falsa) é mitigado pelo fato de a tela de login ser servida pela própria aplicação.
- A surface de ataque acrescentada pelo ROPC (senha trafega pelo backend antes de ir ao Keycloak) é aceitável neste contexto, desde que a comunicação backend→Keycloak seja em rede interna (Docker) e o backend nunca persista a senha.

**Trade-off registrado**: se no futuro o Aegis precisar expor login a usuários externos (OAuth social, SSO corporativo), a migração para Auth Code + PKCE é necessária — esta arquitetura torna isso mais trabalhoso porque o BFF precisaria ser reescrito para proxy de redirect, não de credenciais.

## Objetivo

1. Expor endpoints `/api/v1/auth/*` que o frontend usa para toda interação de autenticação.
2. Configurar SMTP no Keycloak (MailHog em dev, real SMTP em prod via env vars).
3. Criar o template de e-mail Aegis para o fluxo de convite do Keycloak.

---

## Tarefas

### A. Endpoints Auth Proxy

```txt
POST /api/v1/auth/login
POST /api/v1/auth/logout
POST /api/v1/auth/refresh
```

Estes endpoints são **públicos** (não exigem `Authorization: Bearer` — o usuário ainda não tem token ao chamar `/login`). Ajustar o `SecurityFilterChain` da etapa 05 para liberar `/api/v1/auth/**` sem autenticação.

#### A.1 `POST /api/v1/auth/login`

Request:
```json
{ "username": "loki", "password": "senha123" }
```

O backend faz um `POST` interno para o endpoint de token do Keycloak:
```
POST http://keycloak:8080/realms/aegis/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

client_id=aegis-web&grant_type=password&username={username}&password={password}
```

> **Nota**: dentro da rede Docker o Keycloak é acessível em `http://keycloak:8080` (nome do serviço, não a porta exposta no host `8282`). O backend nunca usa `localhost:8282` para chamar o Keycloak — sempre o nome do serviço interno. Configurar via env var `KEYCLOAK_INTERNAL_BASE_URL=http://keycloak:8080` (ver Seção G).

Resposta ao frontend (repassa o essencial do token do Keycloak):
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "expiresIn": 300,
  "tokenType": "Bearer"
}
```

Erros:
- `401 INVALID_CREDENTIALS` se o Keycloak retornar 401 — nunca repassar o corpo bruto do Keycloak, sempre normalizar para o padrão de erro Aegis.
- `423 ACCOUNT_DISABLED` se o Keycloak retornar conta desabilitada/bloqueada.

#### A.2 `POST /api/v1/auth/logout`

Request:
```json
{ "refreshToken": "eyJ..." }
```

O backend chama o endpoint de logout do Keycloak para invalidar a sessão:
```
POST http://keycloak:8080/realms/aegis/protocol/openid-connect/logout
Content-Type: application/x-www-form-urlencoded

client_id=aegis-web&refresh_token={refreshToken}
```

Resposta: `204 No Content` (independente do resultado do Keycloak — se o refresh token já expirou, isso ainda é um logout válido do ponto de vista do cliente).

#### A.3 `POST /api/v1/auth/refresh`

Request:
```json
{ "refreshToken": "eyJ..." }
```

Chama o endpoint de token do Keycloak com `grant_type=refresh_token`:
```
POST http://keycloak:8080/realms/aegis/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

client_id=aegis-web&grant_type=refresh_token&refresh_token={refreshToken}
```

Resposta: mesmo shape de `POST /login` (novo `accessToken` + `refreshToken` rotacionado + `expiresIn`).

Erros:
- `401 REFRESH_TOKEN_EXPIRED` se o refresh token expirou ou foi revogado.

#### A.4 Implementação — `KeycloakTokenClient`

Criar uma classe `auth/KeycloakTokenClient.java` responsável exclusivamente por chamar o Keycloak via HTTP (usando `RestClient` do Spring 6+, não `RestTemplate` obsoleto). Razões para isolar:
- Testável com WireMock sem subir o Keycloak real.
- Único ponto de conhecimento da URL interna do Keycloak no código Java.
- Reutilizada pela etapa 15 (`KeycloakAdminClient` é diferente — Admin API; `KeycloakTokenClient` é o token endpoint).

Não existe entidade, repositório ou mapper nesta etapa — as rodadas são:
1. `KeycloakTokenClient` + testes WireMock (simular Keycloak retornando 200, 401, conta desabilitada).
2. DTOs de request/response (`AuthLoginRequest`, `AuthTokenResponse`, `AuthRefreshRequest`, `AuthLogoutRequest` — todos `record`).
3. `AuthService` (orquestra `KeycloakTokenClient`, normaliza erros) + testes unitários com mock do client.
4. `AuthController` (endpoints da Seção A) + testes `@WebMvcTest` — liberar `/api/v1/auth/**` no `SecurityFilterChain` nestes testes também.

### B. SMTP para o Keycloak

O Keycloak precisa de um servidor SMTP para enviar e-mails (convite de usuário, redefinição de senha). Sem isso, a Seção B da etapa 15 (`tenants/{tenantId}/users/invite`) cria o usuário no Keycloak mas o e-mail de convite nunca é entregue.

#### B.1 Desenvolvimento local — MailHog

Adicionar ao `docker-compose.yml`:

```yaml
  mailhog:
    image: mailhog/mailhog:latest
    container_name: aegis-mailhog
    restart: unless-stopped
    ports:
      - "${MAILHOG_SMTP_PORT:-1025}:1025"   # SMTP
      - "${MAILHOG_UI_PORT:-8025}:8025"     # UI web para inspecionar e-mails
    networks:
      - aegis-network
```

MailHog captura todos os e-mails enviados sem encaminhar nada. Acessar a UI em `http://localhost:8025` para inspecionar o e-mail de convite durante o desenvolvimento.

#### B.2 Configuração do Keycloak para usar SMTP

Atualizar o Keycloak no `docker-compose.yml` com as variáveis de ambiente SMTP:

```yaml
  keycloak:
    # ... configuração existente da etapa 03 ...
    environment:
      # ... variáveis existentes ...
      KC_SMTP_HOST: ${SMTP_HOST:-mailhog}
      KC_SMTP_PORT: ${SMTP_PORT:-1025}
      KC_SMTP_FROM: ${SMTP_FROM:-noreply@aegis.app}
      KC_SMTP_FROM_DISPLAY_NAME: ${SMTP_FROM_DISPLAY_NAME:-Aegis PMS}
      KC_SMTP_AUTH: ${SMTP_AUTH:-false}
      KC_SMTP_USER: ${SMTP_USER:-}
      KC_SMTP_PASSWORD: ${SMTP_PASSWORD:-}
      KC_SMTP_STARTTLS: ${SMTP_STARTTLS:-false}
      KC_SMTP_SSL: ${SMTP_SSL:-false}
```

> **Produção**: substituir as vars `SMTP_*` por credenciais reais (ex.: AWS SES, SendGrid, Gmail SMTP) via `.env` — as variáveis padrão apontam para MailHog em dev, sem necessidade de alterar o `docker-compose.yml`.

Após subir, confirmar via Admin Console do Keycloak (`Realm Settings → Email`) que o SMTP está salvo e testável.

#### B.3 Export do realm atualizado

Após configurar o SMTP, re-exportar o realm para versionar a configuração:

```bash
docker exec -it aegis-keycloak /opt/keycloak/bin/kc.sh export \
  --dir /tmp/keycloak-export \
  --realm aegis \
  --users realm_file

docker cp aegis-keycloak:/tmp/keycloak-export/aegis-realm.json ./infra/keycloak/realm/aegis-realm.json
```

> **Atenção**: o export do realm inclui `smtpServer` com as configurações de SMTP, mas **sem a senha** (o Keycloak omite campos de credencial no export). Isso é correto — a senha fica em variável de ambiente, nunca versionada.

### C. Template de e-mail Keycloak (identidade visual Aegis)

O Keycloak usa templates FreeMarker (`.ftl`) para os e-mails que envia. O template padrão é genérico; criar um tema `aegis` que sobrescreve o e-mail de convite/ações obrigatórias.

#### C.1 Estrutura de diretórios

```
infra/keycloak/themes/aegis/
├── login/          ← tema da tela de login (Sprint 06 do frontend)
└── email/
    ├── theme.properties
    ├── html/
    │   ├── executeActions.ftl    ← e-mail de convite / definir senha
    │   └── password-reset.ftl   ← e-mail de redefinição de senha
    └── text/
        ├── executeActions.ftl   ← versão texto puro (fallback)
        └── password-reset.ftl
```

> **Nota:** Os templates `productAssignment.ftl` e `productAccessRevoked.ftl` (e-mails informativos de atribuição/revogação de produto) são necessários pelas etapas 10 e 15, mas **não fazem parte do escopo desta etapa** — esta etapa foi executada antes da ADR-0020 existir. Esses templates estão documentados em `06.1_hotfix_templates_email_produto.md`.

Montar o volume no `docker-compose.yml` (Keycloak):

```yaml
  keycloak:
    # ... configuração existente ...
    volumes:
      - ./infra/keycloak/themes:/opt/keycloak/themes
      - ./infra/keycloak/realm:/opt/keycloak/realm
```

Configurar o tema do realm para usar `aegis`:

```bash
# Via kcadm.sh (executar após o Keycloak subir com o volume montado)
docker exec -it aegis-keycloak /opt/keycloak/bin/kcadm.sh config credentials \
  --server http://localhost:8080 --realm master --user admin --password admin

docker exec -it aegis-keycloak /opt/keycloak/bin/kcadm.sh update realms/aegis \
  -s emailTheme=aegis
```

#### C.2 `theme.properties`

```properties
parent=base
import=common/keycloak
```

#### C.3 `email/html/executeActions.ftl` — e-mail de convite

Este template é chamado quando o backend (etapa 15) executa `executeActionsEmail` via Admin API com `required_actions=["UPDATE_PASSWORD"]`. O link `${link}` é gerado pelo Keycloak e aponta para o endpoint de action-token, que abre a tela de definição de senha (usa o tema `aegis/login`).

```html
<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Convite Aegis PMS</title>
</head>
<body style="margin:0;padding:0;background-color:#f4f4f5;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;">
  <table width="100%" cellpadding="0" cellspacing="0" style="background-color:#f4f4f5;padding:40px 0;">
    <tr>
      <td align="center">
        <table width="560" cellpadding="0" cellspacing="0"
               style="background-color:#ffffff;border-radius:8px;overflow:hidden;
                      box-shadow:0 1px 3px rgba(0,0,0,0.1);">

          <!-- Header -->
          <tr>
            <td style="background-color:#1a1a2e;padding:32px 40px;text-align:center;">
              <span style="color:#ffffff;font-size:22px;font-weight:700;
                           letter-spacing:-0.5px;">Aegis PMS</span>
            </td>
          </tr>

          <!-- Body -->
          <tr>
            <td style="padding:40px 40px 32px;">
              <p style="margin:0 0 16px;color:#111827;font-size:16px;font-weight:600;">
                Olá${firstName?has_content?then(', ' + firstName, '')}!
              </p>
              <p style="margin:0 0 24px;color:#374151;font-size:15px;line-height:1.6;">
                Você foi convidado para acessar o <strong>Aegis PMS</strong>.
                Clique no botão abaixo para definir sua senha e ativar seu acesso.
              </p>

              <!-- CTA Button -->
              <table cellpadding="0" cellspacing="0" style="margin:0 0 32px;">
                <tr>
                  <td style="background-color:#4f46e5;border-radius:6px;">
                    <a href="${link}"
                       style="display:inline-block;padding:14px 28px;
                              color:#ffffff;font-size:15px;font-weight:600;
                              text-decoration:none;letter-spacing:0.2px;">
                      Definir minha senha
                    </a>
                  </td>
                </tr>
              </table>

              <p style="margin:0 0 8px;color:#6b7280;font-size:13px;line-height:1.5;">
                Se o botão não funcionar, copie e cole este link no seu navegador:
              </p>
              <p style="margin:0 0 24px;word-break:break-all;">
                <a href="${link}" style="color:#4f46e5;font-size:13px;">${link}</a>
              </p>

              <p style="margin:0;color:#9ca3af;font-size:12px;line-height:1.5;">
                Este link expira em <strong>${linkExpirationFormatter(linkExpiration)?no_esc}</strong>.
                Se você não esperava este convite, pode ignorar este e-mail com segurança.
              </p>
            </td>
          </tr>

          <!-- Footer -->
          <tr>
            <td style="background-color:#f9fafb;border-top:1px solid #e5e7eb;
                       padding:20px 40px;text-align:center;">
              <p style="margin:0;color:#9ca3af;font-size:12px;">
                Aegis PMS · Sistema de Gestão de Produtos
              </p>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>
```

#### C.4 `email/html/password-reset.ftl` — redefinição de senha

Mesmo layout, texto diferente:

```html
<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Redefinição de senha — Aegis PMS</title>
</head>
<body style="margin:0;padding:0;background-color:#f4f4f5;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;">
  <table width="100%" cellpadding="0" cellspacing="0" style="background-color:#f4f4f5;padding:40px 0;">
    <tr>
      <td align="center">
        <table width="560" cellpadding="0" cellspacing="0"
               style="background-color:#ffffff;border-radius:8px;overflow:hidden;
                      box-shadow:0 1px 3px rgba(0,0,0,0.1);">
          <tr>
            <td style="background-color:#1a1a2e;padding:32px 40px;text-align:center;">
              <span style="color:#ffffff;font-size:22px;font-weight:700;
                           letter-spacing:-0.5px;">Aegis PMS</span>
            </td>
          </tr>
          <tr>
            <td style="padding:40px 40px 32px;">
              <p style="margin:0 0 16px;color:#111827;font-size:16px;font-weight:600;">
                Olá${firstName?has_content?then(', ' + firstName, '')}!
              </p>
              <p style="margin:0 0 24px;color:#374151;font-size:15px;line-height:1.6;">
                Recebemos uma solicitação para redefinir a senha da sua conta no Aegis PMS.
                Clique no botão abaixo para criar uma nova senha.
              </p>
              <table cellpadding="0" cellspacing="0" style="margin:0 0 32px;">
                <tr>
                  <td style="background-color:#4f46e5;border-radius:6px;">
                    <a href="${link}"
                       style="display:inline-block;padding:14px 28px;
                              color:#ffffff;font-size:15px;font-weight:600;
                              text-decoration:none;letter-spacing:0.2px;">
                      Redefinir senha
                    </a>
                  </td>
                </tr>
              </table>
              <p style="margin:0 0 8px;color:#6b7280;font-size:13px;line-height:1.5;">
                Se o botão não funcionar, copie e cole este link no seu navegador:
              </p>
              <p style="margin:0 0 24px;word-break:break-all;">
                <a href="${link}" style="color:#4f46e5;font-size:13px;">${link}</a>
              </p>
              <p style="margin:0;color:#9ca3af;font-size:12px;line-height:1.5;">
                Este link expira em <strong>${linkExpirationFormatter(linkExpiration)?no_esc}</strong>.
                Se você não solicitou a redefinição de senha, ignore este e-mail.
                Sua senha permanece a mesma.
              </p>
            </td>
          </tr>
          <tr>
            <td style="background-color:#f9fafb;border-top:1px solid #e5e7eb;
                       padding:20px 40px;text-align:center;">
              <p style="margin:0;color:#9ca3af;font-size:12px;">
                Aegis PMS · Sistema de Gestão de Produtos
              </p>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>
```

#### C.5 `email/text/executeActions.ftl` — fallback texto puro

```
Olá${firstName?has_content?then(', ' + firstName, '')}!

Você foi convidado para acessar o Aegis PMS.
Acesse o link abaixo para definir sua senha:

${link}

Este link expira em ${linkExpirationFormatter(linkExpiration)?no_esc}.

Se você não esperava este convite, ignore este e-mail.
```

#### C.6 `email/text/password-reset.ftl` — fallback texto puro

```
Olá${firstName?has_content?then(', ' + firstName, '')}!

Recebemos uma solicitação para redefinir sua senha no Aegis PMS.
Acesse o link abaixo para criar uma nova senha:

${link}

Este link expira em ${linkExpirationFormatter(linkExpiration)?no_esc}.

Se você não solicitou isso, ignore este e-mail.
```

---

### D. Fluxo completo de convite — ponta a ponta

Este é o fluxo que integra a etapa 06 com a etapa 15 (domínio users). Documentado aqui para que o GPT da etapa 15 saiba exatamente o que precisa estar pronto.

```
Admin (TENANT_ADMIN ou SUPER_ADMIN)
  │
  ├── POST /api/v1/tenants/{tenantId}/users/invite
  │     { name, email, role, allowedProducts }
  │
  ▼
Backend (etapa 15 — UserService)
  ├── Verifica duplicata de e-mail no tenant → 409 se duplicado
  ├── Chama Keycloak Admin API: POST /admin/realms/aegis/users
  │     { username: email, email, firstName: name,
  │       enabled: true, emailVerified: false,
  │       requiredActions: ["UPDATE_PASSWORD"] }
  ├── Obtém o subject (UUID) do usuário criado no Keycloak
  ├── Cria TenantMembership local (subject, tenantId, role, status: "convidado")
  ├── Se veio de ProductAssignment: cria também o registro
  ├── Chama Keycloak Admin API: PUT /admin/realms/aegis/users/{id}/execute-actions-email
  │     body: ["UPDATE_PASSWORD"]
  │     queryParam: lifespan=86400 (link válido por 24h)
  │     → Keycloak dispara o e-mail via SMTP (template Seção C)
  └── Retorna UserSummary { ..., inviteStatus: "pendente" }

Usuário convidado
  ├── Recebe e-mail com botão "Definir minha senha"
  ├── Clica → Keycloak → tela de definição de senha (tema aegis/login, Sprint 06 frontend)
  ├── Define senha → Keycloak marca emailVerified: true, remove requiredActions
  └── Redireciona para login do Aegis

Primeiro login do usuário convidado
  ├── POST /api/v1/auth/login { username: email, password: novaSenha }
  ├── Backend obtém token do Keycloak
  ├── Frontend recebe accessToken + refreshToken
  ├── Frontend chama GET /api/v1/me → role: "editor" (ou a role do convite)
  └── Backend (próximo GET /tenants/{tenantId}/users):
        ├── Detecta que emailVerified agora é true no Keycloak
        └── Atualiza TenantMembership.inviteStatus → "ativo"
```

> **Nota importante sobre `inviteStatus: "ativo"`**: a atualização do `inviteStatus` pode acontecer de duas formas — lazy (na próxima chamada a `GET /users`) ou via evento do Keycloak (Event Listener SPI). A forma lazy é mais simples de implementar e suficiente por ora: ao listar usuários, verificar `emailVerified` do Keycloak via Admin API e atualizar localmente. A decisão de qual implementar fica a critério do GPT executando a etapa 15 — registrar a escolha no `SPRINT-RESULTADO.md`.

### E. Fluxo de redefinição de senha (link "Esqueci minha senha" na tela de login)

O frontend já tem um link "Esqueci minha senha" (ou similar) na tela de login. Com esta etapa pronta, o fluxo é:

```
POST /api/v1/auth/forgot-password
{ "email": "usuario@exemplo.com" }
```

Backend chama Keycloak Admin API:
```
PUT /admin/realms/aegis/users/{userId}/execute-actions-email
body: ["UPDATE_PASSWORD"]
```

Resposta: sempre `200 OK` com `{ "message": "Se o e-mail estiver cadastrado, você receberá as instruções em breve." }` — nunca confirmar se o e-mail existe ou não (evitar enumeração de usuários).

> **Adicionar este endpoint** (`POST /api/v1/auth/forgot-password`) ao `AuthController` desta etapa, não à etapa 15. Ele não precisa de autenticação (público, como `/login`).

### F. Padrão de qualidade e entrega (obrigatório)

> Resumo — detalhe completo em `00_padrao_qualidade_e_arquitetura.md`.

- **Java 25 / Spring Boot 4.1.x**. Não existe entidade ou repository nesta etapa. As classes elegíveis para cobertura JaCoCo são: `KeycloakTokenClient`, `AuthService`, `AuthController`.
- 100% de cobertura de linha+branch em todas as classes funcionais. `KeycloakTokenClient` testada com **WireMock** (`com.github.tomakehurst:wiremock-standalone`), não com o Keycloak real — adicionar WireMock ao `pom.xml` no scope `test`.
- `AuthController`: usar `@WebMvcTest(AuthController.class)` com `@MockitoBean AuthService` — não subir o contexto inteiro.
- Rodadas (entregar nesta ordem, uma de cada vez):
  1. `KeycloakTokenClient` + testes WireMock.
  2. DTOs (`AuthLoginRequest`, `AuthTokenResponse`, `AuthRefreshRequest`, `AuthLogoutRequest`, `AuthForgotPasswordRequest` — todos `record`).
  3. `AuthService` + testes unitários.
  4. `AuthController` + testes `@WebMvcTest` + atualização do `SecurityFilterChain` para liberar `/api/v1/auth/**`.

Os arquivos de tema (`.ftl`, `theme.properties`) e a configuração do MailHog no `docker-compose.yml` são entregues junto com a rodada 1, antes do código Java, para que o ambiente de teste já possa disparar e-mails ao longo das rodadas.

### G. Novas variáveis de ambiente

Adicionar ao `.env.example` (já feito nesta revisão — apenas confirmar que estão presentes):

```env
# SMTP (dev: MailHog automático; prod: substituir pelos valores reais)
SMTP_HOST=mailhog
SMTP_PORT=1025
SMTP_FROM=noreply@aegis.app
SMTP_FROM_DISPLAY_NAME=Aegis PMS
SMTP_AUTH=false
SMTP_USER=
SMTP_PASSWORD=
SMTP_STARTTLS=false
SMTP_SSL=false
MAILHOG_SMTP_PORT=1025
MAILHOG_UI_PORT=8025

# URL interna do Keycloak (container-to-container, não a porta exposta no host)
KEYCLOAK_INTERNAL_BASE_URL=http://keycloak:8080
```

## Critérios de aceite

- [ ] `POST /api/v1/auth/login` com credenciais válidas retorna `accessToken` + `refreshToken` + `expiresIn`.
- [ ] `POST /api/v1/auth/login` com credenciais inválidas retorna `401 INVALID_CREDENTIALS`.
- [ ] `POST /api/v1/auth/logout` com refresh token válido retorna `204` e invalida a sessão no Keycloak (uma tentativa de refresh subsequente com o mesmo refresh token retorna `401`).
- [ ] `POST /api/v1/auth/refresh` com refresh token válido retorna novo `accessToken` + `refreshToken`.
- [ ] `POST /api/v1/auth/refresh` com refresh token expirado/revogado retorna `401 REFRESH_TOKEN_EXPIRED`.
- [ ] `POST /api/v1/auth/forgot-password` retorna `200` independente de o e-mail existir ou não (sem enumeração de usuários).
- [ ] MailHog acessível em `http://localhost:8025` após `docker compose up -d`.
- [ ] Keycloak envia e-mail via MailHog (visível na UI do MailHog) — testar disparando `execute-actions-email` manualmente via Admin Console.
- [ ] E-mail exibido no MailHog usa o template Aegis (não o template padrão do Keycloak).
- [ ] O link no e-mail redireciona para a tela de definição de senha (Keycloak) e, após definir, redireciona para o login do Aegis.
- [ ] `mvn clean verify` passa com 100% de cobertura nas classes elegíveis (JaCoCo).
- [ ] `infra/keycloak/themes/aegis/email/html/executeActions.ftl` e `password-reset.ftl` existem e estão versionados.
- [ ] `aegis-realm.json` re-exportado com configuração de SMTP (sem senha).

## Validação

> **Entrega via collection Postman, não só curl** (ver `00_padrao_qualidade_e_arquitetura.md`, Seção 11). Os `curl` abaixo são a especificação exata de cada request — adicione-os à pasta desta etapa em `aegis-postman-collection.json` (collection cumulativa, autenticação via `{{token}}` herdado da pasta "Auth") e devolva o JSON completo atualizado para download.

```bash
# Login via backend (não mais direto ao Keycloak)
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"loki","password":"123456"}' | jq .
# esperado: { accessToken, refreshToken, expiresIn, tokenType }

# Armazenar o token para os requests seguintes
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"loki","password":"123456"}' | jq -r .accessToken)

# Verificar que /me ainda funciona com token obtido via proxy
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/me

# Refresh
REFRESH=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"loki","password":"123456"}' | jq -r .refreshToken)

curl -s -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH\"}" | jq .

# Logout
curl -s -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH\"}"
# esperado: 204

# Forgot password (sem autenticação)
curl -s -X POST http://localhost:8080/api/v1/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{"email":"loki@byop.dev"}' | jq .
# esperado: 200 com mensagem genérica, independente de o e-mail existir

# Testar e-mail via MailHog:
# 1. Via Admin Console do Keycloak: Users → loki → Actions → Send email → Update Password
# 2. Verificar recebimento em http://localhost:8025
```

> **Nota para a collection Postman**: a pasta "Auth" da collection deve ter um post-response script em `POST /auth/login` que captura o `accessToken` em `pm.environment.set("token", pm.response.json().accessToken)` e o `refreshToken` em `pm.environment.set("refreshToken", pm.response.json().refreshToken)` — assim todos os requests autenticados subsequentes herdam o token automaticamente.

## Artefato de continuidade — `SPRINT-RESULTADO.md`

> Ver `00_padrao_qualidade_e_arquitetura.md`, Seção 12. Antes do commit, gere/atualize `docs/sprints/backend/SPRINT-RESULTADO.md` (arquivo inteiro, nunca um diff) com a entrada desta etapa (template fixo da Seção 12.2): classes criadas, endpoints confirmados, qualquer decisão que esta etapa deixou a seu critério (registre a escolha real — ex.: "lazy update de `inviteStatus` escolhida, não Event Listener SPI"), e retrofits pendentes para etapas futuras. É o que a próxima conversa do GPT vai receber em vez da memória que ela não tem.

## Commit sugerido

```bash
git add backend/ docker-compose.yml .env.example infra/keycloak/
git commit -m "feat(auth): proxy de autenticação via backend, smtp local com mailhog e templates de email keycloak"
```
