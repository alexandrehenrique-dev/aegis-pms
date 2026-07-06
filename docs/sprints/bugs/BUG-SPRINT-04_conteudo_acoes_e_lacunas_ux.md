# Bug Sprint 04 — Lacunas de UX e Contrato no Módulo de Conteúdo

> **Contexto:** Sprint de auditoria de jornadas do módulo `Conteúdo`. Bugs identificados durante uso real do sistema: ações de ciclo de vida (arquivar, excluir) ausentes ou com contrato quebrado, checklist de publicação hardcoded, e lacunas de acesso rápido na lista de conteúdos.
>
> **Arquivo em construção incremental — NÃO modificar durante commits de implementação.**
>
> **Branch sugerida:** `bugfix/sprint-04-conteudo-acoes-ux`

---

## F.1 — Lista de conteúdos: ações de ciclo de vida ausentes (Arquivar / Excluir)

**Módulos afetados:** `frontend/src/domains/content/pages/ContentDataGrid.tsx`, `contentService.ts`, `backend/.../content/controller/ContentController.java`

**Comportamento observado:** Na lista de conteúdos (`ContentDataGrid`), cada linha exibe apenas três ações: `Abrir`, `Preview`, `Histórico`. Não há ação de arquivar ou excluir diretamente da lista — o usuário precisa abrir o conteúdo, navegar até o `PublishPanel` e executar a ação de lá.

**Diagnóstico — não é regra de sistema, é lacuna de UX:**

O backend suporta arquivamento via `POST /products/{productId}/content/{contentId}/transition`. O `contentService.archive()` existe e é chamado pelo `PublishPanel`. Nenhuma regra de permissão bloqueia a ação na lista — simplesmente o botão não foi adicionado ao `ContentDataGrid`.

```tsx
// ContentDataGrid.tsx — ações atuais (linhas 104-106):
<PermGate allowed={canEdit}><Button onClick={() => navigate(`/content/${r.id}/editor`)}>Abrir</Button></PermGate>
<Button onClick={() => navigate(`/content/${r.id}/preview`)}>Preview</Button>
<Button onClick={() => navigate(`/content/${r.id}/versions`)}>Histórico</Button>
// ← sem Arquivar, sem Excluir
```

**F.1.1 — Arquivar: ação existe, só não está na lista**

`contentService.archive()` existe e funciona:
```typescript
async archive(id?: string, productId = requireCurrentProductId()): Promise<void> {
  if (IS_API_MODE && id)
    return apiClient.post(`/products/${productId}/content/${id}/transition`, { from: "Published", to: "Archived" });
}
```

Mas há um **bug adicional**: o `from` está hardcoded como `"Published"`. Se o conteúdo estiver em `Draft` ou `In Review`, a transição vai falhar no backend (que valida que `from` corresponde ao status atual). A ação deveria usar o status atual do conteúdo como `from`.

**F.1.2 — Excluir: endpoint backend inexistente**

`contentService.deleteContent()` existe no frontend:
```typescript
async deleteContent(id: string): Promise<void> {
  if (IS_API_MODE) return apiClient.delete(`/products/${requireCurrentProductId()}/content/${id}`);
}
```

Mas o `ContentController` **não tem `@DeleteMapping`**:
```java
// ContentController.java — endpoints existentes:
@PostMapping("/api/v1/products/{productId}/content")
@GetMapping("/api/v1/products/{productId}/content")
@GetMapping("/api/v1/products/{productId}/content/{contentId}")
@PutMapping("/api/v1/products/{productId}/content/{contentId}")
@PostMapping("/api/v1/products/{productId}/content/{contentId}/transition")
@GetMapping("/api/v1/products/{productId}/content/{contentId}/versions")
@PostMapping("/api/v1/products/{productId}/content/{contentId}/publish")
@GetMapping("/api/v1/products/{productId}/content/edit-events")
@GetMapping("/api/v1/products/{productId}/content/workflow-items")
// ← sem DELETE
```

