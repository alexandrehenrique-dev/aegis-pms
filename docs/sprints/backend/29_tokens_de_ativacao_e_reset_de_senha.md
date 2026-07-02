# Etapa 29 — Tokens de ativação de convite e reset de senha via Aegis (sem redirecionar para o Keycloak)

> Pré-requisitos: etapa 06 (SMTP + `KeycloakAdminClient`), etapa 10 (`ProductAssignment`), etapa 15 (users).
>
> **Motivação:** o fluxo atual usa `executeActionsEmail` do Keycloak Admin API — o Keycloak gera e envia o e-mail com um link que aponta para as próprias páginas do Keycloak (`{keycloakBaseUrl}/realms/aegis/login-actions/...`). O usuário final vê uma tela Keycloak fora da identidade visual do Aegis, sem contexto do convite (tenant, produto, papel), e sem a UX do `InviteScreen.tsx` já implementado no frontend. O mesmo problema acontece no fluxo de "Esqueci minha senha" — o e-mail enviado é do Keycloak, o link vai para o Keycloak.
>
> Esta etapa substitui ambos os fluxos por tokens gerenciados pelo Aegis: o **backend gera o token, envia o e-mail com link para o frontend Aegis**, e quando o usuário define a senha, o backend chama o Keycloak Admin API para persistir a senha. O Keycloak deixa de enviar qualquer e-mail de ação.

## A. Entidade `AuthActionToken`

```java
@Entity
@Table(name = "auth_action_tokens", indexes = {
    @Index(name = "idx_aat_status_expires", columnList = "status, expires_at")
})
public class AuthActionToken {
  @Id UUID id;                   // tokenId — vai na URL do frontend
  String keycloakId;             // UUID do usuário no Keycloak
  String userEmail;              // e-mail do destinatário (para exibir no InviteScreen)
  String userName;               // nome do usuário convidado
  @Enumerated(EnumType.STRING)
  AuthActionType type;           // INVITE | PASSWORD_RESET
  @Enumerated(EnumType.STRING)
  AuthActionStatus status;       // PENDING | USED | EXPIRED
  // Campos de contexto do convite (nullable para PASSWORD_RESET)
  UUID tenantId;
  String tenantName;
  String productNames;           // JSON array serializado
  String role;
  String inviterName;
  // Lifecycle
  Instant createdAt;
  Instant expiresAt;             // INVITE: +48h; PASSWORD_RESET: +1h
  Instant usedAt;
}
```

**Expiração por tipo:**
- `INVITE`: 48 horas (usuário tem tempo para checar e-mail corporativo)
- `PASSWORD_RESET`: 1 hora (janela curta por segurança)

**Limpeza:** job `@Scheduled(cron = "0 0 4 * * *")` → marca `PENDING` com `expiresAt < now` como `EXPIRED`.

## B. Endpoints novos

```txt
POST /api/v1/auth/invite/validate          ← valida token antes de exibir o card (GET seria semanticamente correto mas expõe token na URL de log — usar POST com body)
POST /api/v1/auth/activate                 ← ativa convite: define senha e marca token como USED
POST /api/v1/auth/reset-password/request   ← solicita reset: gera token, envia e-mail
POST /api/v1/auth/reset-password/confirm   ← confirma reset: valida token, define nova senha
```

Todos são **públicos** (sem autenticação JWT) — adicionar ao `SecurityFilterChain` como permitAll():
```java
.requestMatchers("/api/v1/auth/**").permitAll()
```
Essa linha já existe da etapa 06 — confirmar que os paths novos estão cobertos.

### B.1 `POST /api/v1/auth/invite/validate`

```ts
// Request
{ token: string }
// Response 200
{
  userName: string;
  userEmail: string;
  tenantName: string;
  productNames: string[];
  role: string;
  inviterName: string;
  expiresAt: string; // ISO 8601
}
// Response 400 — token já usado
{ "error": "TOKEN_ALREADY_USED" }
// Response 410 — expirado
{ "error": "TOKEN_EXPIRED" }
// Response 404 — token não existe
{ "error": "TOKEN_NOT_FOUND" }
```

