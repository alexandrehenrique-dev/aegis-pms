# Bug Sprint 03 — Contrato de API, Isolamento Frontend/Backend e Testes E2E

> **Contexto:** Sprint de consolidação arquitetural. Objetivo: garantir que o frontend funciona 100% sem o backend (modo mock), o backend funciona 100% sem o frontend (Bruno + JaCoCo), e os dois funcionam 100% integrados — sem que uma mudança num lado quebre silenciosamente o outro.
>
> **Problema central identificado no BUG-SPRINT-02:** vários componentes React têm dados de negócio hardcoded no JSX, fora do guarda `IS_API_MODE`. Isso faz com que mesmo com o backend perfeito e `IS_API_MODE=true`, a tela exibe dado falso. Adicionalmente, os tipos TypeScript do frontend e os DTOs do backend não têm nenhuma garantia de sincronismo — divergências são descobertas em runtime, não em build time.
>
> **Esta sprint não substitui o BUG-SPRINT-02** — os bugs de produto lá documentados continuam válidos e serão implementados em paralelo ou antes desta sprint. Esta sprint trata da infraestrutura de qualidade que garante que os bugs do SPRINT-02 (e futuros) não reapareçam.
>
> **Arquivo em construção incremental — NÃO modificar durante commits de implementação.**
>
> **Branch sugerida:** `bugfix/sprint-03-contrato-e2e-isolamento`

---

## Diagnóstico de ambiente

```bash
# 1. Backend UP
curl -s http://localhost:8080/actuator/health | jq '.status'
# Esperado: "UP"

# 2. OpenAPI spec disponível
curl -s http://localhost:8080/v3/api-docs | jq '.info.title'
# Esperado: "Aegis PMS API" (ou similar)

# 3. TypeScript sem erros
cd frontend && npm run typecheck
# Esperado: 0 erros

# 4. JaCoCo
cd backend && mvn verify -q
# Esperado: BUILD SUCCESS + jacoco-check PASSED

# 5. Bruno
cd bruno && bru run --env local
# Esperado: 0 FAILED

# 6. Frontend mock mode (sem backend)
# Parar o backend → abrir http://localhost:5173
# Esperado: login funciona, navegação funciona, dados mock visíveis — zero erros no console
```

---

## Seção A — Contrato de API: sincronização de tipos entre backend e frontend

### A.1 — Gerar tipos TypeScript automaticamente a partir do OpenAPI do backend

**Problema:** Os tipos do frontend (`AuthInviteValidationResponse`, `ProductSummary`, `TenantReference`, etc.) são escritos manualmente e não têm garantia de sincronismo com os DTOs do backend. Quando o backend adiciona um campo (ex.: `requiredActions` no `KeycloakUserResponse`, `productSlug` no `AuthInviteValidationResponse`), o frontend não sabe — a divergência aparece em runtime.

**Solução:** Usar `openapi-typescript` para gerar automaticamente os tipos a partir do `springdoc-openapi` do backend.

**Implementação necessária:**

1. Verificar se `springdoc-openapi` já está configurado no backend:
   ```xml
   <!-- pom.xml -->
   <dependency>
     <groupId>org.springdoc</groupId>
     <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
     <version>2.x.x</version>
   </dependency>
   ```
   Endpoint esperado: `GET http://localhost:8080/v3/api-docs`

2. Instalar gerador no frontend:
   ```bash
   cd frontend && npm install -D openapi-typescript
   ```

3. Criar script de geração em `frontend/package.json`:
   ```json
   "scripts": {
     "generate:types": "openapi-typescript http://localhost:8080/v3/api-docs -o src/contracts/api.generated.ts",
     "typecheck": "tsc --noEmit"
   }
   ```

4. Criar `frontend/src/contracts/api.generated.ts` (gerado — nunca editar manualmente).

5. Nos services que hoje têm tipos manuais, importar de `api.generated.ts`:
   ```ts
   // Antes (manual, frágil):
   type AuthInviteValidationResponse = { requiresPasswordSetup: boolean; ... }
   
   // Depois (gerado, sincronizado):
   import type { components } from "../contracts/api.generated";
   type AuthInviteValidationResponse = components["schemas"]["AuthInviteValidationResponse"];
   ```

6. Adicionar ao CI/script de build:
   ```bash
   npm run generate:types && npm run typecheck
   # Se o backend mudou um DTO e o frontend não foi atualizado → tsc falha → detectado em build
   ```

**Critério de aceite:**
- [ ] `GET http://localhost:8080/v3/api-docs` retorna spec OpenAPI válida.
- [ ] `npm run generate:types` gera `api.generated.ts` sem erro.
- [ ] `npm run typecheck` passa com zero erros usando os tipos gerados.
- [ ] Modificar um DTO no backend → regenerar → tsc aponta o arquivo frontend desalinhado.
- [ ] `api.generated.ts` está no `.gitignore` (gerado, não versionado) — OU versionado com script de validação no CI.

---

### A.2 — Audit de isolamento: todo dado variável deve passar pelo service

**Problema:** Componentes React têm strings de negócio hardcoded no JSX que deveriam vir da API ou dos mocks. Exemplos encontrados no BUG-SPRINT-02:
- `AssetPreviewPanel`: `"hero-maestro-beton.jpg"`, `"1920×1080"`, `"2.4 MB"`
- `SettingsOverview`: `"2 usuários com convite pendente."`
- `SecuritySettingsPanel`: `"Integração mock — Sprint 07"`
- `EditorialDashboard`: KPIs hardcoded
- `FormsDashboard`: KPIs hardcoded

**Regra a ser imposta:** Nenhum componente pode conter string, número ou boolean de negócio hardcoded. Dados de negócio = tudo que representa estado real da aplicação (nome, contagem, status, URL, configuração).

**Implementação necessária:**

1. Escrever script de auditoria que varre o frontend e lista violações:
   ```bash
   # scripts/audit-hardcoded-business-data.sh
   # Procura por padrões suspeitos em arquivos .tsx/.ts (fora de mocks/ e *.mocks.ts)
   grep -rn \
     --include="*.tsx" --include="*.ts" \
     --exclude-dir=mocks \
     --exclude="*.mocks.ts" \
     -E '(Maestro Beton|hero-maestro|1920|Sprint [0-9]+|2\.4 MB|hardcod|mock —)' \
     frontend/src/domains/ \
     | grep -v "// " \
     | grep -v "\.mocks\."
   ```

2. O agente deve rodar este script, listar todas as ocorrências e corrigir uma a uma.

3. Cada correção segue o padrão:
   ```tsx
   // ERRADO:
   <h2>hero-maestro-beton.jpg</h2>
   
   // CERTO:
   <h2>{asset.name}</h2>  // asset vem da API ou do mock via service
   ```

**Critério de aceite:**
- [ ] Script de auditoria retorna zero linhas após as correções.
- [ ] tsc --noEmit: zero erros.
- [ ] Frontend em mock mode: telas mostram dados do mock (coerentes, não invenções).
- [ ] Frontend em API mode: telas mostram dados reais.

---

## Seção B — Isolamento: frontend deve rodar sem backend

### B.1 — Garantir que IS_API_MODE=false resulta em UX funcional e coerente

**Problema:** Com o backend parado e `IS_API_MODE=false`, algumas telas quebram ou exibem estados de erro porque:
- Componentes hardcoded (A.2) que dependiam de dado fixo agora buscam de um service que retorna mock — mas o mock pode não ter o dado esperado.
- Rotas de detalhe (ex.: `/assets/:assetId`) esperam um `assetId` que não existe no mock store.

**Implementação necessária:**

1. Criar `frontend/src/infra/__tests__/mockMode.smoke.ts` — arquivo de smoke test de mock mode:
   ```ts
   // Verifica que cada service retorna dado válido em modo mock
   // (sem fazer fetch, sem backend)
   import { assetsService } from "../domains/assets/services/assetsService";
   import { contentService } from "../domains/content/services/contentService";
   import { formsService } from "../domains/forms/services/formsService";
   
   // Em mock mode, todos devem retornar arrays não-vazios sem lançar
   test("assetsService.listAssets retorna mock válido", async () => {
     const result = await assetsService.listAssets("mock-product-id");
     expect(result).toBeDefined();
     expect(Array.isArray(result)).toBe(true);
   });
   ```

2. Garantir que os mocks têm dados coerentes para os assets, conteúdos e forms criados durante os testes manuais.

3. Rotas de detalhe em mock mode devem funcionar com IDs dos mocks:
   - `/assets/mock-asset-1` → `assetsService.getAsset("p1", "mock-asset-1")` retorna dado do mock store.

**Critério de aceite:**
- [ ] Backend parado + `IS_API_MODE=false` → login funciona, dashboard carrega, assets listam, conteúdo lista.
- [ ] Nenhum erro de console em mock mode.
- [ ] Rotas de detalhe funcionam com IDs do mock store.

---

### B.2 — Mock store de assets: dados coerentes por produto

**Problema:** O mock store de assets (`assets.mocks.ts`) tem dados genéricos que não correspondem aos produtos reais (Maestro Beton, Conecta Talentos, WikiDev). Quando o frontend filtra por `productId`, o mock retorna vazio ou dados errados.

**Implementação necessária:**