Chamada a `deleteContent()` retorna 404 ou 405. O botão de exclusão também não aparece na UI (`ContentDataGrid` nem o expõe).

**Decisão aprovada:** exclusão é suportada, restrita a `SUPER_ADMIN` e `TENANT_ADMIN`, e apenas para conteúdo em `DRAFT` que **nunca foi publicado** (`currentVersion == 1` e `status == DRAFT`). Conteúdo que já passou por `PUBLISHED` só pode ser arquivado — jamais excluído. Sem ADR adicional; esta regra entra como invariante documentada no contrato.

**Implementação necessária:**

**F.1.1 — Adicionar ação "Arquivar" na lista com status dinâmico:**

```tsx
// ContentDataGrid.tsx — botão "Arquivar" condicional por status:
{(r.status === "Published" || r.status === "In Review") && (
  <PermGate allowed={canEdit}>
    <Button onClick={() => handleArchive(r.id, r.status)}>Arquivar</Button>
  </PermGate>
)}
```

```typescript
// contentService.ts — corrigir from hardcoded:
async archive(id: string, currentStatus: string, productId = requireCurrentProductId()): Promise<void> {
  if (IS_API_MODE)
    return apiClient.post(`/products/${productId}/content/${id}/transition`, {
      from: currentStatus,  // dinâmico — nunca hardcoded "Published"
      to: "Archived",
    });
  // mock mode:
  const item = contentsStore.find((c) => c.id === id);
  if (item) item.status = "Archived";
}
```

**F.1.2 — Excluir: implementar endpoint no backend e exposição na lista:**

```java
// ContentController.java — novo endpoint:
@DeleteMapping("/api/v1/products/{productId}/content/{contentId}")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void deleteContent(@PathVariable UUID productId,
                          @PathVariable UUID contentId,
                          Authentication authentication) {
    AuthenticatedUser caller = assertProductAccess(authentication, productId);
    contentService.deleteContent(productId, contentId, caller);
}
```

```java
// ContentService.java — lógica completa:
private static final String ROLE_SUPER_ADMIN  = "ROLE_SUPER_ADMIN";
private static final String ROLE_TENANT_ADMIN = "ROLE_TENANT_ADMIN";

public void deleteContent(UUID productId, UUID contentId, AuthenticatedUser caller) {
    boolean isAdmin = caller.authorities().contains(ROLE_SUPER_ADMIN)
                   || caller.authorities().contains(ROLE_TENANT_ADMIN);
    if (!isAdmin) {
        throw new InsufficientContentRoleException(
            "Apenas SUPER_ADMIN ou TENANT_ADMIN podem excluir conteúdo.");
    }

    Content content = contentRepository.findByIdAndProductId(contentId, productId)
        .orElseThrow(() -> new ContentNotFoundException(contentId));

    // Nunca excluir conteúdo que já foi publicado
    boolean neverPublished = content.getStatus() == ContentStatus.DRAFT
                          && content.getCurrentVersion() == 1;
    if (!neverPublished) {
        throw new InvalidContentTransitionException(
            "Apenas conteúdo em Draft que nunca foi publicado pode ser excluído. " +
            "Use a transição para Archived.");
    }

    auditService.record(productId, caller.subject(), "CONTENT_DELETED", contentId.toString());
    versionRepository.deleteAllByContentId(contentId);
    contentRepository.delete(content);
}
```

> **Nota de consistência:** o padrão de prefixo `ROLE_` nos authorities varia no codebase — `SettingsService` usa `ROLE_SUPER_ADMIN`, `PermissionMatrixDefaults` usa `SUPER_ADMIN`. Verificar o token real emitido pelo Keycloak e alinhar uma constante compartilhada em `AegisRoles` (oportunidade de refactoring).

```typescript
// contentService.ts — mock mode para deleteContent():
async deleteContent(id: string): Promise<void> {
  if (IS_API_MODE)
    return apiClient.delete(`/products/${requireCurrentProductId()}/content/${id}`);
  // mock: remover do store em memória
  const idx = contentsStore.findIndex((c) => c.id === id);
  if (idx !== -1) contentsStore.splice(idx, 1);
}
```