### B.2 `POST /api/v1/auth/activate`

```ts
// Request
{ token: string; password: string; }
// Response 200
{ "message": "Conta ativada. Faça login para continuar." }
// Response 422 — senha fraca (validar no backend também)
{ "error": "WEAK_PASSWORD", "message": "A senha deve ter ao menos 8 caracteres, incluindo letras e números." }
// Erros de token: 400, 410, 404 (iguais ao validate)
```

Sequência:
1. Buscar e validar `AuthActionToken` (tipo INVITE, status PENDING, não expirado)
2. Chamar Keycloak Admin API: `PUT /admin/realms/aegis/users/{keycloakId}/reset-password`
   ```json
   { "type": "password", "value": "<senha>", "temporary": false }
   ```
3. Chamar Keycloak Admin API: `PUT /admin/realms/aegis/users/{keycloakId}` com `{ "enabled": true }` (garantir que está habilitado)
4. Remover `requiredActions` do usuário no Keycloak (se houver algum do momento da criação): `PUT /admin/realms/aegis/users/{keycloakId}` com `{ "requiredActions": [] }`
5. Marcar `AuthActionToken.status = USED`, `usedAt = now`
6. Registrar evento de auditoria: `USER_INVITE_ACTIVATED`
7. Retornar 200

### B.3 `POST /api/v1/auth/reset-password/request`

```ts
// Request
{ email: string }
// Response 200 — sempre (não revelar se o e-mail existe ou não — prevenção de enumeração)
{ "message": "Se este e-mail existe na plataforma, um link de recuperação será enviado." }
```

Sequência:
1. Buscar usuário pelo e-mail via `KeycloakAdminClient.findUserByEmail(email)` — se não encontrado, **retornar 200 sem fazer nada** (não revelar que e-mail não existe)
2. Invalidar tokens `PASSWORD_RESET` pendentes do mesmo usuário (marcar `EXPIRED`) — evitar múltiplos links válidos simultâneos
3. Criar novo `AuthActionToken` com `type = PASSWORD_RESET`, `expiresAt = now + 1h`
4. Enviar e-mail via SMTP com template `passwordReset.ftl` e link: `${AEGIS_APP_BASE_URL}/reset-password?token={tokenId}`
5. Registrar evento: `PASSWORD_RESET_REQUESTED`

**Rate limiting** (sem library externa — implementar manualmente):
- Máximo 3 tokens `PASSWORD_RESET` por e-mail em 15 minutos → retornar 429 `TOO_MANY_REQUESTS` ao 4º pedido (com Retry-After header)
- Contar via query: `countByUserEmailAndTypeAndCreatedAtAfter(email, PASSWORD_RESET, now.minus(15 min))`

### B.4 `POST /api/v1/auth/reset-password/confirm`

```ts
// Request
{ token: string; password: string; }
// Response 200
{ "message": "Senha redefinida. Faça login para continuar." }
// Mesmos erros de token que /activate
```

Sequência: igual ao `/activate` mas sem habilitar usuário (já está ativo) e sem remover `requiredActions`. Registrar `PASSWORD_RESET_COMPLETED`.

## C. Atualização do fluxo de convite (etapas 06, 10, 15)

**Substituir `executeActionsEmail`** em todos os pontos onde é chamado:

| Chamada atual | Substituir por |
|---|---|
| `KeycloakAdminClient.executeActionsEmail(keycloakId, ["UPDATE_PASSWORD"])` no convite de usuário (etapa 15) | Criar `AuthActionToken(INVITE)` + enviar `inviteActivation.ftl` |
| `KeycloakAdminClient.executeActionsEmail(keycloakId, ["UPDATE_PASSWORD"])` no restore de usuário (etapa 15) | Criar `AuthActionToken(INVITE)` + enviar `inviteActivation.ftl` com texto de restauração |
| `executeActionsEmail` no `POST /products/{productId}/users` com `inviteEmail` (etapa 10) | Criar `AuthActionToken(INVITE)` + enviar `inviteActivation.ftl` |

