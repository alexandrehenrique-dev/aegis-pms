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

<!-- PRÓXIMO BUG: inserir abaixo desta linha -->

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