```tsx
// ContentDataGrid.tsx — botão "Excluir" apenas para Draft nunca publicado:
{r.status === "Draft" && (
  <PermGate allowed={viewAsRole === "super_admin" || viewAsRole === "tenant_admin"}>
    <Button
      variant="destructive"
      onClick={() => {
        if (confirm("Excluir permanentemente este rascunho?")) {
          contentService.deleteContent(r.id).then(() => refresh());
        }
      }}
    >
      Excluir
    </Button>
  </PermGate>
)}
```

Também adicionar caso ao **Bruno**:
- `DELETE /products/{productId}/content/{contentId}` com Draft → `204 No Content`
- mesma rota com conteúdo Published → `400` com mensagem descritiva
- mesma rota com role editor → `403`
- `GET` após delete → `404`

**Cobertura JaCoCo:** `ContentService.deleteContent()` + handler no `ContentExceptionHandler` devem atingir 100% — incluir testes para os três ramos (role inválido, status inválido, sucesso).

**Critério de aceite:**
- [ ] Lista exibe botão "Arquivar" para conteúdos `Published` e `In Review`.
- [ ] Lista exibe botão "Excluir" apenas para `Draft` + roles `super_admin`/`tenant_admin`.
- [ ] `archive()` usa `currentStatus` como `from` — nunca hardcoded.
- [ ] `DELETE /content/{contentId}` → 204 para Draft nunca publicado / 400 para demais status / 403 para roles sem permissão.
- [ ] Deleção registra evento de auditoria antes de remover.
- [ ] Mock mode: `deleteContent()` remove item do store em memória; `archive()` atualiza status local.
- [ ] Bruno: todos os 4 cenários acima passam.
- [ ] JaCoCo: 100% nas novas linhas de `deleteContent()`.

**Smoke test:**
```
1. Criar conteúdo → status Draft
2. Lista → verificar: sem botão Arquivar (Draft não é arquivável diretamente)
3. Transitar para Published (via Workflow ou PublishPanel)
4. Lista → verificar: botão "Arquivar" visível
5. Clicar Arquivar → confirmar → status muda para Archived
6. Filtrar por Archived → conteúdo visível com status correto
```

---

## F.2 — `PublishPanel`: checklist de pré-publicação hardcoded

**Módulos afetados:** `frontend/src/domains/content/pages/PublishPanel.tsx`

**Comportamento observado:** O checklist "Checklist pré-publicação" exibe sempre os mesmos itens com valores fixos, independentemente do conteúdo real:

```tsx
// PublishPanel.tsx — hardcoded:
{([
  ["SEO completo", false],
  ["Traduções completas", false],
  ["Assets com alt text", false],
  ["Formulário vinculado", true],
] as [string, boolean][]).map(([x, ok]) => (...))}
```

O item "Formulário vinculado" sempre aparece como ✓ (`true`), mesmo que o conteúdo não tenha formulário. Os demais sempre aparecem como ✗ (`false`), mesmo que o conteúdo tenha SEO completo.

**Implementação necessária:**

Derivar o checklist a partir do conteúdo real carregado. O `ContentEditor` já carrega o conteúdo via `contentService.getContent()` — o `PublishPanel` pode receber o conteúdo como prop ou buscá-lo por `id`.

```typescript
// Checklist derivado de dados reais:
const checks = [
  {
    label: "SEO completo",
    ok: !!(content?.metadataJson && JSON.parse(content.metadataJson).title),
  },
  {
    label: "Traduções completas",
    ok: content?.translations?.length > 0,
  },
  {
    label: "Assets com alt text",
    ok: true, // derivar de assets vinculados quando endpoint disponível
  },
];
```

**Critério de aceite:**
- [ ] Checklist reflete estado real do conteúdo.
- [ ] "Formulário vinculado" só aparece como ✓ se `content.formId` está preenchido.
- [ ] "SEO completo" deriva dos metadados reais do conteúdo.