`KeycloakAdminClient.executeActionsEmail` **não é mais chamado em produção** após esta etapa. Manter o método na classe para uso opcional em ambientes de desenvolvimento/debug (documentar no SPRINT-RESULTADO.md).

Ao criar o usuário no Keycloak via `KeycloakAdminClient.createUser(...)`, **não passar `requiredActions`** — o Aegis agora controla o fluxo de senha, não o Keycloak.

## D. Templates de e-mail atualizados

### D.1 `inviteActivation.ftl` — substitui `executeActions.ftl` para convites

> Arquivo: mesmo caminho (`infra/keycloak/themes/aegis/email/html/inviteActivation.ftl`)
>
> Diferença chave: o link aponta para `${aegisAppUrl}/invite?token=${tokenId}`, não para `${link}` (URL do Keycloak).

Variáveis: `${userName}`, `${inviterName}`, `${tenantName}`, `${productNames}` (lista separada por vírgula), `${role}`, `${activationUrl}` (`${aegisAppUrl}/invite?token=${tokenId}`), `${expiresAt}` (formatado pt-BR).

```html
<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8">
  <title>Você foi convidado — Aegis PMS</title>
  <style>
    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #f5f5f5; margin: 0; padding: 0; }
    .container { max-width: 560px; margin: 40px auto; background: #fff; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,.1); }
    .header { background: #1a1a2e; padding: 32px 40px; text-align: center; }
    .header h1 { color: #fff; font-size: 20px; margin: 0; font-weight: 600; }
    .body { padding: 36px 40px; color: #374151; }
    p { font-size: 15px; line-height: 1.6; margin: 0 0 16px; }
    .invite-card { background: #ede9fe; border-radius: 8px; padding: 18px 22px; margin: 22px 0; }
    .invite-card .label { font-size: 11px; color: #7c3aed; text-transform: uppercase; letter-spacing: .5px; font-weight: 600; margin-bottom: 10px; }
    .invite-row { display: flex; justify-content: space-between; font-size: 13px; margin-bottom: 6px; }
    .invite-row .key { color: #6b7280; }
    .invite-row .val { font-weight: 600; color: #111827; }
    .cta { text-align: center; margin: 28px 0 12px; }
    .btn { display: inline-block; background: #4f46e5; color: #fff; text-decoration: none; padding: 14px 40px; border-radius: 7px; font-size: 15px; font-weight: 600; }
    .expire { font-size: 12px; color: #9ca3af; text-align: center; margin-top: 10px; }
    .warning { background: #fffbeb; border: 1px solid #fcd34d; border-radius: 6px; padding: 12px 16px; font-size: 13px; color: #92400e; margin: 20px 0; }
    .footer { padding: 20px 40px; border-top: 1px solid #f3f4f6; color: #9ca3af; font-size: 12px; text-align: center; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header"><h1>Aegis PMS</h1></div>
    <div class="body">
      <p>Olá, <strong>${userName}</strong>.</p>
      <p><strong>${inviterName}</strong> convidou você para a plataforma <strong>Aegis PMS</strong>. Clique no botão abaixo para definir sua senha e ativar sua conta.</p>

      <div class="invite-card">
        <div class="label">Detalhes do convite</div>
        <div class="invite-row"><span class="key">Workspace</span><span class="val">${tenantName}</span></div>
        <div class="invite-row"><span class="key">Papel</span><span class="val">${role}</span></div>
        <div class="invite-row"><span class="key">Produtos</span><span class="val">${productNames}</span></div>
      </div>

      <div class="cta">
        <a href="${activationUrl}" class="btn">Ativar minha conta</a>
      </div>
      <div class="expire">Este link expira em <strong>${expiresAt}</strong>.</div>

      <div class="warning">
        ⚠️ Se você não esperava este convite, ignore este e-mail — nenhuma ação é necessária da sua parte.
      </div>
    </div>
    <div class="footer">Aegis PMS · aegis.app<br>Você recebeu este e-mail porque foi convidado para a plataforma.</div>
  </div>
</body>
</html>
```