1. Reestruturar `assets.mocks.ts` no padrão já usado por `content.mocks.ts` (que tem `contentByProduct`):
   ```ts
   export const assetsByProduct: Record<string, AssetRow[]> = {
     "maestro-beton": [
       { id: "a1", name: "hero-maestro-beton.jpg", type: "image", size: "2.4 MB", status: "ativo", ... },
       { id: "a2", name: "release-institucional.pdf", type: "pdf", size: "800 KB", status: "ativo", ... },
     ],
     "conecta-talentos": [
       { id: "a3", name: "logo-conecta.png", type: "image", size: "120 KB", status: "ativo", ... },
     ],
   };
   ```

2. `assetsService.listAssets(productId)` em modo mock: retorna `assetsByProduct[slugify(productId)] ?? []`.

3. `assetsService.getAsset(productId, assetId)` em modo mock: busca por `id` no store do produto.

**Critério de aceite:**
- [ ] Produto Maestro Beton em mock mode → Assets lista 2-3 assets reais (não genéricos).
- [ ] Produto Conecta Talentos → lista assets desse produto.
- [ ] Clicar em asset → AssetDetail exibe dados do mock (não hardcoded).

---

## Seção C — Isolamento: backend deve ser testável sem frontend

### C.1 — Collection Bruno de contrato: validar shape dos responses

**Problema:** As collections Bruno existentes testam status HTTP (200, 400, 404) mas não validam o shape do response body. Se o backend renomear um campo (`productSlug` → `slug`), o Bruno passa mas o frontend quebra.

**Implementação necessária:**

1. Para cada endpoint crítico, adicionar assertions de schema no Bruno:
   ```js
   // bruno/auth/validate-invite.bru — seção tests:
   test("response tem os campos obrigatórios", function() {
     const body = res.getBody();
     expect(body).toHaveProperty("userName");
     expect(body).toHaveProperty("userEmail");
     expect(body).toHaveProperty("requiresPasswordSetup");
     expect(body).toHaveProperty("productSlug");
     expect(typeof body.requiresPasswordSetup).toBe("boolean");
   });
   ```

2. Endpoints críticos a cobrir (mínimo):
   - `POST /auth/invite/validate`
   - `POST /auth/activate`
   - `POST /auth/invite/accept-existing`
   - `GET /products/{id}`
   - `GET /products/{id}/assets`
   - `GET /tenants/{id}/users`
   - `POST /tenants/{id}/users/invite`

3. Rodar: `bru run --env local` deve validar shapes, não só status.

**Critério de aceite:**
- [ ] Todas as collections críticas têm assertions de shape, não só de status.
- [ ] Renomear campo no DTO → Bruno falha → desenvolvedor percebe antes de fazer merge.
- [ ] `bru run --env local`: 0 failed após todas as assertions adicionadas.

---

### C.2 — Documentação OpenAPI: anotações `@Schema` nos DTOs críticos

**Problema:** O `springdoc-openapi` gera spec automaticamente mas os campos não têm descrições, exemplos ou constraints documentados. O tipo gerado para o frontend é correto mas sem semântica.

**Implementação necessária:**

Adicionar `@Schema` nos DTOs que geram tipos usados pelo frontend:

```java
// AuthInviteValidationResponse.java
public record AuthInviteValidationResponse(
    @Schema(description = "Nome do usuário convidado") String userName,
    @Schema(description = "Email do usuário convidado") String userEmail,
    @Schema(description = "Nome do tenant") String tenantName,
    @Schema(description = "Produtos ao qual o convite dá acesso") List<String> productNames,
    @Schema(description = "Slug do produto principal do convite", nullable = true) String productSlug,
    @Schema(description = "Papel atribuído ao usuário no produto") String role,
    @Schema(description = "Nome de quem enviou o convite") String inviterName,
    @Schema(description = "Data de expiração do token") Instant expiresAt,
    @Schema(description = "true = usuário novo, deve definir senha; false = usuário existente, só confirmar acesso")
    boolean requiresPasswordSetup
) {}
```

Prioridade: DTOs usados no fluxo de convite, produtos e assets.

**Critério de aceite:**
- [ ] `GET /v3/api-docs` retorna descriptions nos campos dos DTOs críticos.
- [ ] Swagger UI (`/swagger-ui`) mostra descriptions e exemplos.
- [ ] `npm run generate:types` gera tipos com JSDoc preservado.

---

## Seção D — Testes E2E: fluxos críticos automatizados

### D.1 — Setup Playwright para smoke tests E2E

**Problema:** Não existe nenhum teste que valide o sistema integrado (frontend + backend + Keycloak + MailHog) de ponta a ponta. Regressões de integração são descobertas pelo PO na mão.

**Implementação necessária:**

1. Instalar Playwright:
   ```bash
   cd frontend && npm install -D @playwright/test
   npx playwright install chromium
   ```

2. Criar `frontend/e2e/` com configuração base:
   ```ts
   // frontend/playwright.config.ts
   export default {
     testDir: "./e2e",
     baseURL: "http://localhost:5173",
     use: { headless: true },
     projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
   };
   ```

3. Adicionar script em `package.json`:
   ```json
   "e2e": "playwright test",
   "e2e:ui": "playwright test --ui"
   ```

**Critério de aceite:**
- [ ] `npm run e2e` executa sem erro de configuração.
- [ ] Ambiente local com backend UP → testes rodam.

---

### D.2 — Smoke test E2E: fluxo de login e navegação básica

```ts
// frontend/e2e/smoke.login.spec.ts
test("super-admin faz login e vê o dashboard", async ({ page }) => {
  await page.goto("/login");
  await page.fill('[name="email"]', "super-admin@byop.io");
  await page.fill('[name="password"]', "senha-local");
  await page.click('button[type="submit"]');
  await expect(page).toHaveURL(/dashboard/);
  await expect(page.locator("text=Aegis PMS")).toBeVisible();
});

test("sidebar exibe produto ativo", async ({ page }) => {
  // login...
  await expect(page.locator("text=Maestro Beton")).toBeVisible();
});
```

**Critério de aceite:**
- [ ] Login com super-admin → dashboard visível.
- [ ] Login com credenciais erradas → mensagem de erro visível.
- [ ] Sidebar mostra produto do tenant.

---

### D.3 — Smoke test E2E: fluxo de convite completo

**Este é o fluxo que mais quebrou nos testes manuais (BUG-SPRINT-02, Seção F).**

```ts
// frontend/e2e/smoke.invite.spec.ts
test("convite para produto correto chega no MailHog com dados certos", async ({ page, request }) => {
  // 1. Login como super-admin
  // 2. Navegar para /users/invite
  // 3. Preencher form: email=novo@teste.com, role=editor, produto=Conecta Talentos
  // 4. Submeter
  // 5. Verificar toast "Convite enviado!"
  // 6. Consultar MailHog API
  const mailhog = await request.get("http://localhost:8025/api/v2/messages");
  const messages = await mailhog.json();
  const conviteEmail = messages.items.find(m => m.To[0].Mailbox === "novo");
  expect(conviteEmail).toBeDefined();
  expect(conviteEmail.Content.Body).toContain("Conecta Talentos");
  expect(conviteEmail.Content.Body).not.toContain("Maestro Beton");
});

test("novo usuário convidado vê tela de criar senha", async ({ page }) => {
  // Pegar URL do email no MailHog
  // Navegar para a URL do convite
  // Verificar que aparece form de criar senha (não "você já tem conta")
});
```

**Critério de aceite:**
- [ ] Teste de convite: email chega com produto correto.
- [ ] Teste de convite: novo usuário vê form de senha.
- [ ] Teste de convite: usuário existente vê "você já tem conta".

---

### D.4 — Smoke test E2E: fluxo de assets

```ts
// frontend/e2e/smoke.assets.spec.ts
test("upload de asset e download com nome correto", async ({ page }) => {
  // 1. Login → Maestro Beton → Assets
  // 2. Clicar "Upload de asset"
  // 3. Fazer upload de arquivo teste.pdf
  // 4. Verificar que aparece na biblioteca
  // 5. Clicar no asset → verificar que AssetDetail exibe "teste.pdf"
  // 6. Clicar "Baixar" → verificar download com nome "teste.pdf"
});

test("AssetDetail reflete tipo real do asset", async ({ page }) => {
  // Abrir asset de imagem → badge "imagem" visível
  // Abrir asset de PDF → badge "pdf" visível, não "imagem"
});
```

---

## Seção E — Bugs adicionais (a ser preenchido incrementalmente)

> Esta seção será preenchida pelo PO conforme os testes de integração avançam.
> Cada bug segue o formato: Sintoma → Causa raiz → Fix → Critério de aceite → Smoke test.

---

### E.1 — Fluxo de ativação de conta não coleta nome e sobrenome exigidos pelo Keycloak

**Sintoma:** Ao ativar uma conta via link de convite, o Keycloak exige `firstName` e `lastName` para considerar o perfil completo. O modal de ativação atual só possui campos de senha e confirmação de senha. O usuário consegue definir a senha mas o perfil fica incompleto no Keycloak, podendo causar erros em operações futuras que dependem do nome (ex.: envio de email, exibição no sistema).

