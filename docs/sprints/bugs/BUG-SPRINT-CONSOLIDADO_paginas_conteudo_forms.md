# Bug Sprint Consolidado — Páginas, Conteúdo e Forms

> **Contexto:** Consolidação dos BUG-SPRINT-03 e BUG-SPRINT-04 para execução em rodada única de implementação.
> Cobre bugs identificados em uso real do sistema: contratos frontend↔backend quebrados, erros silenciados sem feedback ao usuário, dados hardcoded onde deveria haver chamadas à API, e endpoints backend faltantes.
>
> **Constraint crítico:** `BUG-SPRINT-03_contrato_api_e2e_isolamento.md` e `BUG-SPRINT-04_conteudo_acoes_e_lacunas_ux.md` são os documentos de referência canônicos — este arquivo é a versão de execução consolidada. Não substituir os originais.
>
> **Branch:** `bugfix/sprint-03-04-paginas-conteudo-forms`
>
> **Ordem de execução recomendada:**
> 1. Error handling silencioso (E.6.2, E.9.1, F.3) — quick wins sem backend
> 2. Contratos de dados quebrados (E.6.1, E.6.3, E.9.2, E.10, E.11)
> 3. Backend endpoints faltantes (F.1.2)
> 4. Persistência real (E.7.1, E.5)
> 5. UX e dados estáticos (E.4, E.8, E.10.3, F.1.1, F.2, F.4, F.5)

---

## Diagnóstico de ambiente

```bash
# Backend UP:
curl -s http://localhost:8080/actuator/health | jq '.status'
# → "UP"

# TypeScript sem erros:
cd frontend && npm run typecheck
# → 0 erros

# Backend compila e testes passam:
cd backend && mvn verify -q
# → BUILD SUCCESS + jacoco-check PASSED

# Bruno (todos os contratos):
cd bruno && bru run --env local
# → 0 FAILED

# Frontend mock mode (backend parado):
IS_API_MODE=false npm run dev
# → login + navegação + dados mock — zero erros de console
```

---

## E.4 — Jornada Páginas × Conteúdo: separação correta, UX ausente e dashboard hardcoded

**Módulos afetados:** `frontend/src/domains/content/pages/EditorialDashboard.tsx`, `contentService.ts`

**Comportamento observado:** Páginas criadas em `Páginas` não aparecem em `Conteúdo` e vice-versa. Workflow vazio. Alertas no dashboard referenciam "Maestro Beton" para qualquer produto.

**Diagnóstico:** A separação é **intencional por design** — `Páginas` gerencia estrutura institucional (`draft|review|published|archived`), `Conteúdo` gerencia artigos editoriais com workflow completo (`DRAFT→IN_REVIEW→PUBLISHED→ARCHIVED`). Os bugs são de UX e dados hardcoded.

**E.4.1 — `EditorialDashboard`: card "Estados globais" hardcoded**
```tsx
// ERRADO — sempre placeholder:
<EmptyState compact title="Sem conteúdo" ... />
```
Fix: derivar de `rows` com contagem por `ContentStatus`:
```tsx
const byStatus = rows.reduce((acc, r) => {
  acc[r.status] = (acc[r.status] ?? 0) + 1; return acc;
}, {} as Record<string, number>);
// Renderizar: Draft, In Review, Published, Archived com counts reais
```

**E.4.2 — `EditorialAttentionCard`: alertas hardcoded com "Maestro Beton"**
Remover `EditorialAttentionCard` ou criar endpoint `GET /products/{productId}/content/attention-items`.

**E.4.3 — Workflow vazio** — resolvido junto com E.3 (seed popula `Content`).

**E.4.4 — UX diferenciadora ausente** — tooltip/banner na primeira visita explicando a distinção entre Páginas e Conteúdo.

**Critério de aceite:**
- [ ] Card de estados globais exibe contagens reais por status de Content.
- [ ] Zero referência a "Maestro Beton" em alertas visíveis ao usuário.
- [ ] Workflow exibe itens reais após E.3.

---

## E.5 — Formulários: `FormBuilder` não persiste form criado + filtro mock com slug errado

**Módulos afetados:** `frontend/src/domains/forms/pages/FormBuilder.tsx`, `formsService.ts`

