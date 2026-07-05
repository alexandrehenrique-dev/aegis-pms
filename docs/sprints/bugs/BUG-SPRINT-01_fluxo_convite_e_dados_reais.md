# Bug Sprint 01 — Fluxo de convite, dados reais e cobertura de testes

> **Contexto:** Esta sprint documenta todos os bugs identificados durante testes manuais do fluxo de convite de usuários (produto e tenant), exibição de dados reais no frontend, e gaps de cobertura que quebram o gate JaCoCo 100%.
>
> **Pré-requisito antes de qualquer tarefa:** reiniciar o backend Spring Boot para aplicar a migration **V19** (`ALTER TABLE auth_action_tokens ADD COLUMN product_slug VARCHAR(120)`). Sem isso, qualquer endpoint que leia/escreva `auth_action_tokens` falhará com erro de schema.
>
> **Branch sugerida:** `bugfix/sprint-01-convite-dados-reais`

---

## Diagnóstico de ambiente

Antes de trabalhar nas tarefas, o agente deve validar o ambiente com os comandos abaixo. Se qualquer check falhar, não adianta testar os fluxos.

```bash
# 1. Backend UP e migration aplicada
curl -s http://localhost:8080/actuator/health | jq '.status'
# Esperado: "UP"

curl -s http://localhost:8080/actuator/flyway | jq '.contexts[].flywayBeans.flyway.migrations[] | select(.script | contains("V19")) | .state'
# Esperado: "SUCCESS"

# 2. TypeScript sem erros
cd frontend && npm run typecheck
# Esperado: saída vazia (0 erros)

# 3. Maven testes + JaCoCo
cd backend && mvn verify -q
# Esperado: BUILD SUCCESS + jacoco-check PASSED (100% line e branch)

# 4. Bruno smoke tests
cd bruno && bru run --env local --reporter-json results.json
# Esperado: todos os testes PASSED, 0 FAILED
```

---

## Seção A — Bugs de dados reais no frontend

### A.1 — `productCount` sempre mostra 0 no card do tenant

**Sintoma:** Tela "Gestão de Tenants" (rota `/`) mostra "0 produtos" no card mesmo quando o tenant tem produtos cadastrados.

**Causa raiz:** `tenantMapper.ts` hardcoda `productCount: 0` porque o backend não retorna esse campo no DTO de `GET /tenants`. Os produtos existem, mas o count nunca é atualizado após o carregamento.

**Fix já aplicado em código:**
- `frontend/src/core/auth/AuthContext.tsx` — após montar `productsByTenant`, re-emite `setUserTenants` com `productCount` derivado da contagem real de produtos carregados:
  ```ts
  setUserTenants(tenants.map(t => ({
    ...t,
    productCount: (productsByTenant[t.id] ?? []).length
  })));
  ```
  Aplicado tanto no `initSession` quanto no `useEffect` de restauração (F5).

**Critério de aceite:**
- [ ] Logar como Super Admin → tela "Gestão de Tenants" → card "CLIENTES BETA" exibe "1 produto" (ou o número real).
- [ ] Fazer F5 → contagem persiste corretamente.
- [ ] Criar um novo produto → productCount incrementa sem re-login.

**Validação de jornada — smoke test manual:**
```
1. Login: super-admin / senha
2. Navegar para /  (Gestão de Tenants)
3. Observar o card: deve mostrar "N produtos" onde N > 0
4. Se ainda mostrar 0: verificar Network → GET /products → confirmar que
   o campo tenantId do produto bate com o id do tenant no card
```

**Se ainda mostrar 0 após o fix frontend:** o problema pode ser no mapeamento de IDs. Verificar:
```bash
# Comparar tenantId dos produtos com id dos tenants
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/tenants | jq '.[].id'
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/products | jq '.[].tenantId'
# Os valores devem coincidir
```

---

### A.2 — Lista de usuários vazia após convite via produto