---

## F.3 — `PublishPanel`: ações sem tratamento de erro

**Módulos afetados:** `frontend/src/domains/content/pages/PublishPanel.tsx`

Todas as actions do `PublishPanel` (`handleArchive`, `handlePublish`, `handleSaveDraft`, `handleSchedule`, `handleSubmitForReview`) usam `try/finally` sem `catch` — erros do backend são silenciados:

```typescript
const handleArchive = async () => {
  setArchiving(true);
  try {
    await contentService.archive(id, productId); // pode lançar 400/422
    toast.success("Conteúdo arquivado.");
  } finally {
    setArchiving(false); // erro silenciado — sem toast.error
  }
};
```

**Implementação necessária:** Adicionar `catch` em todas as 5 funções com `toast.error` descritivo — mesmo padrão do fix proposto em E.6.2.

**Critério de aceite:**
- [ ] Falha em qualquer ação do PublishPanel → toast de erro com mensagem do backend.
- [ ] Sucesso → toast de sucesso (já implementado).

---

---

## F.4 — `FormsTimeline`: dados completamente hardcoded

**Módulos afetados:** `frontend/src/domains/forms/components/FormsTimeline.tsx`, `formsService.ts`

**Comportamento observado:** O painel "Timeline Forms" no `FormsDashboard` (e o componente `FormsTimeline` reutilizado em `SubmissionDetails`) exibe sempre os mesmos quatro eventos fictícios, com timestamps calculados como `há {i + 1} h`, independentemente do produto ou de qualquer dado real:

```tsx
// FormsTimeline.tsx — array literal hardcoded:
{["Novo orçamento recebido", "Novo contato enviado", "RSVP confirmado", "Formulário publicado"].map((t, i) => (
  <div key={t}>
    <p>{t}</p>
    <p>há {i + 1} h · {effectiveProduct?.name ?? "Produto"} → Forms</p>
  </div>
))}
```

Nenhuma chamada a API ou serviço é feita — o componente é puro mock decorativo.

**Causa raiz:** componente criado como placeholder visual; nunca conectado a uma fonte de dados real.

**Fonte de dados real disponível:** o backend tem o endpoint de auditoria com filtro por módulo:

```
GET /api/v1/tenants/{tenantId}/audit-events?module=FORM&productId={productId}
```

O `AuditEventSummary` retorna `{ id, actor, action, target, module, time, risk }` — `action` e `target` compõem a descrição do evento (ex.: `FORM_PUBLISHED` + nome do formulário).

> **Nota de escopo:** eventos de submissão ("Novo orçamento recebido") são operacionais, não de auditoria. Para incluí-los no timeline, seria necessário um endpoint agregado (`GET /products/{productId}/forms/activity`) ou derivar dos dados de `listSubmissions` por formulário. Como a timeline do dashboard serve como **visão de ciclo de vida**, filtrar por `module=FORM` no endpoint de audit já cobre os casos mais relevantes (publicação, despublicação, criação). Submissões recentes pertencem à aba "Submissions".

**Implementação necessária:**

```typescript
// formsService.ts — novo método:
async listActivity(tenantId: string, productId: string): Promise<AuditEventSummary[]> {
  if (IS_API_MODE)
    return apiClient.get<AuditEventSummary[]>(
      `/tenants/${tenantId}/audit-events?module=FORM&productId=${productId}`
    );
  // mock mode: retornar array vazio — sem dados fabricados
  return [];
}
```