**Comportamento observado:** Usuário cria formulário mas ele não aparece na lista — parece que não foi salvo.

**E.5.1 — `FormBuilder` nunca chama `createForm()`**
```tsx
// FormBuilder.tsx — rota /forms/new:
const { slug: formId } = useParams<{ slug: string }>(); // undefined
const handleSaveDraft = async () => {
  if (formId && productId) await formsService.saveFormFields(...); // PULADO — formId undefined
};
```
`formsService.createForm()` NUNCA é chamado. O formulário não é criado no backend.

Fix: ao clicar "Novo formulário", criar o form primeiro e redirecionar:
```tsx
// FormsList / FormsDashboard — ao criar:
const created = await formsService.createForm(productId, { name: "Novo formulário", type: "contato" });
navigate(`/forms/${created.id}/builder`);
```
Adicionar rota:
```tsx
<Route path="/forms/:slug/builder" element={<FormBuilder />} />
```

**E.5.2 — Filtro mock retorna vazio**
```typescript
// formsService.ts:
return formsStore.filter((f) => f.productSlug === currentProductSlugOrId());
// currentProductSlugOrId() retorna "p1" mas mock tem productSlug: "maestro-beton" → []
```
Fix:
```typescript
return formsStore.filter((f) =>
  f.productSlug === productId || f.productSlug === currentProductSlugOrId()
);
```

**Critério de aceite:**
- [ ] Criar formulário → aparece na lista com status "Rascunho".
- [ ] Em mock mode, formulários do produto ativo listam corretamente.
- [ ] Rota `/forms/:slug/builder` funciona para editar formulário existente.

---

## E.6 — Eventos: salvar falha silenciosamente

**Módulos afetados:** `frontend/src/domains/pages/components/EventsManagerDrawer.tsx`, `eventsService.ts`, `backend/.../pages/contract/CreateEventRequest.java`

**Comportamento observado:** Preencher formulário de evento e clicar "Salvar evento" — nada acontece. Evento não aparece na lista. Imagem selecionada não é salva.

**E.6.1 — `@NotBlank location` exigido pelo backend, não validado no frontend**
```java
// CreateEventRequest.java:
@NotBlank String location;  // obrigatório no backend
```
Frontend habilita "Salvar" apenas com título preenchido → backend retorna 400 silenciado.

Fix — validar `location` no frontend:
```tsx
<Button primary onClick={() => onSave(draft)}
  disabled={saving || !draft.title.trim() || !draft.location.trim()} />
```

**E.6.2 — `handleSave` sem `catch` → erro 400 silenciado**
```typescript
// EventsManagerDrawer.tsx:
try {
  await eventsService.createEvent(...); // lança 400
  toast.success(...); // nunca executado
} finally {
  setSaving(false); // erro silenciado — sem toast.error
}
```
Fix:
```typescript
try {
  await eventsService.createEvent(productSlug, req);
  toast.success("Evento criado", { description: req.title });
  setEditing(null); refresh();
} catch (err: unknown) {
  toast.error("Falha ao salvar", {
    description: (err as { message?: string }).message ?? "Erro ao salvar evento."
  });
} finally {
  setSaving(false);
}
```

**E.6.3 — `imageAssetId`: frontend envia filename (string), backend espera UUID**
```typescript
// eventsService.ts:
imageAssetId: req.image || undefined  // "foto-evento.jpg" — não é UUID!
```
Fix — capturar `asset.id` separadamente via `onSelectAsset`:
```tsx
<MediaField
  label="Foto do evento"
  value={draft.image ?? ""}
  typeFilter="imagem"
  onChange={(name) => patch({ image: name })}
  onSelectAsset={(asset) => patch({ imageAssetId: asset.id, image: asset.name })}
/>
```

**Critério de aceite:**
- [ ] Evento com "Local" vazio → mensagem de validação, não envio silencioso.
- [ ] Erro do backend → toast de erro descritivo.
- [ ] `imageAssetId` enviado como UUID válido do asset.
- [ ] Evento criado aparece na lista após salvar.

---

## E.7 — event-list: evento selecionado não aparece no preview

