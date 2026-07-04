# Sprint de Integração 02 — Contratos cross-cutting: slug→UUID, placeholders literais, multipart

> **Pré-requisito:** Sprint 01 concluída (autenticação real funcionando).
>
> **Problema:** existem três classes de bugs que afetam TODOS os domínios simultaneamente e precisam ser resolvidos antes de qualquer sprint por domínio:
> 1. Frontend usa `key`/slug nos paths de URL; backend espera `UUID id` — toda chamada `/products/{slug}/...` retorna 400/404.
> 2. Placeholders literais `{productId}`, `{contentId}`, `{formId}` nunca substituídos em vários services — chamadas garantidamente quebradas.
> 3. `apiClient.ts` fixa `Content-Type: application/json` em todos os requests — upload multipart de assets é impossível.
>
> **Branch:** `integration/02-contratos-cross-cutting`

---

## A. Slug vs UUID — decisão e implementação

### A.1 — Decisão arquitetural

O backend usa `UUID id` como identificador primário em todos os paths. O frontend usa `key` (slug, ex: `"maestro-beton"`) porque é mais legível nas URLs de produto. A solução que não quebra nenhum dos dois lados:

**Regra:** todos os services frontend que constroem paths de API devem usar `product.id` (UUID), não `product.key`. A UI continua exibindo `key` para o usuário, mas nunca o coloca em chamadas de API.

### A.2 — `AuthContext` / `effectiveProduct`

Verificar que `effectiveProduct` em `AuthContext` (e `useAuth()`) expõe **tanto** `id` (UUID) **quanto** `key` (slug). O shape atual de `Product` no frontend:

```ts
// Verificar frontend/src/shared/types/ ou contracts/
type Product = {
  id: string;   // DEVE ser UUID, ex: "b0000000-..."
  key: string;  // slug para exibição, ex: "maestro-beton"
  name: string;
  // ...
};
```

Se `id` hoje carrega o slug em vez do UUID: o `GET /api/v1/products` retorna ambos (`ProductSummary.java` tem `UUID id` e `String key`). Mapear corretamente na resposta.

### A.3 — Auditar e corrigir todos os services

Buscar todas as ocorrências de `effectiveProduct?.key`, `effectiveProduct?.name`, `product.key` sendo interpolados em paths de API:

```bash
cd frontend && grep -rn "\/\${.*key\}\|\/\${.*slug\}\|\/\${effectiveProduct\?\.key" src/domains/*/services/ src/core/*/services/
```

Para cada ocorrência: substituir pela propriedade `id` correspondente.

**Lista confirmada pelo audit (substituir `key` → `id` nos paths):**
- `productsService.ts`: linhas com `/products/${product.key || product.id}` → `/products/${product.id}`
- `pagesService.ts`: slug em `/products/{slug}/pages` → UUID
- `globalsService.ts`: idem
- `eventsService.ts`: idem
- `contentService.ts`: idem
- `formsService.ts`: idem
- `analyticsService.ts`: idem
- `assetsService.ts`: idem
- `knowledgeService.ts`: idem

---

## B. Placeholders literais não substituídos

O audit identificou chamadas com strings literais `"{productId}"`, `"{contentId}"`, `"{formId}"` nunca substituídas. Exemplo de bug:

```ts
// ERRADO — placeholder literal nunca substituído:
return apiClient.put('/products/{productId}/settings', body);

// CORRETO:
const productId = effectiveProduct?.id;
return apiClient.put(`/products/${productId}/settings`, body);
```

Buscar e corrigir todas as ocorrências:

```bash
cd frontend && grep -rn '"{productId}"\|"{contentId}"\|"{formId}"\|"{assetId}"\|"{formId}"' src/
# também buscar pela interpolação incompleta:
grep -rn "'\/{[a-zA-Z]*Id\}'" src/
```