**Fluxos afetados (o agente deve mapear todos antes de implementar):**
1. `InviteScreen.tsx` — ativação via link de convite (usuário novo, `requiresPasswordSetup: true`)
2. `ConfirmPasswordReset` — redefinição de senha (verificar se também exige nome)
3. Qualquer outro fluxo que chame `authActivationService.activateAccount()` ou `keycloakAdminClient.setPassword()`

**Investigação necessária:**
1. Verificar no Keycloak Admin quais campos são obrigatórios para o perfil do usuário no realm `aegis`.
2. Verificar se `PUT /admin/realms/aegis/users/{id}` é chamado com `firstName` e `lastName` no momento da ativação — se não, o perfil fica incompleto.
3. Verificar o backend: `AuthActivationService.activate()` — ele atualiza o perfil além de definir a senha?

**Fix necessário:**

Backend:
1. `AuthActivationService.activate()` deve receber `firstName` e `lastName` além de `password`.
2. Após definir a senha no Keycloak, chamar `keycloakAdminClient.updateUserProfile(keycloakId, firstName, lastName)`.
3. DTO `AuthActivateRequest` deve incluir os novos campos com validação (`@NotBlank`, `@Size(max=100)`).
4. Novo endpoint (ou ajuste do existente): `POST /auth/activate` aceita `{ token, password, firstName, lastName }`.

Frontend — `InviteScreen.tsx`:
1. Adicionar campos "Nome" e "Sobrenome" acima dos campos de senha no formulário de ativação.
2. Validação: ambos obrigatórios, mínimo 2 caracteres.
3. Pré-preencher com `invite.userName` se o backend já retornar o nome (separar em `firstName`/`lastName`).
4. `authActivationService.activateAccount()` deve passar `{ token, password, firstName, lastName }`.

Frontend — outros fluxos:
- Verificar `ConfirmPasswordReset` — se o Keycloak também exige nome neste fluxo, adicionar os campos.
- Verificar se há outros pontos de ativação no código.

**Novo contrato do endpoint:**
```json
POST /auth/activate
{
  "token": "uuid",
  "password": "senha123",
  "firstName": "Alexandre",
  "lastName": "Silva"
}
```

**Critério de aceite:**
- [ ] InviteScreen exibe campos Nome, Sobrenome, Senha, Confirmar Senha.
- [ ] Submeter com nome vazio → erro de validação visível no campo.
- [ ] Ativar conta → perfil no Keycloak tem `firstName` e `lastName` preenchidos.
- [ ] Email de boas-vindas (se existir) usa o nome real, não placeholder.
- [ ] tsc --noEmit: zero erros.
- [ ] JaCoCo: `AuthActivationService.activate()` coberto 100% com os novos campos.
- [ ] Bruno: `POST /auth/activate` com e sem `firstName` — validar 200 e 400.

**Smoke test:**
```
1. Enviar convite para novo email
2. Abrir link do email no MailHog
3. Verificar que formulário exibe 4 campos: Nome, Sobrenome, Senha, Confirmar Senha
4. Preencher todos → Ativar conta
5. Login com a conta ativada
6. Verificar no Keycloak Admin: firstName e lastName preenchidos corretamente
7. Sidebar do sistema exibe o nome real do usuário
```

---

### E.2 — Super Admin não consegue remover ou bloquear usuário; sem notificação por email

**Sintoma:** Na tela de usuários (`UserTable`), o Super Admin não tem ação para bloquear ou remover um usuário do tenant ou da plataforma. Ações disponíveis são apenas "Abrir" e "Permissões". Falta operação crítica de gestão de acesso.

**Escopo das ações necessárias (dois níveis):**

**Bloquear usuário** (reversível):
- Desabilita o usuário no Keycloak (`enabled: false`) — impede login imediato.
- Mantém todos os dados, assignments e memberships intactos.
- Status na tabela muda para "bloqueado".
- Email enviado ao usuário: "Seu acesso à plataforma foi temporariamente suspenso."
- Super Admin (ou Tenant Admin com permissão) pode reverter.

**Remover usuário do tenant** (permanente no tenant, reversível na plataforma):
- Remove `TenantMembership` e todos os `ProductAssignment` do tenant.
- NÃO remove o usuário do Keycloak (pode ter acesso a outros tenants).
- Email enviado: "Seu acesso ao tenant [Nome] foi revogado."
- Ação irreversível sem novo convite.

**Implementação necessária:**

Backend:
1. `PATCH /tenants/{tenantId}/users/{userId}/block` — bloqueia usuário (Keycloak `enabled=false` + email).
2. `DELETE /tenants/{tenantId}/users/{userId}` — remove membership + assignments + email.
3. Verificar permissões: apenas `SUPER_ADMIN`, `TENANT_ADMIN` e `PRODUCT_MANAGER` podem executar estas ações (ver escopo abaixo).
4. Nenhum usuário pode bloquear/remover a si mesmo.
5. Templates de email (já criados em `infra/keycloak/themes/aegis/email/html/`):
   - `userBlocked.ftl` — notificação de bloqueio com status "Conta suspensa" (laranja), campo opcional de motivo via `${reason}`, e link de suporte via `${supportEmail}`. Variáveis: `${userName}`, `${tenantName}`, `${reason}` (opcional), `${supportEmail}`.
   - `userRemoved.ftl` — notificação de remoção permanente do workspace com card vermelho. Variáveis: `${userName}`, `${tenantName}`, `${supportEmail}`.
6. Ambas as ações geram evento de auditoria (`USER_BLOCKED`, `USER_REMOVED_FROM_TENANT`).
7. JaCoCo 100% nos novos services/controllers.

Frontend:
1. `UserTable`: adicionar botão de ação expandido (dropdown "⋮") com:
   - "Bloquear acesso" — abre dialog de confirmação com motivo opcional.
   - "Remover do tenant" — abre dialog de confirmação com aviso de irreversibilidade.
2. Dialog de confirmação de bloqueio:
   - Texto: "Isso impedirá o login imediatamente. O usuário receberá um email de notificação."
   - Campo opcional: "Motivo (visível apenas para admins)".
   - Botões: "Cancelar" / "Bloquear usuário" (vermelho).
3. Dialog de confirmação de remoção:
   - Texto: "Esta ação é irreversível. O usuário perderá acesso a todos os produtos deste tenant."
   - Digitação do email para confirmar (UX de segurança).
   - Botões: "Cancelar" / "Remover permanentemente" (vermelho).
4. Após ação bem-sucedida: toast + atualizar lista de usuários.
5. Usuário bloqueado na tabela: badge "bloqueado" (vermelho) na coluna Status + ação "Desbloquear".

**Permissões:**
- Super Admin: pode bloquear/remover qualquer usuário de qualquer tenant.
- Tenant Admin: pode bloquear/remover usuários do seu tenant (exceto outros Tenant Admins e Super Admins).
- Product Manager: pode bloquear/remover usuários atribuídos ao(s) produto(s) que gerencia (exceto Tenant Admins e Super Admins).
- Editor: sem acesso a estas ações.
- Ninguém bloqueia a si mesmo.

**Critério de aceite:**
- [ ] Super Admin vê dropdown "⋮" em cada linha da UserTable com opções Bloquear e Remover.
- [ ] Bloquear usuário → dialog de confirmação → confirmar → usuário fica com status "bloqueado" → email chega no MailHog.
- [ ] Usuário bloqueado tenta login → Keycloak rejeita → tela de erro no frontend.
- [ ] Remover usuário → dialog com digitação de email → confirmar → usuário some da tabela → email chega.
- [ ] Usuário removido tenta login → consegue (conta existe no Keycloak) mas não tem tenants para acessar.
- [ ] Auditoria: ambas as ações aparecem no AuditTimeline com actor e detalhes.
- [ ] JaCoCo: novos métodos cobertos 100%.
- [ ] Bruno: `PATCH /block` e `DELETE` com 200, 403 (sem permissão), 404 (usuário não encontrado).
- [ ] tsc --noEmit: zero erros.

**Smoke test:**
```
1. Login: super-admin → CLIENTES BETA → Usuários
2. Linha "Alexandre Teste" → ⋮ → "Bloquear acesso"
3. Confirmar no dialog
4. Verificar: badge "bloqueado" na tabela
5. Verificar MailHog: email de bloqueio chegou para alexandre.henrique@byop.io
6. Tentar login como Alexandre Teste → deve falhar com mensagem de conta suspensa
7. Voltar como super-admin → Desbloquear
8. Login como Alexandre Teste → deve funcionar normalmente
9. AuditTimeline → verificar eventos USER_BLOCKED e USER_UNBLOCKED com detalhes corretos
```

---

### E.3 — Seed não popula conteúdos dos produtos (pages, blocos, assets)

**Sintoma:** Os produtos criados pela seed de desenvolvimento (ex.: Maestro Beton, Conecta Talentos) aparecem corretamente na listagem e no sidebar, mas não possuem nenhum conteúdo associado — pages, blocos de conteúdo, assets e demais dados de produto estão ausentes. O produto existe mas está vazio.

**Impacto:** Impossível validar fluxos de Conteúdo, Assets e demais módulos de produto sem criar conteúdo manualmente a cada reset de ambiente. Aumenta fricção nos testes de integração e de smoke.

**Causa provável:** A seed atual (`DataSeeder` ou equivalente) cria apenas as entidades de nível de tenant/produto (`Tenant`, `Product`, `TenantMembership`, `ProductAssignment`) mas não semeia dados filhos: `Page`, `Block`, `Asset`, etc.