**Módulos afetados:** `frontend/src/domains/pages/pages/PageEditor.tsx`, `frontend/src/domains/pages/components/BlockRenderer.tsx`

**Comportamento observado:** Usuário seleciona evento, clica "Salvar rascunho", abre Preview — preview exibe "Nenhum evento selecionado ainda".

**E.7.1 — "Salvar rascunho" é cosmético: `triggerSave()` nunca chama backend**
```typescript
// PageEditor.tsx — 100% visual, zero persistência:
const triggerSave = () => {
  setSaveStatus("dirty");
  setTimeout(() => { setSaveStatus("saving"); setTimeout(() => setSaveStatus("saved"), 1100); }, 1800);
};
```
O debounce (`handleChangeContent`) persiste patches de blocos — mas clicar "Salvar rascunho" não flusheia o debounce. Se o timer ainda não disparou, `selectedEventIds` não está no backend quando o Preview abre.

Fix — `handleSaveDraft` real:
```typescript
const handleSaveDraft = async () => {
  if (!page) return;
  setSaveStatus("saving");
  try {
    // 1. Flush do debounce pendente
    if (contentDebounceTimer.current) {
      clearTimeout(contentDebounceTimer.current);
      const pending = pendingContentPatch.current;
      pendingContentPatch.current = null;
      if (pending) {
        await pagesService.updateSection(page.productSlug, page.id, pending.sectionId, {
          content: { ...selectedSection?.content, ...pending.patch },
        });
      }
    }
    // 2. Persistir via rota real
    await pagesService.updatePage(page.productSlug, page.id, { status: "draft" });
    await refreshPage(page);
    setSaveStatus("saved");
    toast.success("Rascunho salvo");
    setTimeout(() => setSaveStatus("idle"), 3000);
  } catch (err: unknown) {
    setSaveStatus("idle");
    toast.error("Falha ao salvar", {
      description: (err as { message?: string }).message ?? "Tente novamente."
    });
  }
};
```
Botão no JSX:
```tsx
<Button onClick={handleSaveDraft} disabled={saveStatus === "saving"}>Salvar rascunho</Button>
```

**E.7.2 — `BlockRenderer` para `event-list` é placeholder**
```tsx
// Atual — apenas conta:
return <p>{selectedCount} evento(s) selecionado(s)</p>
```
Fix recomendado: desnormalizar dados do evento no bloco ao selecionar (`{ id, title, date, location, type }` em vez de só IDs), permitindo renderização no `BlockRenderer` sem fetch adicional.

**Critério de aceite:**
- [ ] "Salvar rascunho" flusheia debounce + persiste via `pagesService.updatePage`.
- [ ] Preview após salvar exibe eventos selecionados com título, data e local.
- [ ] Erro no save → toast de erro (nunca silêncio).

---

## E.8 — Troca de produto sem feedback visual

**Módulos afetados:** `frontend/src/core/auth/AuthContext.tsx`, `frontend/src/app/layouts/AppShell.tsx`

**Comportamento observado:** Troca de produto via Switcher ou `ProductCard` atualiza a interface instantaneamente sem nenhum indicador visual de mudança de contexto.

**Diagnóstico:** `switchProduct` é síncrono sem estado de loading.

**Implementação:**
```typescript
// AuthContext.tsx:
const [productSwitching, setProductSwitching] = useState(false);
const switchProduct = useCallback((productId: string) => {
  const p = (userProducts[effectiveTenant?.id ?? ""] || []).find((p) => p.id === productId);
  if (!p) return;
  setProductSwitching(true);
  setSelectedProduct(p);
  setTimeout(() => setProductSwitching(false), 1000);
}, [effectiveTenant, userProducts]);
```

```tsx
// Novo componente ProductSwitchingOverlay:
export function ProductSwitchingOverlay({ product }: { product: Product | null }) {
  return (
    <div className="fixed inset-0 z-50 flex flex-col items-center justify-center gap-4 bg-background">
      <img src="/aegis-logo.svg" alt="Aegis PMS" className="h-16 w-16 animate-pulse" />
      <p className="text-sm font-medium text-muted-foreground">Carregando {product?.name ?? "produto"}…</p>
      <div className="mt-4 w-full max-w-xl space-y-3 px-8"><SkeletonLines /></div>
    </div>
  );
}

// AppShell.tsx:
{productSwitching && <ProductSwitchingOverlay product={effectiveProduct} />}
```