**Lista confirmada:**
- `productsService.ts`: `PUT "/products/{productId}/settings"` e `PUT "/products/{productId}"` (linhas ~117, ~123)
- `contentService.ts`: `PUT "/content/{contentId}"`, `POST "/content/{contentId}/transition"` (linhas ~130, ~134)
- `formsService.ts`: `POST "/forms/{formId}/test-submit"` (linha ~163)
- `analyticsService.ts`: múltiplos `GET/POST "/analytics/..."` sem prefixo de produto
- `knowledgeService.ts`: `{productId}` literal em listNodes/listEdges (linhas ~38-57)

---

## C. Prefixos de produto obrigatórios

Vários services chamam endpoints sem o prefixo `/products/{productId}/` obrigatório:

| Service | Endpoint errado | Endpoint correto |
|---|---|---|
| `contentService` | `GET /content` | `GET /products/{id}/content` |
| `contentService` | `GET /content/{id}` | `GET /products/{id}/content/{contentId}` |
| `formsService` | `GET /forms` | `GET /products/{id}/forms` |
| `formsService` | `GET /forms/{id}` | `GET /products/{id}/forms/{formId}` |
| `formsService` | `GET /forms/{id}/submissions` | `GET /products/{id}/forms/{formId}/submissions` |
| `auditService` | `GET /audit/events` | `GET /tenants/{tenantId}/audit-events` |
| `auditService` | `POST /audit/events` | **REMOVER** — auditoria é server-side, frontend nunca cria eventos de auditoria |
| `knowledgeService` | `GET /graph/nodes/{id}/preview` | `GET /products/{id}/graph/nodes/{nodeId}/preview` |
| `analyticsService` | `GET /analytics/*` | `GET /products/{id}/analytics/*` |

Para cada correção, o service precisa receber `productId` como parâmetro (via argumento ou via `useAuth().effectiveProduct.id`). Preferir receber como argumento para facilitar testes.

---

## D. Paths divergentes pontuais

| Service | Errado | Correto |
|---|---|---|
| `notificationsService` | `GET /notifications/me` | `GET /notifications/mine` |
| `notificationsService` | `GET /notifications/me/pending-modal` | `GET /notifications/mine/pending-modal` |
| `productAssignmentsService` | `GET /products/{id}/assignments` | `GET /products/{id}/users` |
| `productAssignmentsService` | `POST /products/{id}/assignments` | `POST /products/{id}/users` |
| `productsService` | `GET /products/check-slug` | **REMOVER** — endpoint não existe; validação de slug é feita no `POST /products` (400 se slug duplicado) |
| `productsService` | `POST /products/{id}/archive` | **REMOVER** — backend não tem archive; usar `PUT /products/{id}` com `status: "ARCHIVED"` se necessário, ou registrar como retrofit |
| `usersService` | `{userId}` no path usa **email** do usuário | Deve usar `userId` (string Keycloak subject) — ver Sprint 06 |

---

## E. `apiClient.ts` — suporte a multipart

`frontend/src/shared/services/apiClient.ts` (linha ~42) fixa `Content-Type: application/json` em todos os requests, impossibilitando upload de arquivos.

### E.1 — Adicionar método `upload`

```ts
// apiClient.ts
async upload<T>(path: string, formData: FormData): Promise<T> {
  const token = tokenProvider?.() ?? '';
  const response = await fetch(`${BASE_URL}${path}`, {
    method: 'POST',
    headers: {
      // NÃO setar Content-Type — o browser seta automaticamente com o boundary correto
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: formData,
  });
  if (!response.ok) throw await response.json();
  return response.json();
},
```

### E.2 — `assetsService.upload` usar o novo método

```ts
// assetsService.ts
async upload(file: File, productId: string, metadata?: AssetMetadata): Promise<{ assetId: string }> {
  if (IS_API_MODE) {
    const fd = new FormData();
    fd.append('file', file);
    if (metadata?.friendlyName) fd.append('friendlyName', metadata.friendlyName);
    return apiClient.upload<{ assetId: string }>(`/products/${productId}/assets`, fd);
  }
  // mock: gerar assetId fake
  const assetId = `mock-asset-${Date.now()}`;
  assetsStore.push({ assetId, name: file.name, url: URL.createObjectURL(file), ...metadata });
  return { assetId };
},
```