**Implementação necessária:**

1. Auditar o `DataSeeder` e identificar quais entidades filho existem no domínio mas não são criadas:
   - `Page` (com `slug`, `title`, `status: PUBLISHED`)
   - `Block` (vinculado a uma `Page`, com `type` e `content` mínimos)
   - `Asset` (com `name`, `type: image`, `status: ativo`, URL de placeholder)
   - Qualquer outra entidade que popule módulos visíveis no produto

2. Para cada produto da seed, criar ao menos:
   - 2–3 Pages com status PUBLISHED
   - 1–2 Blocks por Page (ex.: um bloco de texto e um bloco de imagem)
   - 2–3 Assets (imagens de placeholder referenciáveis nos blocos)

3. Garantir que a seed seja idempotente: verificar existência antes de inserir (para não duplicar em re-execuções).

4. Verificar que os dados da seed aparecem corretamente no frontend em modo `IS_API_MODE=true`.

**Critério de aceite:**
- [ ] Após rodar a seed, cada produto tem ao menos 2 Pages visíveis na aba Conteúdo.
- [ ] Cada Page tem ao menos 1 Block renderizado no editor.
- [ ] A aba Assets exibe ao menos 2–3 assets por produto.
- [ ] Re-executar a seed não duplica os dados.
- [ ] Frontend em `IS_API_MODE=true`: navegar para Maestro Beton → Conteúdo → lista de pages não está vazia.

**Smoke test:**
```
1. Reset do banco → ./scripts/seed.sh (ou mvn spring-boot:run com seed ativa)
2. Login como super-admin → selecionar Maestro Beton
3. Menu → Conteúdo → verificar lista de pages (esperado: ≥2 páginas visíveis)
4. Clicar em uma page → verificar blocos renderizados
5. Menu → Assets → verificar lista de assets (esperado: ≥2 assets)
6. Repetir para Conecta Talentos
```

<!-- PRÓXIMOS BUGS: inserir abaixo desta linha -->

---

## E.4 — Jornada Páginas × Conteúdo: separação correta, UX ausente e dashboard hardcoded

**Módulos afetados:** `frontend/src/domains/content/pages/EditorialDashboard.tsx`, `contentService.ts`, `pagesService.ts`

**Comportamento observado pelo usuário:**
- Páginas criadas em `Páginas` não aparecem em `Conteúdo`.
- Conteúdo criado em `Conteúdo` não aparece em `Páginas`.
- A aba `Workflow` aparece vazia, sem itens.

**Diagnóstico — separação arquitetural é INTENCIONAL:**

Os dois domínios são distintos por design (documentado em comentários do código):

| Domínio | Entidade | Serviço | Workflow | Tabela |
|---|---|---|---|---|
| `Páginas` | `Page` (páginas institucionais: home, sobre, contato) | `pagesService` | Nenhum — status simples: `draft\|review\|published\|archived` | `pages` |
| `Conteúdo` | `Content` (artigos editoriais com ciclo de vida completo) | `contentService` | `DRAFT→IN_REVIEW→PUBLISHED→ARCHIVED` | `contents` |

Criar de um módulo e não aparecer no outro = **comportamento correto**. O problema é a ausência de comunicação clara ao usuário sobre esta distinção.

**Bugs reais encontrados na auditoria:**

**E.4.1 — `EditorialDashboard`: card "Estados globais do módulo" sempre hardcoded**

Arquivo: `frontend/src/domains/content/pages/EditorialDashboard.tsx`

O card "Estados globais do módulo" no dashboard editorial exibe **sempre** os seguintes estados fixos, independentemente de dados reais:
```tsx
// Hardcoded — nunca reflete dados reais:
<EmptyState compact title="Sem conteúdo" description="Crie o primeiro item editorial do produto." />
<div><p className="mb-2 text-sm font-medium">Loading</p><SkeletonLines /></div>
<PermissionHint />
```
Os KPI widgets acima (`rows`) derivam dados reais corretamente. O card de estados é placeholder nunca substituído.

**E.4.2 — `EditorialAttentionCard`: alertas hardcoded com "Maestro"**

O painel de atenções exibe alertas fixos referenciando "Maestro Beton" para qualquer produto:
```tsx
{["Página Home possui SEO incompleto.", "Sobre o Maestro está aguardando revisão.", ...].map(...)}
```
Deve vir do backend ou ser removido até haver endpoint real.

**E.4.3 — Workflow vazio: causa raiz é E.3 (seed não popula `Content`)**

O backend `GET /products/{productId}/content/workflow-items` existe e funciona. O endpoint `WorkflowBoard` chama-o corretamente. A lista vem vazia porque o seed não cria entidades `Content` (ver E.3). Em mock mode, `wfInitialItems` são todos "Maestro Beton" — filtro por produto não existe.

**E.4.4 — Ausência de UX diferenciadora entre Páginas e Conteúdo**

Nenhuma tela explica ao usuário a diferença entre os dois domínios. Risco de confusão recorrente.

**Implementação necessária:**

1. **E.4.1** — Substituir o card hardcoded por derivação real de `rows` (contagem por `ContentStatus`):
   ```tsx
   const byStatus = rows.reduce((acc, r) => { acc[r.status] = (acc[r.status] ?? 0) + 1; return acc; }, {} as Record<string, number>);
   // Renderizar: Draft, In Review, Published, Archived com counts reais
   ```

2. **E.4.2** — Remover `EditorialAttentionCard` ou criar endpoint `GET /products/{productId}/content/attention-items`.

3. **E.4.3** — Resolvido junto com E.3 (seed popula `Content`).

4. **E.4.4** — Adicionar tooltip/banner explicativo na primeira visita a cada módulo: "Páginas são a estrutura do seu site. Conteúdo são artigos e publicações editoriais."

**Critério de aceite:**
- [ ] Card de estados globais exibe contagens reais por status de Content.
- [ ] Workflow exibe itens reais após E.3 ser corrigido.
- [ ] Zero referência a "Maestro Beton" em alertas visíveis ao usuário.

---

## E.5 — Formulários: `FormBuilder` não persiste form criado + filtro mock com slug errado

**Módulos afetados:** `frontend/src/domains/forms/pages/FormBuilder.tsx`, `formsService.ts`, `FormsDashboard.tsx`

**Comportamento observado:** Usuário cria um formulário, mas ele não aparece na lista de formulários. Parece que não foi salvo.

**Diagnóstico — causa raiz (modo API):**

O `FormBuilder` é montado na rota `/forms/new` sem um `formId` no parâmetro de URL:

```tsx
// FormBuilder.tsx
const { slug: formId } = useParams<{ slug: string }>(); // undefined em /forms/new

const handleSaveDraft = async () => {
  if (formId && productId) await formsService.saveFormFields(productId, formId, fields); // pulado!
  await formsService.saveDraft(productId, formId); // formId = undefined
};

const handlePublish = async () => {
  await formsService.publish(productId, formId); // formId = undefined → noop no API mode
};
```

`formsService.saveDraft` e `formsService.publish` em API mode ambos verificam `if (IS_API_MODE && formId)` — quando `formId` é `undefined`, **nenhuma chamada API é feita**.

**`formsService.createForm()` nunca é chamado pelo `FormBuilder`** — o formulário nunca é criado no backend. A rota `/forms/new` abre o builder sem primeiro criar o recurso no servidor.

**Diagnóstico — causa raiz (modo mock):**

```typescript
// formsService.ts — listForms
const productSlug = currentProductSlugOrId(); // retorna UUID (ex: "p1")
return formsStore.filter((f) => f.productSlug === productSlug);
// Mock store tem productSlug = "maestro-beton" → filter retorna []
```

`currentProductSlugOrId()` retorna o ID do produto (`"p1"`, `"p2"`, etc.), mas os mocks têm `productSlug: "maestro-beton"`. O filtro nunca encontra correspondência.

**E.5.1 — Bugs adicionais identificados:**

- `FormsDashboard` card "Estados previstos" também hardcoded (mesmo padrão do E.4.1).
- Não há rota `/forms/:formId/builder` para editar formulários existentes — o link da `FormsList` navega para `/forms/new` sempre que cria, e não há deep-link para editar.

**Implementação necessária:**

1. **Fluxo de criação** — Antes de abrir o `FormBuilder`, chamar `formsService.createForm()` e redirecionar para `/forms/:newFormId/builder`:
   ```tsx
   // FormsList / FormsDashboard — ao clicar "Novo formulário":
   const created = await formsService.createForm(productId, { name: "Novo formulário", type: "contato" });
   navigate(`/forms/${created.id}/builder`);
   ```

2. **Adicionar rota** em `index.tsx`:
   ```tsx
   <Route path="/forms/:slug/builder" element={<FormBuilder />} />
   // Manter /forms/new apenas como redirect ou remover
   ```

3. **Corrigir filtro mock** — `listForms` deve comparar por ID do produto, não slug:
   ```typescript
   return formsStore.filter((f) => f.productSlug === (currentProductSlugOrId() ?? productId) || f.productSlug === productId);
   ```

4. **Card "Estados previstos"** — derivar de `formsData` assim como os KPIs.