**Critério de aceite:**
- [ ] Troca de produto via Switcher ou `ProductCard` exibe overlay com logo Aegis por ~1s.
- [ ] Overlay exibe nome do produto sendo carregado.
- [ ] Nenhum flash de dados do produto anterior.

---

## E.9 — Inserção de blocos falha silenciosamente (DEFAULT_BLOCK_CONTENT vs backend)

**Módulos afetados:** `frontend/src/domains/pages/pages/PageEditor.tsx`, `blockDefaults.ts`, `backend/.../pages/service/SectionContentValidationService.java`

**Comportamento observado:** Selecionar tipo de bloco e clicar "Adicionar bloco" — interface não responde, sem erro visível.

**E.9.1 — `handleAddBlock` sem `catch`**
```typescript
// ERRO: sem try/catch:
const created = await pagesService.createSection(...); // lança 400 → silenciado
```
Fix:
```typescript
const handleAddBlock = async (type: BlockType) => {
  if (!page) return;
  try {
    const created = await pagesService.createSection(page.productSlug, page.id, {
      type, label: `Novo bloco ${page.sections.length + 1}`,
      content: DEFAULT_BLOCK_CONTENT[type],
    });
    await refreshPage(page);
    setSelectedSectionId(created.id);
    toast.success("Bloco adicionado", { description: type });
  } catch (err: unknown) {
    toast.error(`Não foi possível adicionar bloco "${type}"`, {
      description: (err as { message?: string }).message ?? "Verifique os campos obrigatórios."
    });
  }
};
```

**E.9.2 — `DEFAULT_BLOCK_CONTENT` inválido para 7 tipos**

| Tipo | Erro backend | Fix |
|---|---|---|
| `image` | `IMAGE_ALT_REQUIRED` (alt deve ser raiz, não dentro de `image.{}`) | `{ alt: "Descrição da imagem", src: "" }` |
| `event-list` | `EVENT_LIST_SOURCE_REQUIRED` | `{ title: "Agenda", source: { filter: null }, selectedEventIds: [] }` |
| `video-gallery` | Exige ≥1 item | `{ items: [{ title: "Vídeo 1", source: "youtube", youtubeUrl: "https://www.youtube.com/watch?v=dQw4w9WgXcQ" }] }` |
| `contact` / `form` | `formId` não é UUID | **Opção A:** relaxar validação backend na criação |
| `download` / `audio` / `video` | asset UUID vazio | **Opção A:** relaxar validação backend na criação |

**Opção A (recomendada) — relaxar validação do backend na criação:**
```java
// SectionController: POST /sections → aceita content parcial (salva como draft)
// PUT /sections/{id} e transição de status → valida estritamente
```

**Critério de aceite:**
- [ ] Todos os 19 tipos de bloco podem ser adicionados sem erro silencioso.
- [ ] Falha → toast com mensagem descritiva.
- [ ] `image`, `event-list`, `video-gallery` corrigidos no `blockDefaults.ts`.
- [ ] Blocos com asset/form: ou aceitos sem asset na criação, ou wizard antes de criar.

---

## E.10 — Hero block: lacunas de editor (imagem, variantes, botões extras)

**Módulos afetados:** `frontend/src/domains/pages/components/EditorPanels.tsx`, `blockDefaults.ts`, `backend/.../pages/domain/BlockDefaults.java`

**Comportamento observado:** Editor do bloco `hero` não exibe campo de imagem. Não há seletor de variante. CTAs aparecem como campos planos sem suporte a múltiplos botões.

**E.10.1 — Picker de imagem não aparece quando bloco foi criado sem campo `image`**

`BlockDefaults.java` inicializa hero apenas com `{ title: "Novo título" }`. Se `image` não existe no content, `ImageFieldEditor` nunca renderiza.

Fix — injetar `image` quando ausente:
```tsx
const fieldableContent = (() => {
  const base = Object.fromEntries(Object.entries(content).filter(([k]) => !excludedKeys.has(k)));
  if (section.type === "hero" && !base.image) return { ...base, image: { src: "", alt: "" } };
  return base;
})();
```