**Sintoma:** Após clicar em "Convidar" no `InviteUserDrawer` (rota `/users/invite`), o toast "Convite enviado!" aparece, mas ao voltar para `/users` a lista continua vazia.

**Causa raiz A — frontend:** `InviteUserDrawer` chamava `usersService.invite({ name, email, role, allowedProducts })` sem o segundo argumento `tenantId`. Com `tenantId` undefined, a condição `IS_API_MODE && tenantId` falhava e o convite ia para o mock em memória — invisível na chamada real de `GET /tenants/{id}/users`.

**Fix aplicado:**
```ts
// frontend/src/domains/users/pages/InviteUserDrawer.tsx
const { tenantProducts, effectiveTenant } = useAuth();
// ...
await usersService.invite({ name, email, role, allowedProducts }, effectiveTenant?.id);
```

**Causa raiz B — backend (Fix B da sessão anterior):** `ProductAssignmentService.inviteUser()` criava `ProductAssignment` mas nunca criava `TenantMembership`. O endpoint `GET /tenants/{id}/users` consulta memberships → usuário convidado via produto era invisível.

**Fix aplicado (backend):**
- `ProductAssignmentService.inviteUser()` injeta `TenantUserAccessService` e chama `tenantUserAccessService.invite(...)` se não houver membership.

**Critério de aceite:**
- [ ] Convidar usuário via `/users/invite` com nome/email/role/produto.
- [ ] Após navegar para `/users`, o usuário aparece na lista com status "convidado".
- [ ] O backend retorna HTTP 200 em `POST /tenants/{id}/users/invite`.
- [ ] `GET /tenants/{id}/users` retorna o usuário com `status: "convidado"`.

**Validação de jornada:**
```
1. Login como Tenant Admin ou Super Admin no tenant "CLIENTES BETA"
2. Navegar para /users → clicar "Convidar"
3. Preencher: Nome=Teste, Email=teste@byop.dev, Role=Editor, Produto=Maestro Beton
4. Clicar "Enviar convite"
5. Verificar: navega automaticamente para /users
6. Verificar: "Teste" aparece na tabela com badge "convidado"
7. Verificar no MailHog: email de convite chegou em teste@byop.dev
```

---

### A.3 — `ProductDashboard` exibia KPIs hardcoded

**Sintoma:** Dashboard do produto mostrava "42 conteúdos", "7 em revisão", "89 formulários", "5.2% conversão", "18 assets", "Saudável" — todos valores fixos no código, não dados reais.

**Fix aplicado:**
- `frontend/src/domains/products/pages/ProductDashboard.tsx`: substituídos por "—" para KPIs sem endpoint real. `status` e `modules` usam `effectiveProduct?.status` e `effectiveProduct?.modules` (dados reais do contexto).

**Critério de aceite:**
- [ ] Dashboard do produto exibe o nome, tipo e status real do produto selecionado.
- [ ] KPIs de conteúdo/formulários/assets mostram "—" (não valores inventados).

---

## Seção B — Bugs de UX/navegação

### B.1 — `InviteUserDrawer`: sem navegação pós-envio, Cancelar sem ação

**Sintoma:** Após enviar convite, a tela ficava na mesma página com o botão desabilitado. Botão "Cancelar" não fazia nada.

**Fix aplicado:**
```ts
// Após usersService.invite() bem-sucedido:
toast.success("Convite enviado!", { ... });
navigate("/users");  // ← navegação imediata

// Botão Cancelar:
<Button onClick={() => navigate("/users")}>Cancelar</Button>
```

**Critério de aceite:**
- [ ] Após envio bem-sucedido: navega para `/users` automaticamente.
- [ ] Botão "Cancelar": navega para `/users`.

---

### B.2 — `ModuleCatalog`: botão "Abrir" mostrava toast em vez de navegar

**Sintoma:** Ao clicar "Abrir" em um módulo habilitado (Conteúdo, Assets, Forms etc.), aparecia um toast "Módulo já habilitado" em vez de abrir a tela do módulo.