---

## F. Mapeamento de contratos por domínio — tipos a corrigir

### F.1 — `ProductSummary`

Backend devolve:
```json
{ "id": "UUID", "tenantId": "UUID", "key": "maestro-beton", "type": "Site Institucional", "status": "ACTIVE", "defaultLocale": "pt-BR", "createdAt": "...", "updatedAt": "..." }
```

Frontend espera:
```ts
{ id?, name, type: string, status: "Ativo"|"Pendente"|"Arquivado"|"Sem módulos", modules: number, last, score, modulesList? }
```

Criar `mapProductSummary(dto: ProductSummaryDto): ProductSummary` que:
- Converte `status: "ACTIVE"` → `"Ativo"`, `"ARCHIVED"` → `"Arquivado"`, etc.
- `modules`: buscar de `GET /products/{id}/modules` ou incluir no endpoint de listagem
- `name`: o backend não tem `name` em `ProductSummary`? Verificar — se `ProductSummary.java` não tem `name`, adicionar ao DTO do backend ou remover do frontend

### F.2 — `TenantSummary`

Backend: `{ id, key, name, status: "ACTIVE"|"SUSPENDED", plan, createdAt, updatedAt }`
Frontend: `{ plan, productCount, lastAccess, status: "ativo"|"suspenso" }`

Mapper: `status: "ACTIVE"` → `"ativo"`, sem `productCount`/`lastAccess` (remover do frontend ou adicionar endpoint de summary estendido no backend).

### F.3 — Roles

Backend retorna roles Keycloak no JWT: `AEGIS_SUPER_ADMIN`, `AEGIS_TENANT_ADMIN`, etc.
Frontend usa: `"super_admin"`, `"tenant_admin"`, etc.

`roleMapper.ts` criado na Sprint 01 (Seção D.3) resolve isso.

---

## J. Frontend — silenciar todos os logs em produção

### J.1 — Diagnóstico

A auditoria revelou dois vetores de vazamento de logs em produção:

1. `frontend/src/shared/services/devLog.ts` chama `console.log()` sem nenhum guard — a função `logApiCall()` é chamada por todos os services em modo mock, e se `IS_API_MODE` for `false` em produção por qualquer razão, os logs aparecem no console do usuário.
2. `vite.config.ts` não configura `esbuild.drop` — o build de produção não elimina chamadas `console.*` do bundle final.

### J.2 — Corrigir `devLog.ts`

```ts
// frontend/src/shared/services/devLog.ts
// ANTES:
export function logApiCall(method: string, path: string, payload?: unknown) {
  console.log(`[mock→backend] ${method} ${path}`, payload ?? '');
}

// DEPOIS:
export function logApiCall(method: string, path: string, payload?: unknown) {
  if (!import.meta.env.PROD) {
    console.log(`[mock→backend] ${method} ${path}`, payload ?? '');
  }
}
```

`import.meta.env.PROD` é `true` apenas em `vite build` — em dev (`vite dev`) é `false`. O Vite faz tree-shaking do bloco morto em produção.

### J.3 — `vite.config.ts` — eliminar `console.*` no build de produção

Adicionar opção `esbuild` na seção `build`:

```ts
// vite.config.ts
export default defineConfig({
  // ... plugins, resolve, server existentes ...

  build: {
    outDir: 'dist',
    emptyOutDir: true,

    // Remove toda chamada console.* e debugger do bundle de produção.
    // esbuild faz isso em tempo de transformação (antes do bundling),
    // então não afeta tree-shaking nem source maps de dev.
    esbuild: {
      drop: import.meta.env?.MODE === 'production'
        ? ['console', 'debugger']
        : [],
    },
  },
})
```