Alinhar `BlockDefaults.java`:
```java
defaults.put(BlockType.HERO, Map.of(
    KEY_TITLE, "Novo título",
    "subtitle", "",
    "image", Map.of("src", "", "alt", ""),
    "ctas", List.of(Map.of("label", "Saiba mais", "href", "#"))
));
```

**E.10.2 — Shape de CTAs inconsistente: canonicalizar para array**

Shape canônico:
```typescript
hero: {
  title: "Novo título", subtitle: "", theme: "dark",
  image: { src: "", alt: "" },
  ctas: [{ label: "Saiba mais", href: "#" }],  // 1-4 botões
}
```
`BlockRenderer` usar `asArray(c.ctas).map(...)` em vez de `c.ctaPrimary`/`c.ctaSecondary`.

**E.10.3 — Campo `variant` existe no backend mas nunca exposto no editor**

`PageSection.variant`, `CreateSectionRequest.variant` e `UpdateSectionRequest.variant` existem mas o frontend ignora. Fix:
```tsx
{section.type === "hero" && (
  <SelectLike label="Variante" value={section.variant ?? "primary"}
    options={["primary", "secondary"]}
    onChange={(v) => onChangeVariant(v)} />
)}
```
`secondary`: sem campo `image`, layout compacto, `theme` padrão `"light"`.

**Critério de aceite:**
- [ ] Hero sempre exibe picker de imagem (campo injetado quando ausente).
- [ ] CTAs usam shape `ctas: Array<{ label, href }>` — 1 a 4 botões.
- [ ] `BlockRenderer` renderiza CTAs a partir de `ctas[]`.
- [ ] Variante "secondary" disponível e persiste via `UpdateSectionRequest`.

---

## E.11 — Imagens nunca renderizadas no preview (todos os blocos)

**Módulos afetados:** `frontend/src/shared/components/MediaField.tsx`, `frontend/src/domains/pages/components/BlockRenderer.tsx`

**Comportamento observado:** Imagens selecionadas via picker não aparecem no preview — `BlockRenderer` exibe `[imagem: alt text]` para todos os blocos: `hero`, `image`, `image-text`, `gallery`.

**E.11.1 — `MediaField.onChange` retorna filename, não UUID**
```typescript
// MediaField.tsx linha 50 — ERRADO:
onChange(asset.name);  // "hero-maestro-beton.jpg" — não é resolvível pelo browser
```
Fix — retornar UUID:
```typescript
onSelect={(asset) => {
  onChange(asset.id);         // ← UUID, não filename
  onSelectAsset?.(asset);
}}
```
Atualizar assinatura: `onChange: (assetId: string) => void` (semântica muda de name para id).

**E.11.2 — `BlockRenderer` usa placeholders de texto, nunca `<img>`**
```tsx
// ATUAL para todos os blocos de imagem:
case "image": return <div>[imagem: {asStr(c.alt)}]</div>;
```