### D.2 `passwordReset.ftl` — substitui `password-reset.ftl` do Keycloak

Variáveis: `${userName}`, `${resetUrl}` (`${aegisAppUrl}/reset-password?token=${tokenId}`), `${expiresAt}`.

```html
<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8">
  <title>Recuperação de senha — Aegis PMS</title>
  <style>
    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #f5f5f5; margin: 0; padding: 0; }
    .container { max-width: 560px; margin: 40px auto; background: #fff; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,.1); }
    .header { background: #1a1a2e; padding: 32px 40px; text-align: center; }
    .header h1 { color: #fff; font-size: 20px; margin: 0; font-weight: 600; }
    .body { padding: 36px 40px; color: #374151; }
    p { font-size: 15px; line-height: 1.6; margin: 0 0 16px; }
    .cta { text-align: center; margin: 28px 0 12px; }
    .btn { display: inline-block; background: #4f46e5; color: #fff; text-decoration: none; padding: 14px 40px; border-radius: 7px; font-size: 15px; font-weight: 600; }
    .expire { font-size: 12px; color: #9ca3af; text-align: center; margin-top: 10px; }
    .security { background: #f9fafb; border: 1px solid #e5e7eb; border-radius: 6px; padding: 14px 18px; font-size: 13px; color: #6b7280; margin: 20px 0; line-height: 1.5; }
    .footer { padding: 20px 40px; border-top: 1px solid #f3f4f6; color: #9ca3af; font-size: 12px; text-align: center; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header"><h1>Aegis PMS</h1></div>
    <div class="body">
      <p>Olá, <strong>${userName}</strong>.</p>
      <p>Recebemos uma solicitação para redefinir a senha da sua conta. Clique no botão abaixo para criar uma nova senha.</p>

      <div class="cta">
        <a href="${resetUrl}" class="btn">Redefinir minha senha</a>
      </div>
      <div class="expire">Este link é válido por <strong>1 hora</strong> e expira em <strong>${expiresAt}</strong>.</div>

      <div class="security">
        🔒 Se você não solicitou a recuperação de senha, ignore este e-mail. Sua senha permanece a mesma e nenhuma ação é necessária. Se você continuar recebendo e-mails não solicitados, entre em contato com o suporte.
      </div>
    </div>
    <div class="footer">Aegis PMS · aegis.app<br>Este e-mail foi enviado porque alguém solicitou a recuperação de senha desta conta.</div>
  </div>
</body>
</html>
```

## E. Migração do Flyway

```sql
-- V28__auth_action_tokens.sql
CREATE TABLE auth_action_tokens (
    id              UUID         PRIMARY KEY,
    keycloak_id     VARCHAR(255) NOT NULL,
    user_email      VARCHAR(255) NOT NULL,
    user_name       VARCHAR(255),
    type            VARCHAR(20)  NOT NULL CHECK (type IN ('INVITE', 'PASSWORD_RESET')),
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'USED', 'EXPIRED')),
    tenant_id       UUID,
    tenant_name     VARCHAR(255),
    product_names   TEXT,        -- JSON array: ["Maestro Beton","Portal BYOP"]
    role            VARCHAR(50),
    inviter_name    VARCHAR(255),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at      TIMESTAMPTZ  NOT NULL,
    used_at         TIMESTAMPTZ
);

CREATE INDEX idx_aat_status_expires ON auth_action_tokens (status, expires_at);
CREATE INDEX idx_aat_email_type_created ON auth_action_tokens (user_email, type, created_at);
```

## F. Padrão de qualidade e entrega (obrigatório)