**Nota:** a forma correta no Vite é via `esbuild` na raiz do `defineConfig`, não dentro de `build`. Usar a sintaxe:

```ts
export default defineConfig(({ mode }) => ({
  // ...
  esbuild: {
    drop: mode === 'production' ? ['console', 'debugger'] : [],
  },
  build: {
    outDir: 'dist',
    emptyOutDir: true,
  },
  // ...
}))
```

### J.4 — `console.warn` no `knowledgeService.ts` (mock paths)

`knowledgeService.createEdge()` tem dois `console.warn()` que já estão dentro de blocos `if (!IS_API_MODE)` implicitamente (são warnings de validação do mock). Mesmo assim, envolve-los com o mesmo guard de `devLog.ts` por consistência:

```ts
// knowledgeService.ts — createEdge()
if (!allNodes.some(n => n.id === to)) {
  if (!import.meta.env.PROD) {
    console.warn(`knowledgeService.createEdge: nó "${to}" não existe — edge não criada.`);
  }
  return;
}
```

### J.5 — Verificação obrigatória antes do merge

```bash
# Build de produção
cd frontend && VITE_API_MODE=api npm run build

# O bundle de prod não deve conter nenhum console.log/warn/error
grep -r "console\.\(log\|warn\|error\|info\|debug\)" dist/assets/*.js \
  && echo "FALHOU — logs vazando em prod" \
  || echo "OK — sem logs no bundle de prod"

# Deve retornar "OK"
```

---

## K. Backend — logging estruturado com `@Slf4j`

### K.1 — Diagnóstico

A auditoria encontrou **zero** classes com `@Slf4j` (Lombok). Apenas 8 classes usam `LoggerFactory.getLogger()` manualmente. Os 55+ services de domínio não têm nenhum log estruturado — falhas silenciosas em produção são impossíveis de diagnosticar.

### K.2 — Padrão de logging adotado

Usar `@Slf4j` do Lombok (já presente no `pom.xml` como dependência do projeto). Não usar `LoggerFactory.getLogger()` manual — substituir pelos poucos casos que fazem isso.

Níveis por contexto:

| Nível | Quando usar |
|---|---|
| `log.debug()` | Início de método público, parâmetros relevantes, decisões internas |
| `log.info()` | Operações de escrita concluídas (create/update/delete) |
| `log.warn()` | Caminhos degradados esperados (ex.: Telegram falhou, tentativa retentada) |
| `log.error()` | Exceções não esperadas que foram capturadas |

**Nunca logar senhas, tokens, CPFs ou qualquer dado sensível** — logar apenas IDs, slugs e contagens.

### K.3 — Template de adição por service

Cada service deve seguir o padrão abaixo. Exemplo com `TenantService`:

```java
// ANTES:
@Service
@RequiredArgsConstructor
public class TenantService {
    public TenantSummary create(CreateTenantCommand cmd) {
        // lógica sem log
    }
}

// DEPOIS:
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    public TenantSummary create(CreateTenantCommand cmd) {
        log.debug("create: name='{}', plan='{}'", cmd.name(), cmd.plan());
        TenantSummary result = // ... lógica existente ...
        log.info("create: tenant criado id='{}', key='{}'", result.id(), result.key());
        return result;
    }

    public void suspend(UUID tenantId) {
        log.debug("suspend: tenantId='{}'", tenantId);
        // ... lógica existente ...
        log.info("suspend: tenant suspenso id='{}'", tenantId);
    }
}
```

### K.4 — Services prioritários (adicionar `@Slf4j` primeiro)

Por impacto em diagnóstico de produção, priorizar nesta ordem:

1. `AuthService` — login, refresh, logout
2. `TenantService` — create, suspend, restore
3. `ProductService` — create, update, archive
4. `FeedbackService` — create, status update
5. `NotificationService` — create, fan-out
6. `AssetService` — upload, archive
7. `FormService` — create, publish
8. `KnowledgeGraphService` — createEdge, listOrphans
9. `TelegramFeedbackNotifier` / `FormSubmissionTelegramNotifier` — dispatch (já tem `LoggerFactory` manual → migrar para `@Slf4j`)
10. Todos os demais services em ordem alfabética

