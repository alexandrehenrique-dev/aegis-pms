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