**Critério de aceite:**
- [ ] Após criar formulário, ele aparece na lista de formulários com status "Rascunho".
- [ ] Editar formulário existente via FormBuilder persiste campos corretamente.
- [ ] Em mock mode, formulários do produto ativo aparecem na lista.
- [ ] Card de estados no `FormsDashboard` exibe contagens reais.

**Smoke test:**
```
1. Clicar "Novo formulário" → deve criar form no backend e redirecionar para /forms/:id/builder
2. Adicionar 2 campos → "Salvar rascunho" → navegar para /forms/list
3. Verificar que o formulário aparece na lista com status "Rascunho"
4. Clicar "Publicar" → status muda para "Publicado"
```

---

## E.6 — Eventos: salvar falha silenciosamente (contrato + error handling + imageAssetId)

**Módulos afetados:** `frontend/src/domains/pages/components/EventsManagerDrawer.tsx`, `eventsService.ts`, `backend/.../pages/contract/CreateEventRequest.java`

**Comportamento observado:** Usuário preenche o formulário de evento (Título, Data, Descrição), clica "Salvar evento" — nada acontece. O evento não aparece na lista. Também não é possível fazer upload de imagem pelo painel.

**Diagnóstico — 3 causas raiz identificadas:**

**E.6.1 — `@NotBlank location` no backend vs campo opcional no frontend**

O backend exige `location` como obrigatório:
```java
// CreateEventRequest.java
public record CreateEventRequest(
    @NotBlank String title,
    @NotNull LocalDateTime datetime,
    @NotBlank String location,  // ← obrigatório!
    ...
) {}
```

Mas o frontend não valida `location` — o botão "Salvar evento" só bloqueia se `title` estiver vazio:
```tsx
// EventForm — botão habilitado mesmo com location vazia:
<Button primary onClick={() => onSave(draft)} disabled={saving || !draft.title.trim()}>
```

Se o usuário deixar "Local" em branco (como mostrado na screenshot), o backend retorna 400. O frontend não vê o erro porque:

**E.6.2 — `handleSave` sem `catch` → erro silencioso**

```typescript
// EventsManagerDrawer.tsx
const handleSave = async (req: CreateEventRequest) => {
  setSaving(true);
  try {
    await eventsService.createEvent(productSlug, req); // lança exceção 400
    toast.success("Evento criado", { description: req.title }); // nunca executado
    setEditing(null);
    refresh();
  } finally {
    setSaving(false); // só executa o finally — nenhuma mensagem de erro
  }
};
```

Sem bloco `catch`, a exceção é silenciada. O usuário vê o botão parar de girar mas nenhum feedback de erro.

**E.6.3 — `imageAssetId`: tipo errado (filename string em vez de UUID)**

`MediaField` retorna `asset.name` (nome do arquivo, ex: `"foto-evento.jpg"`) ao selecionar um asset. O `eventsService.toEventDto` passa esse valor como `imageAssetId`:

```typescript
function toEventDto(req) {
  return { ..., imageAssetId: req.image || undefined }; // "foto-evento.jpg" — não é UUID!
}
```

O backend espera `UUID imageAssetId`. Spring rejeita a deserialização se o valor não for um UUID válido → 400 mesmo com todos os outros campos corretos.

A `MediaField` foi projetada para retornar o nome do asset para exibição, mas o evento precisa do ID do asset. É necessário usar `onSelectAsset` (que fornece o `AssetSummary` completo) em vez de `onChange` para capturar o `asset.id`.

**Implementação necessária:**

1. **E.6.1** — Tornar `location` opcional no backend (remover `@NotBlank`), ou adicionar validação no frontend + mensagem de erro ao usuário:
   ```tsx
   // Opção A — validação no frontend (recomendado para UX):
   <Button primary onClick={() => onSave(draft)}
     disabled={saving || !draft.title.trim() || !draft.location.trim()}>
   // + Field de Local com required visual
   ```

2. **E.6.2** — Adicionar `catch` no `handleSave`:
   ```typescript
   const handleSave = async (req: CreateEventRequest) => {
     setSaving(true);
     try {
       if (editing && editing !== "new") {
         await eventsService.updateEvent(productSlug, editing.id, req);
         toast.success("Evento atualizado", { description: req.title });
       } else {
         await eventsService.createEvent(productSlug, req);
         toast.success("Evento criado", { description: req.title });
       }
       setEditing(null);
       refresh();
     } catch (err: unknown) {
       const msg = (err as { message?: string }).message ?? "Erro ao salvar evento.";
       toast.error("Falha ao salvar", { description: msg });
     } finally {
       setSaving(false);
     }
   };
   ```

3. **E.6.3** — Capturar `asset.id` (UUID) em vez de `asset.name` para `imageAssetId`:
   ```tsx
   // EventForm — patch deve guardar o ID do asset, não o nome:
   <MediaField
     label="Foto do evento"
     value={draft.image ?? ""}
     typeFilter="imagem"
     onChange={(name) => patch({ image: name })}          // manter para exibição
     onSelectAsset={(asset) => patch({ imageAssetId: asset.id, image: asset.name })}
   />
   ```
   E ajustar `CreateEventRequest` frontend para incluir `imageAssetId?: string` separado de `image`.

**Critério de aceite:**
- [ ] Evento com "Local" vazio → mensagem de validação visível, não envio silencioso.
- [ ] Evento com erro de backend → toast de erro com mensagem descritiva.
- [ ] Selecionar imagem via AssetPicker → `imageAssetId` enviado ao backend como UUID válido.
- [ ] Evento criado aparece na lista após salvar.
- [ ] Bruno: `POST /products/{productId}/events` com `location` vazia → 400 com `violations`.

**Smoke test:**
```
1. Editor de Páginas → bloco event-list → "Gerenciar eventos"
2. Preencher apenas Título e Descrição (Local vazio) → "Salvar evento"
3. Esperado: mensagem de validação visível (não silêncio)
4. Preencher Local → "Salvar evento"
5. Esperado: toast "Evento criado" e evento aparece na lista
6. Selecionar imagem via "Selecionar imagem" → salvar
7. Esperado: backend recebe UUID do asset, não filename
```

---

## E.7 — event-list: evento selecionado não aparece no preview (BlockRenderer placeholder + debounce não flushado)

**Módulos afetados:** `frontend/src/domains/pages/components/BlockRenderer.tsx`, `frontend/src/domains/pages/pages/PageEditor.tsx`

**Comportamento observado:** Usuário cria evento, seleciona-o na lista do editor (checkmark visível), clica "Salvar rascunho", abre Preview — preview exibe "Nenhum evento selecionado ainda (ver editor)."

**Diagnóstico — duas causas raiz independentes:**

**E.7.1 — "Salvar rascunho" é cosmético: sem rota real, sem flush de debounce**

`PageEditor` tem dois mecanismos de persistência separados que **não se comunicam**:

- **Debounce de conteúdo** (`handleChangeContent`): persiste patches de blocos `CONTENT_SAVE_DEBOUNCE_MS` após a última interação via `pagesService.updateSection`. É este mecanismo que persiste `selectedEventIds`.
- **`triggerSave()`** (botão "Salvar rascunho"): atualiza apenas o indicador visual de status (`saveStatus`). **Nunca chama nenhum serviço** — não flusheia o debounce, não persiste a página, não chama o backend.

```typescript
// PageEditor.tsx — triggerSave() é 100% cosmético:
const triggerSave = () => {
  setSaveStatus("dirty");
  if (saveTimer.current) clearTimeout(saveTimer.current);
  saveTimer.current = setTimeout(() => {
    setSaveStatus("saving");
    setTimeout(() => { setSaveStatus("saved"); ... }, 1100);
  }, 1800);
};
```

O botão "Preview" navega para `/content/${page?.slug}/preview` (rota separada), que busca dados frescos do backend. Se o debounce não disparou, `selectedEventIds` não está persistido → preview mostra lista vazia.

O toast "Salvo automaticamente" visível na screenshot = indicador visual de `triggerSave()`, **não** uma confirmação de persistência real.

**Jornada correta para "Salvar rascunho":**

A rota real já existe no backend (`PUT /api/v1/products/{productId}/pages/{pageId}`) e `pagesService.updatePage()` já a chama em modo API. O botão simplesmente nunca chegou a usá-la. A jornada completa deve ser:

```
Usuário clica "Salvar rascunho"
  │
  ├─ 1. Flush imediato do debounce de conteúdo
  │       → cancelar timer pendente
  │       → se há patch pendente: pagesService.updateSection(pageId, sectionId, { content })
  │
  ├─ 2. Persistir a página como rascunho
  │       → pagesService.updatePage(productSlug, pageId, { status: "draft" })
  │       → PUT /api/v1/products/{productId}/pages/{pageId} com payload completo
  │
  ├─ 3. Atualizar estado local com a resposta do backend
  │       → refreshPage(page) — fonte da verdade é o backend
  │
  └─ 4. Feedback real ao usuário
          → toast.success("Rascunho salvo") em caso de sucesso
          → toast.error("Falha ao salvar", { description: err.message }) em caso de erro
          → setSaveStatus("saved") apenas após confirmação do backend
```

**Implementação:**