**Fix aplicado:**
```ts
// frontend/src/domains/products/components/ModuleCatalog.tsx
const MODULE_ROUTES: Record<string, string> = {
  "Conteúdo":        "/content",
  "Assets":          "/assets",
  "Forms":           "/forms",
  "Analytics":       "/analytics",
  "SEO":             "/settings/product",
  "Knowledge Graph": "/knowledge",
  "Pages":           "/pages",
};

// handleAction quando state === "habilitado":
const route = MODULE_ROUTES[m.name];
if (route) navigate(route);
```

**Critério de aceite:**
- [ ] Clicar "Abrir" em Conteúdo → navega para `/content`.
- [ ] Clicar "Abrir" em Assets → navega para `/assets`.
- [ ] Clicar "Abrir" em Forms → navega para `/forms`.
- [ ] Módulo sem rota mapeada: mantém toast informativo (não erro).

---

### B.3 — Tela branca após login (tenants carregam só no F5)

**Sintoma:** Após fazer login, a tela ficava em branco. Os tenants só apareciam depois de F5.

**Causa raiz:** `initSession()` chamava `setUserTenants(tenants)` apenas no final do chain, depois de `await listProducts()`. Se `listProducts()` lançasse erro, `setUserTenants` nunca era chamado — usuário ficava autenticado mas sem tenants.

**Fix aplicado:**
```ts
// AuthContext.tsx — initSession e useEffect de restauração
const tenants = await tenantsService.listTenants();
setUserTenants(tenants);  // ← setado ANTES de aguardar listProducts

const products = await productsService.listProducts()
  .catch(() => [] as ...);  // ← erro absorvido graciosamente
```

**Critério de aceite:**
- [ ] Login → sem tela branca → tenants aparecem imediatamente.
- [ ] F5 → sessão restaurada → tenants e produtos carregam.
- [ ] Se `GET /products` retornar 500: login ainda funciona, mostra tenants sem produtos.

---

## Seção C — Bugs de TypeScript (compilação)

### C.1 — `AuditEventDetail.tsx`: funções/variáveis fora de escopo

**Sintoma:** `tsc --noEmit` reportava 8 erros em `AuditEventDetail.tsx`:
- `Cannot find name 'effectiveTenant'` e `effectiveProduct'` em função definida fora do componente.
- `Cannot find name 'payload'` em JSX onde a variável era local de outra função.

**Fix:** Movida `exportEventJson` para dentro do componente; `payload` definido no escopo do componente (não dentro da função de exportação).

### C.2 — `ProductSettings.tsx`: `product.key` inexistente em `ProductOption`

**Sintoma:** `Property 'key' does not exist on type 'ProductOption'`.

**Fix:**
- Adicionado `key?: string` ao tipo `ProductOption` em `shared/types/auth.ts`.
- `AuthContext.tsx` propaga `key: product.key` nos dois locais onde `ProductOption` é montado.

**Critério de aceite:** `npm run typecheck` → saída vazia (zero erros).

---

## Seção D — Cobertura JaCoCo (100% line + branch obrigatório)

### D.1 — `ProductAssignmentActivationListener` sem teste

**Arquivo:** `backend/src/main/java/br/com/byop/aegis/product/service/ProductAssignmentActivationListener.java`

Classe nova criada na sessão de bugfix. Sem nenhum teste → JaCoCo reprovar no `mvn verify`.

**Teste criado:** `ProductAssignmentActivationListenerTest.java` (3 casos):
1. `shouldDoNothingWhenNoInvitedAssignmentsExist` — lista vazia, `save()` nunca chamado.
2. `shouldTransitionSingleInvitedAssignmentToAssigned` — 1 assignment vai de INVITED para ASSIGNED.
3. `shouldTransitionAllInvitedAssignmentsWhenUserHasMultiple` — 2 assignments, ambos transitados.

**Critério de aceite:**
```bash
cd backend && mvn test -pl . -Dtest=ProductAssignmentActivationListenerTest -q
# BUILD SUCCESS
```