### K.5 — Pontos estratégicos por domínio

**Auth:**
```java
log.debug("login: username='{}'", request.username());
log.info("login: autenticação bem-sucedida para subject='{}'", subject);
log.warn("login: falha de autenticação para username='{}'", request.username());
```

**Assets (upload):**
```java
log.debug("upload: productId='{}', fileName='{}', size={}B", productId, file.getOriginalFilename(), file.getSize());
log.info("upload: asset salvo id='{}', path='{}'", assetId, storagePath);
```

**Telegram (best-effort — deve logar warn, não error):**
```java
log.debug("dispatch: tentando envio Telegram para chatId='{}'", chatId);
log.warn("dispatch: Telegram falhou para chatId='{}' — {}", chatId, e.getMessage());
// NUNCA: log.error() aqui — falha de Telegram não é erro crítico do sistema
```

**Knowledge Graph:**
```java
log.debug("createEdge: from='{}', to='{}', type='{}'", from, to, edgeType);
log.warn("createEdge: nó de destino '{}' não encontrado — edge ignorada", to);
```

### K.6 — Substituir `LoggerFactory` manual por `@Slf4j`

Os 8 arquivos que usam `LoggerFactory.getLogger()` manual devem ser migrados:

```java
// ANTES:
private static final Logger log = LoggerFactory.getLogger(TelegramFeedbackNotifier.class);

// DEPOIS (remover a linha acima e adicionar @Slf4j na classe):
@Slf4j
public class TelegramFeedbackNotifier {
  // log.* disponível automaticamente
}
```

### K.7 — Cobertura JaCoCo

`log.debug()` e `log.info()` são statements simples que não criam branches — se o método está coberto pelo teste existente, as linhas de log são automaticamente cobertas. **Não é necessário escrever novos testes apenas para os logs.**

Exceção: `log.warn()` dentro de um `catch` ou `if` cria uma branch nova. Esses casos precisam de teste específico:

```java
// Este warn cria uma branch que precisa ser testada:
if (result == null) {
    log.warn("findById: produto '{}' não encontrado", id);
    return Optional.empty();
}
```

Para a branch `null`, o teste mock deve cobrir:
```java
// Teste:
when(repo.findById(id)).thenReturn(Optional.empty());
assertThat(service.findById(id)).isEmpty();
// JaCoCo: a linha do log.warn() está coberta ✓
```

### K.8 — Configuração de log por ambiente

`backend/src/main/resources/application.yml` — adicionar seção de logging:

```yaml
logging:
  level:
    br.com.byop.aegis: INFO          # padrão: INFO em produção
    br.com.byop.aegis.security: WARN  # Security: só warns e erros
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
```

`backend/src/main/resources/application-local.yml` — adicionar:
```yaml
logging:
  level:
    br.com.byop.aegis: DEBUG          # DEBUG completo em local/dev
```

`backend/src/main/resources/application-prod.yml` — adicionar:
```yaml
logging:
  level:
    br.com.byop.aegis: INFO
    br.com.byop.aegis.security: WARN
    org.springframework: WARN         # reduzir ruído do framework em prod
  pattern:
    # JSON para ingestão por ferramentas de observabilidade (Grafana Loki, etc.)
    console: '{"timestamp":"%d{ISO8601}","level":"%level","logger":"%logger","message":"%message"}%n'
```

### K.9 — Validação JaCoCo

Após adicionar os logs, rodar:

```bash
cd backend && mvn test jacoco:report
# Abrir target/site/jacoco/index.html
# Todos os services alterados devem manter >= 80% de cobertura de linha
# (ou 100% se já estavam em 100% antes)

# No CI, o check de cobertura mínima deve estar em jacoco-check:
# (configurado no pom.xml — verificar se existe <rule> de mínimo)
```