```tsx
// FormsTimeline.tsx — conectado à API:
export function FormsTimeline() {
  const { effectiveProduct, effectiveTenant } = useAuth();
  const { data: events = [] } = useAsyncData(
    () =>
      effectiveTenant?.id && effectiveProduct?.id
        ? formsService.listActivity(effectiveTenant.id, effectiveProduct.id)
        : Promise.resolve([]),
    [effectiveTenant?.id, effectiveProduct?.id]
  );

  if (events.length === 0)
    return <p className="text-sm text-muted-foreground">Nenhuma atividade recente.</p>;

  return (
    <div className="space-y-1">
      {events.map((e) => (
        <div key={e.id} className="flex gap-3 rounded-xl p-3 hover:bg-muted">
          <span className="mt-1 h-2.5 w-2.5 rounded-full bg-primary" />
          <div>
            <p className="text-sm font-medium">{e.action}: {e.target}</p>
            <p className="text-xs text-muted-foreground">{e.time} · {effectiveProduct?.name ?? "Produto"} → Forms</p>
          </div>
        </div>
      ))}
    </div>
  );
}
```

**Critério de aceite:**
- [ ] `FormsTimeline` carrega eventos reais via `GET .../audit-events?module=FORM&productId=...`.
- [ ] Estado vazio (`[]`) exibe mensagem "Nenhuma atividade recente." em vez de dados falsos.
- [ ] Em mock mode: retorna `[]` sem fabricar dados.
- [ ] `SubmissionDetails` reutiliza o mesmo componente corrigido.

---

## F.5 — `PublicationPanel`: tela de publicação completamente estática

**Módulos afetados:** `frontend/src/domains/forms/pages/PublicationPanel.tsx`, `app/routes/index.tsx`

**Comportamento observado:** A tela "Publicação do Formulário" exibe canais de publicação com valores completamente hardcoded — URL, Embed, Script, Iframe, Domínio e Status são strings literais que nunca refletem o formulário ou produto atual:

```tsx
// PublicationPanel.tsx — linhas 14-15: constantes globais hardcoded
const EMBED_SNIPPET = "<aegis-form id=contato-comercial />";
const FORM_ID = "form-contato-comercial";

// linha 71: card de canais completamente estático
{[
  ["URL",     "https://maestrobeton.com/forms/contato"],
  ["Embed",   "<aegis-form id=contato-comercial />"],
  ["Script",  '<script src="/aegis/forms.js"></script>'],
  ["Iframe",  '<iframe src="/forms/contato"></iframe>'],
  ["Domínio", "maestrobeton.com"],
  ["Status",  "Publicado e rastreável"],
].map(...)}
```

**F.5.1 — `FORM_ID` hardcoded: rota sem parâmetro**

A rota atual é `/forms/publication` (sem `:formId`). O `PublicationPanel` nunca sabe qual formulário está sendo exibido:

```typescript
// routes/index.tsx linha 160:
<Route path="/forms/publication" element={<PublicationPanel />} />
// ← sem :formId na rota
```

Como consequência:
- `formsService.getDelivery(productId, FORM_ID)` sempre carrega/salva a configuração do formulário "form-contato-comercial", independentemente do formulário selecionado.
- `formsService.publish(productId, FORM_ID)` publica o formulário errado.
- `handleCopyEmbed()` copia sempre o mesmo snippet hardcoded.

**F.5.2 — "Canais de publicação" nunca reflete dados reais**

O backend já retorna o campo `publication` no `FormDetail`:

```java
public record FormDetail(
    UUID id, String name, String type, String status,
    List<Map<String, Object>> fields,
    List<Map<String, Object>> deliveryChannels,
    String publication,   // ← URL pública do formulário publicado
    OffsetDateTime createdAt, OffsetDateTime updatedAt
) {}
```

Este campo deve ser a fonte do item "URL". Os demais canais (Embed, Iframe) derivam do `id` real do formulário. "Domínio" vem do produto. "Status" vem de `FormDetail.status`.

**F.5.3 — `handlePublish` sem catch**

```typescript
const handlePublish = async () => {
  setPublishing(true);
  try {
    await formsService.publish(productId, FORM_ID);
    toast.success("Alterações publicadas!");
    navigate("/forms/list");
  } finally {
    setPublishing(false); // erros silenciados — sem toast.error
  }
};
```

**Implementação necessária:**

**Rota:**
```typescript
// routes/index.tsx — adicionar :formId:
<Route path="/forms/:formId/publication" element={<PublicationPanel />} />
```