```typescript
// PageEditor.tsx — handleSaveDraft substitui triggerSave no botão:
const handleSaveDraft = async () => {
  if (!page) return;
  setSaveStatus("saving");
  try {
    // 1. Flush do debounce de conteúdo pendente
    if (contentDebounceTimer.current) {
      clearTimeout(contentDebounceTimer.current);
      contentDebounceTimer.current = null;
      const pending = pendingContentPatch.current;
      pendingContentPatch.current = null;
      if (pending) {
        await pagesService.updateSection(page.productSlug, page.id, pending.sectionId, {
          content: { ...selectedSection?.content, ...pending.patch },
        });
      }
    }
    // 2. Persistir status "draft" via rota real
    await pagesService.updatePage(page.productSlug, page.id, { status: "draft" });
    // 3. Atualizar estado local
    await refreshPage(page);
    setSaveStatus("saved");
    toast.success("Rascunho salvo");
    setTimeout(() => setSaveStatus("idle"), 3000);
  } catch (err: unknown) {
    setSaveStatus("idle");
    toast.error("Falha ao salvar", {
      description: (err as { message?: string }).message ?? "Tente novamente.",
    });
  }
};
```

```tsx
{/* PageEditor.tsx — botão usa handleSaveDraft, não triggerSave: */}
<Button onClick={handleSaveDraft} disabled={saveStatus === "saving"}>
  {saveStatus === "saving" ? <Loader2 size={14} className="animate-spin" /> : null}
  Salvar rascunho
</Button>
```

**Impacto no botão "Preview":** após o flush via `handleSaveDraft`, o preview pode ser aberto imediatamente e encontrará os dados corretos (incluindo `selectedEventIds`) persistidos no backend.

**E.7.2 — `BlockRenderer` para `event-list` é placeholder**

Mesmo que `selectedEventIds` esteja corretamente persistido no backend, o `BlockRenderer` não renderiza os eventos:

```tsx
// BlockRenderer.tsx — apenas conta, nunca renderiza dados reais:
case "event-list": {
  const selectedCount = Array.isArray(c.selectedEventIds) ? c.selectedEventIds.length : 0;
  return (
    <div className="p-6">
      <h3 className="text-xl font-semibold">{asStr(c.title, "Agenda")}</h3>
      <p className="mt-2 text-sm text-muted-foreground">
        {selectedCount > 0
          ? `${selectedCount} evento(s) selecionado(s) para este bloco.`  // não renderiza os eventos
          : "Nenhum evento selecionado ainda (ver editor)."}
      </p>
    </div>
  );
}
```

O renderer correto deveria buscar os eventos via `eventsService.listEvents(productId)`, filtrar pelos IDs em `selectedEventIds` e renderizar cards com título, data, local e tipo.

**Implementação necessária:**

**E.7.1 — Implementar `handleSaveDraft` real (ver jornada completa no diagnóstico acima).**

A implementação está detalhada no diagnóstico do E.7.1. Resumo dos pontos de mudança:
- Substituir chamada a `triggerSave()` no botão por `handleSaveDraft()` (async)
- `handleSaveDraft`: flush debounce → `pagesService.updateSection` (se pendente) → `pagesService.updatePage({ status: "draft" })` → `refreshPage` → toast real
- Manter `triggerSave()` apenas para o auto-save visual do debounce (sem alteração no comportamento do debounce)

**E.7.2 — `BlockRenderer` deve renderizar cards de evento reais:**

```tsx
// BlockRenderer.tsx — event-list deve buscar e renderizar eventos reais:
case "event-list": {
  // Componente assíncrono ou usar hook — buscar eventos filtrados por selectedEventIds
  const selectedIds: string[] = Array.isArray(c.selectedEventIds) ? c.selectedEventIds as string[] : [];
  // Renderizar: título da agenda + lista de cards com título, data, local, badge de tipo
  // Para o preview estático, mostrar os dados já presentes em c.events (desnormalizados)
  // OU transformar BlockRenderer em componente que aceita productId e faz fetch
}
```

Opção recomendada: desnormalizar os dados do evento no conteúdo do bloco no momento da seleção (guardar `{ id, title, date, location, type }` em vez de só o ID), eliminando a necessidade de fetch no renderer.

**Critério de aceite:**
- [ ] Clicar "Salvar rascunho" persiste mudanças de bloco pendentes antes de atualizar indicador visual.
- [ ] Preview de `event-list` exibe cards reais dos eventos selecionados (título, data, local, tipo).
- [ ] Navegar para preview com conteúdo não salvo: alerta ou flush automático.
- [ ] Selecionar evento no editor → abrir preview → evento visível sem etapas extras.

**Smoke test:**
```
1. Editor → bloco event-list → "Gerenciar eventos" → criar evento → fechar drawer
2. Selecionar evento na lista (checkmark)
3. Clicar "Salvar rascunho" imediatamente
4. Clicar "Preview"
5. Esperado: evento aparece no bloco de agenda com título e data
6. (Antes da fix) Esperado atual: "Nenhum evento selecionado ainda"
```

---

## E.8 — Troca de produto sem feedback visual: ausência de loading de transição

**Módulos afetados:** `frontend/src/core/auth/AuthContext.tsx`, `frontend/src/app/layouts/AppShell.tsx`, `frontend/src/core/products/` (novo componente a criar)

**Comportamento observado:** Ao trocar de produto (via `Switcher` no `AppShell`, via `ProductCard` ou via `ProductsList`), a interface atualiza instantaneamente sem nenhuma indicação visual de que o contexto mudou. Em modo API, cada componente re-fetcha dados de forma independente, resultando em um patchwork de skeletons espalhados pela tela sem unidade visual.

**Diagnóstico:**

`switchProduct` é síncrono e não possui nenhum estado de loading:

```typescript
// AuthContext.tsx — troca imediata, sem loading:
const switchProduct = useCallback((productId: string) => {
  if (!effectiveTenant) return;
  const p = (userProducts[effectiveTenant.id] || []).find((p) => p.id === productId);
  if (p) setSelectedProduct(p); // síncrono — React re-renderiza na mesma frame
}, [effectiveTenant, userProducts]);
```

Efeitos colaterais:
- Em modo mock: troca é instantânea mas sem confirmação visual de que o produto mudou.
- Em modo API: cada módulo (Páginas, Conteúdo, Forms, Assets…) busca dados do novo produto de forma independente ao re-montar, exibindo skeletons descoordenados por durações diferentes.
- Usuário não tem feedback claro de que a troca de contexto ocorreu — especialmente problemático em produtos com nomes similares.

**Implementação necessária:**

Criar um overlay de transição de produto com duração mínima de ~1s, ativado por `switchProduct`:

**1. Adicionar estado de transição ao `AuthContext`:**
```typescript
// AuthContext.tsx
const [productSwitching, setProductSwitching] = useState(false);

const switchProduct = useCallback((productId: string) => {
  if (!effectiveTenant) return;
  const p = (userProducts[effectiveTenant.id] || []).find((p) => p.id === productId);
  if (!p) return;
  setProductSwitching(true);
  setSelectedProduct(p);
  // Mínimo de 1s para garantir feedback visual mesmo em troca rápida (mock/cache)
  setTimeout(() => setProductSwitching(false), 1000);
}, [effectiveTenant, userProducts]);
```

Expor `productSwitching` via `AuthContext`.

**2. Criar `ProductSwitchingOverlay` (novo componente):**
```tsx
// frontend/src/core/products/ProductSwitchingOverlay.tsx
export function ProductSwitchingOverlay({ product }: { product: Product | null }) {
  return (
    <div className="fixed inset-0 z-50 flex flex-col items-center justify-center gap-4 bg-background">
      <img src="/aegis-logo.svg" alt="Aegis PMS" className="h-16 w-16 animate-pulse" />
      <p className="text-sm font-medium text-muted-foreground">
        Carregando {product?.name ?? "produto"}…
      </p>
      {/* Skeleton representativo do dashboard */}
      <div className="mt-4 w-full max-w-xl space-y-3 px-8">
        <SkeletonLines />
      </div>
    </div>
  );
}
```

**3. Renderizar no `AppShell`:**
```tsx
// AppShell.tsx
const { productSwitching, effectiveProduct } = useAuth();
// ...
{productSwitching && <ProductSwitchingOverlay product={effectiveProduct} />}
```

**Dependência de integração com backend:**

O overlay de 1s é suficiente para mock mode. Em modo API, o comportamento ideal é manter o overlay até que o primeiro fetch crítico do novo produto complete (ex.: `listPages` ou `listContent`). Isso requer que `switchProduct` retorne uma `Promise` que resolve quando os dados iniciais do produto estiverem prontos — mudança mais invasiva, a ser avaliada junto com a etapa de integração real (Etapa 30 — Migration SQL).

**Recomendação:** implementar o overlay com timeout fixo de 1s agora (baixo risco, alto valor de UX); refinar para await de fetch real quando a integração com backend estiver estável.

**Critério de aceite:**
- [ ] Trocar produto via Switcher do header exibe overlay com logo Aegis por ~1s.
- [ ] Trocar produto via `ProductCard` ou `ProductsList` também exibe overlay.
- [ ] Overlay some automaticamente após 1s (ou após fetch real, se implementado).
- [ ] Nome do produto sendo carregado aparece no overlay.
- [ ] Nenhum flash de dados do produto anterior durante a transição.