Fix — criar componente `<AssetImage>` que resolve UUID para URL:
```typescript
function resolveAssetSrc(assetId: string | undefined): string | undefined {
  if (!assetId) return undefined;
  if (IS_API_MODE) return `${resolveBaseUrl()}/api/v1/assets/${assetId}/download`;
  // mock mode: placehold.co com nome do asset
  const asset = assetsStore.find((a) => a.id === assetId);
  return asset ? `https://placehold.co/800x400?text=${encodeURIComponent(asset.name)}` : undefined;
}
```

```tsx
// BlockRenderer — bloco "image":
case "image": {
  const src = resolveAssetSrc(typeof c.imageAssetId === "string" ? c.imageAssetId : undefined);
  return src
    ? <img src={src} alt={asStr(c.alt)} className="w-full rounded-lg object-cover" />
    : <div className="rounded-lg bg-muted p-6 text-center text-xs text-muted-foreground">[imagem: {asStr(c.alt, "sem descrição")}]</div>;
}
```
Aplicar o mesmo padrão para `hero` (campo `image.imageAssetId`), `image-text` e cada item de `gallery`.

**Nota de migração:** content existente com `image.src = "filename.jpg"` mostrará placeholder até ser reedita. Aceitável.

**Critério de aceite:**
- [ ] Selecionar imagem via picker → `imageAssetId` armazena UUID do asset.
- [ ] `BlockRenderer` renderiza `<img>` real para `image`, `image-text`, `hero`, `gallery`.
- [ ] Em API mode: imagem carregada via `GET /api/v1/assets/{id}/download`.
- [ ] Em mock mode: imagem exibe placehold.co com nome do arquivo.
- [ ] Blocos sem `imageAssetId` → placeholder texto (retrocompatibilidade).

---

## F.1 — Lista de conteúdos: ações de ciclo de vida ausentes (Arquivar / Excluir)

**Módulos afetados:** `frontend/src/domains/content/pages/ContentDataGrid.tsx`, `contentService.ts`, `backend/.../content/controller/ContentController.java`

**Comportamento observado:** Lista de conteúdos tem apenas `Abrir`, `Preview`, `Histórico` por linha — sem Arquivar nem Excluir.

**F.1.1 — Arquivar: ação existe, `from` hardcoded como `"Published"`**
```typescript
// contentService.ts — ERRADO:
{ from: "Published", to: "Archived" }  // falha se status atual for "In Review"
```
Fix:
```typescript
async archive(id: string, currentStatus: string, productId = requireCurrentProductId()): Promise<void> {
  if (IS_API_MODE)
    return apiClient.post(`/products/${productId}/content/${id}/transition`, {
      from: currentStatus, to: "Archived",
    });
  const item = contentsStore.find((c) => c.id === id);
  if (item) item.status = "Archived";
}
```

Botão na lista:
```tsx
{(r.status === "Published" || r.status === "In Review") && (
  <PermGate allowed={canEdit}>
    <Button onClick={() => handleArchive(r.id, r.status)}>Arquivar</Button>
  </PermGate>
)}
```

**F.1.2 — Excluir: endpoint backend inexistente**

`contentService.deleteContent()` chama `DELETE /products/{productId}/content/{id}` — endpoint não existe no `ContentController`.

**Decisão aprovada:** apenas `Draft` que nunca foi publicado (`currentVersion == 1`), apenas `SUPER_ADMIN` e `TENANT_ADMIN`.

Backend:
```java
// ContentController.java:
@DeleteMapping("/api/v1/products/{productId}/content/{contentId}")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void deleteContent(@PathVariable UUID productId, @PathVariable UUID contentId,
                          Authentication authentication) {
    AuthenticatedUser caller = assertProductAccess(authentication, productId);
    contentService.deleteContent(productId, contentId, caller);
}

// ContentService.java:
private static final String ROLE_SUPER_ADMIN  = "ROLE_SUPER_ADMIN";
private static final String ROLE_TENANT_ADMIN = "ROLE_TENANT_ADMIN";