### D.2 — `TenantUserAccessService.onUserInviteActivated()` sem cobertura

**Arquivo:** `backend/src/main/java/br/com/byop/aegis/tenant/api/TenantUserAccessService.java`

Método `@EventListener` adicionado na sessão de bugfix, sem casos de teste correspondentes.

**Testes adicionados em `TenantUserAccessServiceTest.java`:**
1. `shouldDoNothingOnActivationEventWhenNoInvitedMembershipsExist`
2. `shouldActivateInvitedMembershipsOnUserInviteActivatedEvent`

**Critério de aceite:**
```bash
cd backend && mvn test -pl . -Dtest=TenantUserAccessServiceTest -q
# BUILD SUCCESS
```

### D.3 — Gate completo JaCoCo

```bash
cd backend && mvn verify -q
# Esperado: BUILD SUCCESS
# jacoco-check: LINE 100%, BRANCH 100%
```

Se algum teste falhar por assinatura divergente, verificar:
- `AuthActivationService.activate()` — agora publica `IdentityUserInviteActivatedEvent` além do audit event. O teste `shouldActivateInvite()` usa `atLeastOnce()` e `any(IdentityUserInviteActivatedEvent.class)`.
- `IdentityActionInviteCommand` — agora tem 9 campos (inclui `productSlug` na posição 7). Todos os testes que instanciam esse record devem passar `null` como 7º argumento quando não há produto.
- `addInviteContext()` — agora aceita 6 parâmetros (inclui `productSlug` na posição 4). Todos os callers de teste devem passar `null` quando não houver slug.

---

## Seção E — Fluxo de ativação de convite (Fix A/B/C backend)

### E.1 — `activate()` não transitava `ProductAssignment` para ASSIGNED

**Causa:** `AuthActivationService.activate()` só resetava senha e habilitava usuário no Keycloak, mas nunca notificava o módulo `product` para transicionar o status.

**Fix:** Publica `IdentityUserInviteActivatedEvent` via `ApplicationEventPublisher` após `setUserEnabled()`. Dois listeners consomem:
- `ProductAssignmentActivationListener` → assignment INVITED → ASSIGNED
- `TenantUserAccessService.onUserInviteActivated()` → membership INVITED → ACTIVE

### E.2 — `inviteUser()` não criava `TenantMembership`

**Causa:** `ProductAssignmentService.inviteUser()` criava `ProductAssignment` com status INVITED mas nunca criava `TenantMembership`. `GET /tenants/{id}/users` consulta memberships → usuário invisível.

**Fix:** `inviteUser()` agora chama `tenantUserAccessService.invite(...)` condicionalmente (guard `hasAnyMembership` evita duplicata).

### E.3 — `validateInvite()` não retornava `requiresPasswordSetup` nem `productSlug`

**Causa:** `AuthInviteValidationResponse` não tinha os campos que o frontend `InviteTokenData` espera.

**Fix:** Campo `productSlug` propagado pelo chain completo:
```
ProductAssignmentService.inviteUserThroughPort()
  → ProductAssignmentInvitePort.invite() (7 args)
  → KeycloakProductAssignmentInvitePort
  → IdentityActionInviteCommand (9 campos)
  → AuthActionToken.addInviteContext() (6 params)
  → AuthActionToken.getProductSlug()
  → AuthActivationService.validateInvite()
  → AuthInviteValidationResponse (novo: productSlug + requiresPasswordSetup)
```

**Migration necessária:** `V19__auth_action_token_product_slug.sql`
```sql
ALTER TABLE auth_action_tokens ADD COLUMN product_slug VARCHAR(120);
```

### Jornada de validação completa do fluxo de convite:

```
1. LOGIN: super-admin / senha
2. Navegar para /users (tenant CLIENTES BETA)
3. Clicar "Convidar" → preencher email=novo@email.com, produto=Maestro Beton
4. Clicar "Enviar convite"
   → ESPERADO: navega para /users
   → ESPERADO: "novo@email.com" aparece na lista com status "convidado"

5. Abrir MailHog (http://localhost:8025)
   → ESPERADO: email de convite para novo@email.com
   → ESPERADO: link contém /invite?token=<uuid>

6. Clicar no link do email (abre em nova aba)
   → ESPERADO: tela de ativação com nome/email/produto pré-preenchidos
   → ESPERADO: campo de senha visível (requiresPasswordSetup: true)

7. Digitar senha forte e clicar "Ativar conta"
   → ESPERADO: redireciona para /login com mensagem de sucesso

8. Login com novo@email.com e a senha definida
   → ESPERADO: entra no sistema com acesso ao produto Maestro Beton

9. Voltar como super-admin → /users
   → ESPERADO: novo@email.com aparece com status "ativo"
   → ESPERADO: produto "Maestro Beton" listado nas colunas do usuário
```

---

## Seção F — Bruno: smoke tests que devem ser adicionados

Os testes Bruno existentes cobrem rotas pré-invite. Esta sprint adiciona 4 novos `.bru` em `bruno/invite-flow/`:

### F.1 — `convidar-usuario-via-produto.bru`
```
POST /api/v1/tenants/{{tenantId}}/users/invite
Body: { name, email, role, allowedProducts }
Assertions:
  - status 200
  - body.status == "convidado"
  - body.userId não está vazio
```

### F.2 — `listar-usuarios-tenant-com-convidado.bru`
```
GET /api/v1/tenants/{{tenantId}}/users
Assertions:
  - status 200
  - array contém entry com email == "{{invitedEmail}}" e status == "convidado"
```

### F.3 — `validar-token-convite.bru`
```
GET /api/v1/auth/invite/{{tokenId}}/validate
Assertions:
  - status 200
  - body.requiresPasswordSetup == true
  - body.productSlug não está vazio (quando convite veio de produto)
  - body.userEmail == "{{invitedEmail}}"
```

### F.4 — `ativar-conta-via-convite.bru`
```
POST /api/v1/auth/invite/{{tokenId}}/activate
Body: { password: "SenhaForte123!" }
Assertions:
  - status 200
  - body.message contém "Conta ativada"

Seguido de:
GET /api/v1/tenants/{{tenantId}}/users
Assertions:
  - entry com email == "{{invitedEmail}}" agora tem status == "ativo"
```

---

## Seção G — Bugs adicionais identificados na sessão de testes (pós-primeira versão do documento)

### G.1 — `IdentityActionTokenService.sendInviteActivation()` rola back a transação quando email falha

**Sintoma:** Após enviar convite, o usuário é criado no Keycloak mas **não aparece na lista de usuários** do tenant. O toast "Convite enviado!" aparece (201 retornado), mas `GET /tenants/{id}/users` retorna lista vazia. O email também não chega ao MailHog nos casos em que o usuário já tem uma conta.

**Causa raiz:** `TenantUserService.inviteUser()` é `@Transactional`. Dentro dele, `sendInviteActivation()` cria o `AuthActionToken` (na mesma transação) e depois chama `emailService.sendInviteActivation(token)`. Se o envio do email lançar qualquer exception (SMTP, template Freemarker, MailHog indisponível), a exception propaga para o método `@Transactional` → Spring marca a transação para rollback → **TenantMembership E AuthActionToken são desfeitos** → banco fica sem registro do usuário convidado.

Paradoxo aparente: o frontend mostra 201 porque o Keycloak não participa da transação JDBC — o usuário é criado no Keycloak (operação HTTP REST), mas os dados relacionais são desfeitos.

**Fix aplicado:**
- `IdentityActionTokenService.sendInviteActivation()` agora envolve `emailService.sendInviteActivation(token)` em `try-catch(Exception)`:
  ```java
  AuthActionToken token = tokenService.createInvite(command);
  try {
      emailService.sendInviteActivation(token);
      log.info("sendInviteActivation: convite enviado para keycloakId='{}'", ...);
  } catch (Exception e) {
      log.error("sendInviteActivation: falha ao enviar e-mail — token salvo, use 'Reenviar convite'", e);
  }
  return token;
  ```