**Smoke test:**
```
1. Login → selecionar Maestro Beton → aguardar carregamento
2. Trocar para outro produto via Switcher no header
3. Esperado: overlay com logo Aegis + "Carregando [Produto]…" por ~1s
4. Após overlay: dashboard do novo produto carregado corretamente
5. Repetir via ProductsList → "Abrir"
```

---

## E.9 — Inserção de blocos falha silenciosamente para a maioria dos tipos (contrato vs DEFAULT_BLOCK_CONTENT + sem catch)

**Módulos afetados:** `frontend/src/domains/pages/pages/PageEditor.tsx`, `frontend/src/domains/pages/blockDefaults.ts`, `backend/.../pages/service/SectionContentValidationService.java`

**Comportamento observado:** Ao selecionar um tipo de bloco no dropdown e clicar "Adicionar bloco", a interface não responde — nenhum bloco é criado, nenhuma mensagem de erro aparece. Afeta a maioria dos tipos: `video-gallery`, `image`, `event-list`, `contact`, `form`, `download`, `audio`, `video`.

**Diagnóstico — duas causas raiz combinadas:**

**E.9.1 — `handleAddBlock` sem `catch`: erros do backend são silenciados**

```typescript
// PageEditor.tsx — sem try/catch:
const handleAddBlock = async (type: BlockType) => {
  if (!page) return;
  const label = `Novo bloco ${page.sections.length + 1}`;
  const created = await pagesService.createSection(...); // lança 400 → não tratado
  await refreshPage(page);   // nunca executado
  toast.success("Bloco adicionado", ...); // nunca executado
};
```

Qualquer erro 400 do backend é propagado como exceção não capturada. Não há `toast.error` — o usuário vê o botão sem reação.

**E.9.2 — `DEFAULT_BLOCK_CONTENT` viola restrições de validação do backend**

O backend (`SectionContentValidationService`) valida o conteúdo na criação da seção. Os defaults do frontend produzem conteúdo inválido para 7 dos 19 tipos de bloco:

| Tipo de bloco | Erro de validação backend | Causa no DEFAULT_BLOCK_CONTENT |
|---|---|---|
| `image` | `IMAGE_ALT_REQUIRED` | Backend espera `alt` no nível raiz do content, default envia `{ image: { alt: "..." } }` — estrutura aninhada |
| `event-list` | `EVENT_LIST_SOURCE_REQUIRED` | Backend exige campo `source` como objeto, default não inclui `source` |
| `contact` | `FORM_REFERENCE_REQUIRED` | Backend exige `formId` como UUID válido, default envia `formId: ""` |
| `form` | `FORM_REFERENCE_REQUIRED` | Mesma causa que `contact` |
| `download` | `DOWNLOAD_ITEM_ASSET_INVALID` | Cada item exige asset UUID válido, default envia `fileAssetId: ""` |
| `audio` | `AUDIO_ASSET_INVALID` | Source = "upload" exige asset UUID, default envia `fileAssetId: ""` |
| `video` | `VIDEO_ASSET_INVALID` | Mesma causa que `audio` |
| `video-gallery` | `VIDEO_GALLERY_ITEMS_COUNT_OUT_OF_RANGE` | Exige ≥1 item, default envia `items: []` |

Tipos que **passam** na validação atual (para referência): `hero`, `text`, `rich-text`, `two-column`, `image-text`, `feature-grid`, `card-list`, `gallery`, `timeline`, `cta-section`, `faq`, `social-links`.

**Causa raiz de design:** o backend valida estritamente na criação (`POST /sections`), mas blocos como `audio`, `video`, `download`, `contact` e `form` precisam de assets ou formulários pré-existentes — é impossível criar um default válido sem dados do produto.

**Implementação necessária:**

**E.9.1 — Adicionar try/catch em `handleAddBlock`:**
```typescript
const handleAddBlock = async (type: BlockType) => {
  if (!page) return;
  const label = `Novo bloco ${page.sections.length + 1}`;
  try {
    const created = await pagesService.createSection(page.productSlug, page.id, {
      type, label, content: DEFAULT_BLOCK_CONTENT[type],
    });
    await refreshPage(page);
    setSelectedSectionId(created.id);
    toast.success("Bloco adicionado", { description: `${label} (${type})` });
    triggerSave();
  } catch (err: unknown) {
    const msg = (err as { message?: string }).message ?? "Verifique os campos obrigatórios.";
    toast.error(`Não foi possível adicionar bloco "${type}"`, { description: msg });
  }
};
```

**E.9.2 — Corrigir defaults com falhas simples:**

```typescript
// blockDefaults.ts — fixes para tipos com default inválido corrigível:
"image": { alt: "Descrição da imagem", src: "" },  // mover alt para raiz
"video-gallery": {
  items: [{
    title: "Vídeo 1", source: "youtube",
    youtubeUrl: "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
  }]
},
"event-list": {
  title: "Agenda",
  source: { filter: null },  // satisfaz EVENT_LIST_SOURCE_REQUIRED
  selectedEventIds: [],
},
```

**E.9.3 — Blocos que dependem de recursos externos: abordagem recomendada**

Para `contact`, `form`, `download`, `audio` e `video`, não é possível ter um default válido sem dados do produto. Duas opções:

**Opção A (recomendada) — Relaxar validação do backend na criação:**
Mover a validação estrita de conteúdo para a transição de status (`review`/`published`), não para o `POST /sections`. Na criação, aceitar conteúdo parcial/vazio e salvar como rascunho.

```java
// SectionController.java — criar sem validar conteúdo:
// POST /sections → aceita qualquer content (salva como draft)
// PUT /sections/{id} → valida content
// POST /pages/{id}/transitions → valida content estritamente antes de publicar
```

**Opção B — Wizard de pré-criação no frontend:**
Antes de criar, exibir modal específico por tipo para coletar o mínimo necessário (ex.: para `audio`: picker de asset; para `contact`: picker de formulário). Mais complexo, mas oferece melhor UX.

**Critério de aceite:**
- [ ] Todos os 19 tipos de bloco podem ser adicionados sem erro.
- [ ] Falha na criação de bloco exibe toast com mensagem descritiva.
- [ ] `image` block: `alt` no nível raiz do content.
- [ ] `event-list` block: inclui campo `source` ao criar.
- [ ] `video-gallery` block: criado com 1 item placeholder válido.
- [ ] Blocos dependentes de asset (`audio`, `video`, `download`): ou aceitos sem asset na criação, ou wizard de seleção antes de criar.
- [ ] Bruno: `POST /pages/{pageId}/sections` para cada tipo → 201 Created.

**Smoke test (a ser executado pelo agente implementador):**
```
Para cada tipo em BLOCK_TYPES:
  1. Abrir editor de uma página em branco
  2. Selecionar o tipo no dropdown "Adicionar bloco"
  3. Clicar "Adicionar bloco"
  4. Esperado: bloco aparece na estrutura + toast de sucesso
  5. Registrar: passou / falhou / erro exibido

Tipos a testar: hero, text, rich-text, two-column, image, image-text,
feature-grid, card-list, gallery, timeline, event-list, cta-section,
faq, contact, form, download, audio, video, video-gallery, social-links
```

---

## E.10 — Hero block: lacunas de editor (imagem, variantes, botões extras)

**Módulos afetados:** `frontend/src/domains/pages/components/EditorPanels.tsx`, `blockDefaults.ts`, `backend/.../pages/domain/BlockDefaults.java`

**Comportamento observado:** O editor do bloco `hero` não exibe campo de imagem para o usuário adicionar/trocar a imagem do hero. Não há seletor de variante (primary vs secondary hero). Os campos de CTA aparecem como strings planas (`ctaUrl`, `ctaLabel`) sem suporte a múltiplos botões.

**E.10.1 — Campo de imagem ausente no editor quando o bloco foi criado sem `image`**

O `BlockEditorCanvas` renderiza `ImageFieldEditor` apenas quando o content tem um objeto com chave `src`. Se o bloco foi criado com o default do backend (`BlockDefaults.java` inicializa hero apenas com `{ title: "Novo título" }`), a chave `image` não existe — portanto o picker de imagem nunca aparece.

```java
// BlockDefaults.java — linha 59:
defaults.put(BlockType.HERO, Map.of(KEY_TITLE, "Novo título"));
// ← sem image, sem ctas
```

```tsx
// EditorPanels.tsx — nestedObjectFields só inclui chaves que JÁ existem no content:
const nestedObjectFields = Object.entries(fieldableContent)
  .filter(([, v]) => isPlainObject(v)) as [string, Record<string, unknown>][];
// Se content.image não existe → ImageFieldEditor nunca renderiza
```

**E.10.2 — Inconsistência de shape entre `blockDefaults.ts` e dados reais**

`blockDefaults.ts` define hero com CTAs como objetos aninhados:
```typescript
hero: {
  title: "Novo título",
  subtitle: "Novo subtítulo",
  image: { src: "", alt: "Descrição da imagem" },
  ctaPrimary: { label: "Saiba mais", href: "#" },
  ctaSecondary: { label: "", href: "" },
}
```

Mas o editor exibe campos planos `ctaUrl` e `ctaLabel` para o bloco hero existente (screenshot) — o conteúdo armazenado usa shape diferente do default. Não há validação de shape no backend para CTAs do hero. O `BlockRenderer` usa `c.ctaPrimary?.label`, que é `undefined` quando o dado real tem `ctaUrl`.