public void deleteContent(UUID productId, UUID contentId, AuthenticatedUser caller) {
    boolean isAdmin = caller.authorities().contains(ROLE_SUPER_ADMIN)
                   || caller.authorities().contains(ROLE_TENANT_ADMIN);
    if (!isAdmin) throw new InsufficientContentRoleException(
        "Apenas SUPER_ADMIN ou TENANT_ADMIN podem excluir conteúdo.");

    Content content = contentRepository.findByIdAndProductId(contentId, productId)
        .orElseThrow(() -> new ContentNotFoundException(contentId));

    boolean neverPublished = content.getStatus() == ContentStatus.DRAFT
                          && content.getCurrentVersion() == 1;
    if (!neverPublished) throw new InvalidContentTransitionException(
        "Apenas Draft nunca publicado pode ser excluído. Use transição para Archived.");

    auditService.record(productId, caller.subject(), "CONTENT_DELETED", contentId.toString());
    versionRepository.deleteAllByContentId(contentId);
    contentRepository.delete(content);
}
```

Frontend:
```typescript
// contentService.ts — mock mode:
async deleteContent(id: string): Promise<void> {
  if (IS_API_MODE) return apiClient.delete(`/products/${requireCurrentProductId()}/content/${id}`);
  const idx = contentsStore.findIndex((c) => c.id === id);
  if (idx !== -1) contentsStore.splice(idx, 1);
}
```
```tsx
// ContentDataGrid.tsx:
{r.status === "Draft" && (
  <PermGate allowed={viewAsRole === "super_admin" || viewAsRole === "tenant_admin"}>
    <Button variant="destructive" onClick={() => {
      if (confirm("Excluir permanentemente este rascunho?"))
        contentService.deleteContent(r.id).then(() => refresh());
    }}>Excluir</Button>
  </PermGate>
)}
```

Bruno:
- `DELETE /products/{productId}/content/{contentId}` Draft → `204`
- com Published → `400`
- com role editor → `403`
- `GET` após delete → `404`

**JaCoCo:** 100% em `ContentService.deleteContent()`.

**Critério de aceite:**
- [ ] Lista exibe "Arquivar" para `Published` e `In Review`.
- [ ] Lista exibe "Excluir" apenas para `Draft` + roles admin.
- [ ] `archive()` usa `currentStatus` como `from` — nunca hardcoded.
- [ ] `DELETE` → 204/400/403 conforme regras.
- [ ] Deleção registra evento de auditoria.

---

## F.2 — `PublishPanel` (Conteúdo): checklist de pré-publicação hardcoded

**Módulos afetados:** `frontend/src/domains/content/pages/PublishPanel.tsx`

```tsx
// ERRADO — hardcoded:
{([["SEO completo", false], ["Formulário vinculado", true], ...]).map(...)}
```
"Formulário vinculado" sempre ✓, mesmo sem formulário.

Fix — derivar de dados reais:
```typescript
const checks = [
  { label: "SEO completo", ok: !!(content?.metadataJson && JSON.parse(content.metadataJson).title) },
  { label: "Traduções completas", ok: (content?.translations?.length ?? 0) > 0 },
  { label: "Formulário vinculado", ok: !!content?.formId },
];
```

**Critério de aceite:**
- [ ] Checklist reflete estado real do conteúdo.
- [ ] "Formulário vinculado" ✓ apenas se `content.formId` presente.

---

## F.3 — `PublishPanel` (Conteúdo): todos os handlers sem catch

**Módulos afetados:** `frontend/src/domains/content/pages/PublishPanel.tsx`

5 handlers (`handleArchive`, `handlePublish`, `handleSaveDraft`, `handleSchedule`, `handleSubmitForReview`) usam `try/finally` sem `catch`:
```typescript
try {
  await contentService.archive(...); // pode lançar 400/422
  toast.success(...);
} finally {
  setLoading(false); // erro silenciado
}
```

Fix — adicionar `catch` em todos os 5:
```typescript
} catch (err: unknown) {
  toast.error("Falha na operação", {
    description: (err as { message?: string }).message ?? "Tente novamente."
  });
}
```

**Critério de aceite:**
- [ ] Falha em qualquer ação → toast de erro descritivo.

---

## F.4 — `FormsTimeline`: dados completamente hardcoded

**Módulos afetados:** `frontend/src/domains/forms/components/FormsTimeline.tsx`, `formsService.ts`

```tsx
// ATUAL — array literal hardcoded:
{["Novo orçamento recebido", "Novo contato enviado", "RSVP confirmado", "Formulário publicado"]
  .map((t, i) => <div>há {i + 1} h · ...</div>)}
```

**Fonte real disponível:**
```
GET /api/v1/tenants/{tenantId}/audit-events?module=FORM&productId={productId}
```
Retorna `AuditEventSummary[]` com `{ id, actor, action, target, module, time, risk }`.

Fix:
```typescript
// formsService.ts:
async listActivity(tenantId: string, productId: string): Promise<AuditEventSummary[]> {
  if (IS_API_MODE)
    return apiClient.get(`/tenants/${tenantId}/audit-events?module=FORM&productId=${productId}`);
  return []; // mock mode: vazio — sem dados fabricados
}
```
```tsx
// FormsTimeline.tsx:
const { data: events = [] } = useAsyncData(
  () => effectiveTenant?.id && effectiveProduct?.id
    ? formsService.listActivity(effectiveTenant.id, effectiveProduct.id)
    : Promise.resolve([]),
  [effectiveTenant?.id, effectiveProduct?.id]
);
if (events.length === 0)
  return <p className="text-sm text-muted-foreground">Nenhuma atividade recente.</p>;