- **Java 25** / **Spring Boot 4.1.x**. 100% de cobertura nas classes funcionais. A chamada à Keycloak Admin API (`PUT .../reset-password`) deve ser testada com `WireMock` — nunca chamar Keycloak real no teste unitário.
- Validação de senha mínima (≥ 8 caracteres, ao menos 1 letra e 1 número): implementar no backend como regra de serviço, **não delegar ao Keycloak** (o Keycloak pode ter políticas de senha configuráveis por realm que divergem da UX do frontend). Se Keycloak rejeitar a senha por outra política, capturar a resposta de erro e propagar como 422 com mensagem amigável.
- Entregar em rodadas:
  1. `AuthActionToken` (entity + repository) + migration `V28__auth_action_tokens.sql` + job de expiração + testes `@DataJpaTest`.
  2. `AuthActionTokenService` (criar, validar, consumir, rate-limit) + testes unitários com mocks.
  3. `AuthActivationService` (chamar Keycloak Admin API para setar senha) + testes `WireMock`.
  4. `AuthController` (endpoints Seção B) + atualização de `UserService`/`ProductAssignmentService` para usar `AuthActionToken` em vez de `executeActionsEmail` + testes `@WebMvcTest`.
  5. Templates `.ftl` (Seção D) + teste de renderização com dados mock.

## G. Critérios de aceite

- [ ] `POST /auth/invite/validate` com token válido retorna os dados do convite (userName, tenantName, productNames, role).
- [ ] `POST /auth/invite/validate` com token expirado retorna 410; com token já usado retorna 400.
- [ ] `POST /auth/activate` com senha válida: chama Keycloak Admin API (verificar via WireMock), marca token como USED, retorna 200.
- [ ] `POST /auth/activate` com senha fraca retorna 422 com mensagem amigável.
- [ ] `POST /auth/reset-password/request` com e-mail inexistente retorna 200 (sem revelar que não existe).
- [ ] `POST /auth/reset-password/request` com 4+ pedidos em 15 min retorna 429 com Retry-After.
- [ ] `POST /auth/reset-password/confirm` com token válido chama Keycloak Admin API e marca token como USED.
- [ ] E-mail de convite recebido no MailHog tem link `http://localhost:5173/invite?token=<uuid>` (não `localhost:8282`).
- [ ] E-mail de reset recebido no MailHog tem link `http://localhost:5173/reset-password?token=<uuid>`.
- [ ] Job de expiração marca tokens PENDING com `expiresAt < now` como EXPIRED (testado com `@SpringBootTest` e clock mockado).
- [ ] `executeActionsEmail` do Keycloak **não é mais chamado** nos fluxos de convite e reset — verificado via ausência de chamada no WireMock das etapas de teste.
- [ ] `mvn clean verify` confirma 100% de cobertura (JaCoCo).

## H. Validação

```bash
# Enviar convite (recebe no MailHog)
curl -X POST http://localhost:8080/api/v1/tenants/<tenantId>/users/invite \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"João Alves","email":"joao@byop.com","role":"Editor"}'
# → Abrir MailHog, copiar o token do link

# Validar token (simula o frontend carregando o InviteScreen)
curl -X POST http://localhost:8080/api/v1/auth/invite/validate \
  -H "Content-Type: application/json" \
  -d '{"token":"<uuid-do-email>"}'

# Ativar conta
curl -X POST http://localhost:8080/api/v1/auth/activate \
  -H "Content-Type: application/json" \
  -d '{"token":"<uuid>","password":"Senha123!"}'

# Reset de senha
curl -X POST http://localhost:8080/api/v1/auth/reset-password/request \
  -H "Content-Type: application/json" \
  -d '{"email":"joao@byop.com"}'

curl -X POST http://localhost:8080/api/v1/auth/reset-password/confirm \
  -H "Content-Type: application/json" \
  -d '{"token":"<uuid-do-email>","password":"NovaSenha456!"}'
```

## I. Commit sugerido

```bash
git add backend/ infra/keycloak/themes/
git commit -m "feat(auth): tokens de ativacao e reset via aegis, sem redirecionar para keycloak"
```