**E.10.3 — Campo `variant` existe no backend mas nunca é exposto no editor**

```java
// PageSection.java linha 37: private String variant;
// CreateSectionRequest.java: String variant
// UpdateSectionRequest.java: String variant
```

O frontend NUNCA envia ou exibe `variant`. Um "secondary hero" (compacto, sem imagem de fundo, layout menor) seria implementado via `variant: "secondary"`, mas não há UI para selecioná-lo.

**Implementação necessária:**

**E.10.1 — Garantir image picker sempre visível para hero:**
```tsx
// EditorPanels.tsx — injetar image key quando ausente para tipo hero:
const fieldableContent = (() => {
  const base = Object.fromEntries(Object.entries(content).filter(([k]) => !excludedKeys.has(k)));
  if (section.type === "hero" && !base.image) {
    return { ...base, image: { src: "", alt: "" } };
  }
  return base;
})();
```

Também alinhar `BlockDefaults.java` para incluir estrutura completa:
```java
defaults.put(BlockType.HERO, Map.of(
    KEY_TITLE, "Novo título",
    "subtitle", "",
    "image", Map.of("src", "", "alt", ""),
    "ctas", List.of(Map.of("label", "Saiba mais", "href", "#"))
));
```

**E.10.2 — Canonicalizar shape de CTAs para array:**
```typescript
// Novo shape canônico (substitui ctaPrimary/ctaSecondary/ctaUrl/ctaLabel):
hero: {
  title: "Novo título",
  subtitle: "Novo subtítulo",
  image: { src: "", alt: "" },
  ctas: [
    { label: "Saiba mais", href: "#" },
  ],
  theme: "dark",  // "dark" | "light" | "transparent"
}
```

`BlockRenderer` atualizado para usar `asArray(c.ctas).map(...)` em vez de `c.ctaPrimary`/`c.ctaSecondary`.

**E.10.3 — Adicionar seletor de variante no editor:**
```tsx
// EditorPanels.tsx — expor variant acima dos campos de content:
{(section.type === "hero") && (
  <SelectLike
    label="Variante"
    value={section.variant ?? "primary"}
    options={["primary", "secondary"]}
    onChange={(v) => onChangeVariant(v)}  // novo prop para atualizar seção via UpdateSectionRequest
  />
)}
```

`secondary hero`: sem campo `image`, layout compacto, `theme` padrão `"light"`.

**Critério de aceite:**
- [ ] Hero block sempre exibe o picker de imagem no editor (campo `image` injetado quando ausente).
- [ ] CTAs do hero usam shape `ctas: Array<{ label, href }>` — suporta 1 a 4 botões.
- [ ] `BlockRenderer` renderiza CTAs a partir de `ctas[]`, não de `ctaPrimary`/`ctaSecondary`.
- [ ] Variante "secondary" exibe layout compacto sem campo de imagem.
- [ ] `variant` é persistido via `UpdateSectionRequest` quando alterado.

---

## E.11 — Imagens nunca renderizadas no preview (todos os blocos)

**Módulos afetados:** `frontend/src/shared/components/MediaField.tsx`, `frontend/src/domains/pages/components/BlockRenderer.tsx`

**Comportamento observado:** Imagens selecionadas via picker de assets não aparecem no preview da página. O `BlockRenderer` exibe o texto alternativo entre colchetes (`[imagem: alt text]`) para todos os blocos com imagem: `hero`, `image`, `image-text`, `gallery`. A seleção de um asset aparece nos Assets da plataforma, mas não no preview do PageEditor.

**Causa raiz — 2 problemas independentes encadeados:**

**E.11.1 — `MediaField.onChange` retorna `asset.name` (filename), não `asset.id` (UUID)**

```typescript
// MediaField.tsx linha 50:
onChange(asset.name);  // ← retorna "hero-maestro-beton.jpg", não o UUID
```

O content do bloco armazena `image.src = "hero-maestro-beton.jpg"` — uma string de nome de arquivo que o browser não sabe resolver para uma URL real. Sem o UUID, é impossível chamar o endpoint de download do asset.

**E.11.2 — `BlockRenderer` usa placeholders de texto para todas as imagens**

```tsx
// BlockRenderer.tsx — bloco "image":
case "image":
  return <div>[imagem: {asStr(c.alt, "sem descrição")}]</div>;

// bloco "hero":
{hasImage && <div>[imagem: {asStr(image.alt)}]</div>}  // placeholder, nunca <img>

// bloco "gallery":
{items.map((item) => <div>{asStr(item.alt, "imagem")}</div>)}  // placeholder
```

O `BlockRenderer` foi escrito como preview estrutural, nunca para renderizar imagens reais. A infra para resolver UUIDs em URLs existe (`useAssetObjectUrl(assetId)` em `AssetDetail.tsx`, padrão `resolveBaseUrl() + /assets/{id}/download` em `FeedbackDetailDrawer.tsx`), mas não é usada no `BlockRenderer`.

**Implementação necessária:**

**E.11.1 — `MediaField` deve retornar UUID:**
```typescript
// MediaField.tsx — mudar assinatura e callback:
export function MediaField({
  label, value, onChange, typeFilter, onSelectAsset
}: {
  onChange: (assetId: string) => void;  // agora retorna UUID, não name
  // ...
}) {
  // ...
  onSelect={(asset) => {
    onChange(asset.id);           // ← UUID
    onSelectAsset?.(asset);
  }}
}
```

O `value` prop também muda semântica: de `asset.name` para `asset.id`. Atualizar `ImageFieldEditor` e todos os chamadores.

**E.11.2 — `BlockRenderer` deve renderizar `<img>` real:**
```tsx
// Novo hook utilitário:
function useAssetUrl(assetId: string | undefined): string | null {
  if (!assetId) return null;
  if (IS_API_MODE) return `${resolveBaseUrl()}/api/v1/assets/${assetId}/download`;
  // mock mode: buscar no mock store pelo id e retornar uma URL de placeholder
  const asset = assetsStore.find((a) => a.id === assetId);
  return asset ? `https://placehold.co/800x400?text=${encodeURIComponent(asset.name)}` : null;
}

// BlockRenderer — bloco "image":
case "image": {
  const url = useAssetUrl(typeof c.imageAssetId === "string" ? c.imageAssetId : undefined);
  return url
    ? <img src={url} alt={asStr(c.alt)} className="w-full rounded-lg object-cover" />
    : <div className="rounded-lg bg-muted p-6 text-center text-xs text-muted-foreground">[imagem: {asStr(c.alt, "sem descrição")}]</div>;
}
```

> **Nota:** `useAssetUrl` não pode ser um hook React dentro de um `switch-case` — extrair para uma função utilitária pura que não use `useState`/`useEffect`, ou criar um componente `<AssetImage assetId={...} alt={...} />` reutilizável.

**Migração de dados:** conteúdo existente que usa `image.src = "filename.jpg"` (string de nome) precisa de migração para `imageAssetId = <uuid>`. Adicionar script de migração ou aceitar que blocos antigos mostrem placeholder até reedição.

**Critério de aceite:**
- [ ] Selecionar imagem via picker → `image.imageAssetId` armazena UUID do asset.
- [ ] `BlockRenderer` renderiza `<img>` real para blocos `image`, `image-text`, `hero` (quando `imageAssetId` presente).
- [ ] Em mock mode: imagem exibe placeholder visual com nome do arquivo.
- [ ] Em API mode: imagem exibe a imagem real via `GET /api/v1/assets/{id}/download`.
- [ ] `gallery` itens: cada item com `imageAssetId` → `<img>` real no preview.
- [ ] Blocos sem `imageAssetId` → placeholder texto (retrocompatibilidade).

**Smoke test:**
```
1. Abrir editor de página → bloco "image"
2. Selecionar asset de imagem via MediaField
3. Verificar: BlockRenderer exibe <img> real (não texto entre colchetes)
4. Salvar rascunho → reabrir página → imagem ainda visível
5. Em mock mode: imagem exibe placehold.co com nome do arquivo
```

---

## Seção Z — Critérios de aceite globais da sprint

### Z.1 — Gates obrigatórios

```bash
# Frontend mock mode (backend parado):
IS_API_MODE=false npm run dev
# → Login funciona, navegação funciona, dados mock visíveis, zero erros de console

# Geração de tipos:
npm run generate:types && npm run typecheck
# → 0 erros

# Backend standalone:
cd backend && mvn verify -q
# → BUILD SUCCESS + jacoco-check PASSED

# Bruno contrato:
cd bruno && bru run --env local
# → 0 FAILED (inclui assertions de shape)

# E2E integrado (backend + frontend UP):
npm run e2e
# → todos os testes passam
```

### Z.2 — Regras de ouro desta sprint

1. **Todo dado de negócio no JSX é proibido** — deve vir de prop/hook/service.
2. **Todo service deve ter guarda IS_API_MODE** — sem exceção.
3. **Todo endpoint novo = Bruno assertion de shape** — não só status.
4. **Todo fluxo crítico = teste E2E** — convite, login, assets, conteúdo.
5. **Tipos do frontend = gerados do OpenAPI** — nunca escritos manualmente para DTOs existentes.