**Consequência do fix:** se o email falhar, o token e a membership são salvos. O admin verá o usuário na lista com status "convidado" e pode usar "Reenviar convite" para tentar o email novamente.

**Teste adicionado em `IdentityActionTokenServiceTest.java`:**
- `shouldReturnTokenEvenWhenEmailFails` — verifica que exception do emailService não propaga.

**Critério de aceite:**
- [ ] Convidar usuário → usuário aparece em `GET /tenants/{id}/users` com status "convidado" independente de SMTP UP/DOWN.
- [ ] Se MailHog OFF: backend retorna 201, membership salva, log mostra o erro de e-mail.
- [ ] Se MailHog ON: backend retorna 201, membership salva, e-mail chega ao MailHog.
- [ ] `IdentityActionTokenServiceTest` → `BUILD SUCCESS`.

---

### G.2 — Botão "Reenviar convite" sem debounce (não identificado ainda, prevenir com teste)

O fix G.1 torna o resend ainda mais importante. O endpoint `POST /tenants/{id}/users/{userId}/resend-invite` já existe (`TenantUserController`). Verificar que funciona corretamente para usuários com status "convidado".

**Critério de aceite:**
- [ ] Clicar "Reenviar convite" na lista de usuários → HTTP 200 → novo e-mail no MailHog.

---

### G.3 — `UserTable`: lista não atualizava sem F5

**Sintoma:** Após navegar de `/users/invite` de volta para `/users`, a lista aparecia vazia (problema relacionado com G.1 — sem o fix, o usuário nunca era salvo).

**Fix adicional:** `UserTable` agora tem botão `<RefreshCw>` que força re-fetch incrementando `refreshKey`:
```tsx
const [refreshKey, setRefreshKey] = useState(0);
// deps incluem refreshKey:
useAsyncData(() => usersService.listUsers(effectiveTenant?.id), [isTenantWideView, effectiveTenant?.id, refreshKey]);
// Botão no header:
<Button onClick={() => setRefreshKey(k => k + 1)} title="Atualizar lista"><RefreshCw size={15} /></Button>
```

**Critério de aceite:**
- [ ] Depois de convidar usuário → clicar no botão ↻ → usuário aparece sem necessidade de F5.

---

### G.4 — `AuditTimeline`: filtros laterais eram decorativos (sem ação)

**Sintoma:** O painel "Filtros" no Audit Timeline exibia 6 labels estáticos ("usuário", "produto", "módulo", "evento", "período", "severidade") sem nenhuma ação — clicking não fazia nada.

**Fix aplicado:** `AuditTimeline.tsx` refatorado:
- Painel lateral com filtros funcionais por: Usuário (`actor`), Produto (`tenant`), Módulo (`module`), Evento (`action`), Severidade (`risk`)
- Chips clicáveis com ícones Lucide correspondentes
- Filtro "Período" visível mas desabilitado com tooltip (aguarda timestamps reais do backend)
- Badge no header mostra quantos filtros estão ativos
- Botão "Limpar" reseta todos os filtros
- Popover redundante do header removido (filtros agora são todos no painel lateral)
- Botão "Exportar" atualizado com ícone `<Download>`

**Arquivos modificados:** `frontend/src/domains/audit/pages/AuditTimeline.tsx`

**Critério de aceite:**
- [ ] Clicar em chip "Usuário" → filtra timeline por ator.
- [ ] Clicar em chip "Severidade: alta" → mostra só eventos de risco alto.
- [ ] "Limpar" → todos os eventos reaparecem.
- [ ] Badge "N filtros ativos" aparece quando há filtros selecionados.
- [ ] `npm run typecheck` → 0 erros.

---

## Ordem de execução sugerida para o agente