Se `pom.xml` não tem `jacoco-maven-plugin` com `check` goal, adicionar:
```xml
<execution>
  <id>jacoco-check</id>
  <goals><goal>check</goal></goals>
  <configuration>
    <rules>
      <rule>
        <element>BUNDLE</element>
        <limits>
          <limit>
            <counter>LINE</counter>
            <value>COVEREDRATIO</value>
            <minimum>0.80</minimum>  <!-- mínimo 80% geral; services novos devem ter 100% -->
          </limit>
        </limits>
      </rule>
    </rules>
  </configuration>
</execution>
```

---

## G. Validação

```bash
# Nenhum placeholder literal restante nos services
cd frontend && grep -rn '"{[a-zA-Z]*Id\}\|"{[a-zA-Z]*Slug\}"' src/domains/*/services/ src/core/*/services/
# esperado: zero resultados

# Nenhum path sem prefixo de produto onde necessário
grep -rn "apiClient\.\(get\|post\|put\|delete\)('" src/domains/content/services/ | grep -v "/products/"
# esperado: zero resultados
```

---

## H. Critérios de aceite

**Contratos:**
- [ ] Nenhum service usa `product.key`/slug em paths de API — todos usam `product.id` (UUID).
- [ ] Nenhum placeholder literal `{productId}`, `{contentId}`, `{formId}` em chamadas de API.
- [ ] `POST /api/v1/audit/events` removido do `auditService` frontend.
- [ ] `apiClient.upload()` existe e envia `FormData` sem `Content-Type` fixo.
- [ ] `assetsService.upload()` usa `apiClient.upload()` em modo API.

**Frontend — logs:**
- [ ] Build de produção (`VITE_API_MODE=api npm run build`) não contém `console.log`, `console.warn`, `console.error` no bundle.
  ```bash
  grep -r "console\." dist/assets/*.js && echo "FALHOU" || echo "OK"
  ```
- [ ] `devLog.ts` guarda o `console.log` com `!import.meta.env.PROD`.
- [ ] `vite.config.ts` tem `esbuild: { drop: ['console', 'debugger'] }` no mode `production`.
- [ ] Em `vite dev` (dev mode), `logApiCall()` continua logando normalmente — sem regressão de DX.

**Backend — logging:**
- [ ] Todos os services de domínio têm `@Slf4j`.
- [ ] Nenhum service usa `LoggerFactory.getLogger()` manual — todos migrados para `@Slf4j`.
- [ ] `log.debug()` presente na entrada de cada método público de service.
- [ ] `log.info()` presente após operações de escrita (create/update/delete).
- [ ] `log.warn()` nos caminhos de fallback/degradado (Telegram falhou, nó não encontrado).
- [ ] Nenhum log contém senha, token, CPF ou dado sensível.
- [ ] `mvn test jacoco:report` — serviços alterados mantêm cobertura anterior (sem regressão de JaCoCo).
- [ ] `application-local.yml` tem `logging.level.br.com.byop.aegis: DEBUG`.
- [ ] `application-prod.yml` tem `logging.level.br.com.byop.aegis: INFO` com pattern JSON.

**Geral:**
- [ ] Modo mock completo continua funcionando (nenhuma regressão).
- [ ] `npm run typecheck` — zero erros de tipo.
- [ ] `mvn verify` — todos os testes backend passando.

---

## I. Commit sugerido

```bash
# Frontend: contratos + logs
git add frontend/src/ frontend/vite.config.ts
git commit -m "fix(integration): slug->UUID nos paths de API, placeholders literais, multipart e remocao de logs em prod"

# Backend: @Slf4j + logging estruturado
git add backend/src/main/java/ backend/src/main/resources/application*.yml
git commit -m "feat(backend): logging estruturado com @Slf4j em todos os services; DEBUG em local, INFO+JSON em prod"
```