```

**Critério de aceite:**
- [ ] Timeline carrega eventos reais via audit API com `module=FORM`.
- [ ] Estado vazio exibe "Nenhuma atividade recente." — nunca dados inventados.
- [ ] `SubmissionDetails` reutiliza o mesmo componente corrigido.

---

## F.5 — `PublicationPanel` (Forms): tela completamente estática

**Módulos afetados:** `frontend/src/domains/forms/pages/PublicationPanel.tsx`, `app/routes/index.tsx`

**E.F5.1 — `FORM_ID` hardcoded + rota sem parâmetro**
```typescript
const FORM_ID = "form-contato-comercial";  // global hardcoded
// routes/index.tsx:
<Route path="/forms/publication" element={<PublicationPanel />} />  // sem :formId
```
`getDelivery`, `publish` e `handleCopyEmbed` usam sempre o mesmo form fictício.

Fix — rota com parâmetro:
```typescript
<Route path="/forms/:formId/publication" element={<PublicationPanel />} />
```
```tsx
const { formId } = useParams<{ formId: string }>();
const { data: form } = useAsyncData(
  () => productId && formId ? formsService.getForm(productId, formId) : Promise.resolve(null),
  [productId, formId]
);
```
Atualizar links para `/forms/publication` para incluir `formId`.

**F.5.2 — Canais de publicação hardcoded**
```tsx
// ERRADO — array literal estático:
{[["URL", "https://maestrobeton.com/forms/contato"], ["Status", "Publicado e rastreável"], ...].map(...)}
```
Fix — derivar de `FormDetail` (backend já retorna `publication`, `status`, `id`):
```tsx
const publicationChannels = form ? [
  ["URL",     form.publication ?? "—"],
  ["Embed",   `<aegis-form id="${formId}" />`],
  ["Script",  '<script src="/aegis/forms.js"></script>'],
  ["Iframe",  `/forms/${formId}`],
  ["Status",  form.status],
] : [];
const embedSnippet = `<aegis-form id="${formId}" />`;
```

**F.5.3 — `handlePublish` sem catch**
```typescript
// Fix:
} catch {
  toast.error("Falha ao publicar. Tente novamente.");
}
```

**Critério de aceite:**
- [ ] Rota usa `:formId`; `useParams` extrai o ID real.
- [ ] Canais derivados de `FormDetail` — nunca hardcoded.
- [ ] Status exibe valor real de `FormDetail.status`.
- [ ] `handlePublish` e `handleSaveDelivery` têm `catch` com toast.
- [ ] Embed snippet usa `formId` real.

---

## Seção Z — Gates globais e regras de ouro

### Z.1 — Gates obrigatórios antes de marcar como concluído

```bash
# Backend:
cd backend && mvn verify -q
# → BUILD SUCCESS + jacoco-check PASSED
# → ContentService.deleteContent(): 100% cobertura JaCoCo

# Bruno contrato:
cd bruno && bru run --env local
# → 0 FAILED
# → Inclui: DELETE /content/{id} (204/400/403), audit-events?module=FORM

# TypeScript:
cd frontend && npm run typecheck
# → 0 erros

# Frontend mock mode:
IS_API_MODE=false npm run dev
# → login, navegação, dados mock visíveis — zero erros de console
```

### Z.2 — Regras de ouro desta rodada

1. **Todo handler assíncrono tem `catch` com `toast.error`** — sem silêncio jamais.
2. **`from` nas transições sempre dinâmico** — nunca hardcoded com status presumido.
3. **Exclusão apenas de Draft nunca publicado, apenas roles admin** — invariante absoluta.
4. **Toda rota que exibe recurso específico leva o ID na URL** — sem constantes globais substituindo `useParams`.
5. **Nenhum dado de negócio exibido ao usuário pode ser literal hardcoded** — se não há endpoint, exibir "—" ou vazio.
6. **`MediaField.onChange` retorna UUID do asset** — não filename; `BlockRenderer` resolve UUID para URL real.
7. **Todo endpoint novo = assertion Bruno** — status + shape, nunca só status.