```
1. Verificar ambiente (Seção de Diagnóstico) — parar se falhar
2. Rodar: cd backend && mvn verify -q
   → Se falhar em testes de assinatura: corrigir chamadas de construtores (Seção D.3)
   → Se falhar em jacoco-check: os testes das Seções D.1/D.2 já foram adicionados
3. Rodar: cd frontend && npm run typecheck
   → Deve retornar 0 erros (Seção C já corrigida)
4. Testar jornada manual da Seção E completa
5. Criar os Bruno tests da Seção F e rodar: bru run bruno/invite-flow/ --env local
6. Verificar criterios de aceite de cada seção (checkboxes)
```

---

## Arquivos modificados nesta bug sprint

### Backend — novos arquivos
| Arquivo | Motivo |
|---------|--------|
| `identity/api/IdentityUserInviteActivatedEvent.java` | Spring event para cruzar fronteira identity→product/tenant |
| `product/service/ProductAssignmentActivationListener.java` | Listener que transiciona INVITED→ASSIGNED |
| `resources/db/migration/V19__auth_action_token_product_slug.sql` | Nova coluna product_slug |
| `product/service/ProductAssignmentActivationListenerTest.java` | Testes JaCoCo |

### Backend — arquivos modificados
| Arquivo | Mudança |
|---------|---------|
| `AuthActivationService.java` | Publica event + campos novos em validateInvite() |
| `AuthInviteValidationResponse.java` | +requiresPasswordSetup, +productSlug |
| `AuthActionToken.java` | +productSlug column, addInviteContext() 6 params |
| `AuthActionTokenService.java` | Passa productSlug no createInvite() |
| `IdentityActionInviteCommand.java` | +productSlug (9 campos) |
| `ProductAssignmentService.java` | Cria TenantMembership + passa productKey |
| `ProductAssignmentInvitePort.java` | Interface com 7 args |
| `KeycloakProductAssignmentInvitePort.java` | Override com 7 args |
| `TenantUserService.java` | Passa null para productSlug nos convites tenant-level |
| `TenantUserAccessService.java` | +onUserInviteActivated() @EventListener |
| `TenantUserAccessServiceTest.java` | +2 casos para onUserInviteActivated |
| `AuthActivationServiceTest.java` | atLeastOnce(), event assert, novos campos |
| `AuthActionTokenTest.java` | addInviteContext 6 args |
| `AuthActionEmailServiceTest.java` | addInviteContext 6 args |
| `AuthActionTokenServiceTest.java` | IdentityActionInviteCommand 9 args |
| `IdentityActionTokenServiceTest.java` | IdentityActionInviteCommand 9 args |
| `KeycloakProductAssignmentInvitePortTest.java` | invite() 7 args |

### Frontend — arquivos modificados
| Arquivo | Mudança |
|---------|---------|
| `shared/types/auth.ts` | +key?: string em ProductOption |
| `core/auth/AuthContext.tsx` | productCount real, setUserTenants antecipado, key propagado |
| `domains/users/pages/InviteUserDrawer.tsx` | +effectiveTenant.id no invite(), navigate pós-envio, Cancelar funcional |
| `domains/products/components/ModuleCatalog.tsx` | "Abrir" navega em vez de toast |
| `domains/products/pages/ProductDashboard.tsx` | KPIs hardcoded → "—" / dados reais |
| `domains/audit/pages/AuditEventDetail.tsx` | payload e exportEventJson dentro do componente |
| `domains/settings/pages/ProductSettings.tsx` | product.key acessível via ProductOption.key |
| `core/auth/pages/InviteScreen.tsx` | requiresPasswordSetup branch |
| `core/auth/pages/LoginScreen.tsx` | postLoginNext ref para ?next= redirect |
| `core/auth/services/authActivationService.ts` | InviteTokenData +requiresPasswordSetup, +productSlug |
| `domains/products/pages/CreateProductForm.tsx` | addProduct() após criação |
| `core/tenants/services/tenantsService.ts` | incrementProductCount() |