Atualizar todas as navegações para `/forms/publication` para incluir o `formId` real (ex.: link na aba "Publicação" em `FormBuilder` e `FormDetail`).

**Componente:**
```tsx
// PublicationPanel.tsx — ler formId da rota e derivar dados do FormDetail:
const { formId } = useParams<{ formId: string }>();
const { product } = useCurrentProduct();
const productId = product?.id ?? "";

const { data: form } = useAsyncData(
  () => (productId && formId ? formsService.getForm(productId, formId) : Promise.resolve(null)),
  [productId, formId]
);

// Derivar canais de dados reais:
const publicationChannels = form
  ? [
      ["URL",     form.publication ?? "—"],
      ["Embed",   `<aegis-form id="${formId}" />`],
      ["Script",  '<script src="/aegis/forms.js"></script>'],
      ["Iframe",  `/forms/${formId}`],
      ["Domínio", product?.domain ?? product?.name?.toLowerCase() ?? "—"],
      ["Status",  form.status],
    ]
  : [];

// Embed snippet para clipboard:
const embedSnippet = `<aegis-form id="${formId}" />`;

// handlePublish com catch:
const handlePublish = async () => {
  if (!formId) return;
  setPublishing(true);
  try {
    await formsService.publish(productId, formId);
    toast.success("Alterações publicadas!");
    navigate("/forms/list");
  } catch {
    toast.error("Falha ao publicar. Tente novamente.");
  } finally {
    setPublishing(false);
  }
};
```

> **Nota:** o campo `product.domain` pode não existir no contrato atual do frontend. Se ausente, derivar de `product.name` com slugify como fallback até que o campo seja adicionado à `ProductSummary`.

**Critério de aceite:**
- [ ] Rota alterada para `/forms/:formId/publication`; `useParams` extrai `formId`.
- [ ] "Canais de publicação" usa `FormDetail.publication` para a URL e `formId` real para Embed/Iframe.
- [ ] Status exibe valor real de `FormDetail.status` — nunca hardcoded "Publicado e rastreável".
- [ ] `handlePublish` e `handleSaveDelivery` têm `catch` com `toast.error`.
- [ ] `handleCopyEmbed` copia snippet com `formId` real.
- [ ] Navegação de `/forms/list` → aba "Publicação" passa `formId` na URL.

**Smoke test:**
```
1. Criar formulário → abrir aba Publicação
2. Verificar: URL = FormDetail.publication (ou "—" se não publicado)
3. Verificar: Embed = <aegis-form id="{id-real}" />
4. Clicar "Copiar embed" → clipboard contém o snippet com id real
5. Publicar → toast de sucesso → redirect para lista
6. Publicar com erro simulado → toast de erro visível
```

---

## Seção Z — Critérios de aceite globais da sprint

```bash
# Backend standalone:
cd backend && mvn verify -q
# → BUILD SUCCESS + jacoco-check PASSED (inclui ContentService.deleteContent() 100%)

# Bruno contrato:
cd bruno && bru run --env local
# → 0 FAILED (inclui DELETE /content/{id}: 204/400/403 + audit events FORM filter)

# TypeScript:
npm run typecheck
# → 0 erros (inclui FormsTimeline, PublicationPanel com useParams)
```

### Regras de ouro desta sprint

1. **Ações de ciclo de vida acessíveis sem sair da lista** — arquivar não deve exigir navegar para o editor.
2. **`from` nas transições sempre dinâmico** — nunca hardcoded com status presumido.
3. **Todo handler de ação tem catch com toast.error** — sem silêncio em erros.
4. **Exclusão apenas de Draft nunca publicado, apenas roles admin** — invariante documentada no contrato, sem exceções.
5. **Nenhum dado exibido ao usuário pode ser literal hardcoded** — se não há endpoint disponível, exibir "—" ou estado vazio, nunca valor inventado.
6. **Toda rota que exibe um recurso específico leva o ID na URL** — sem constantes globais substituindo `useParams`.
