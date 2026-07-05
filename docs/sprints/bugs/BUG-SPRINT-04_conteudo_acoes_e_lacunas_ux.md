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

**Decisão de design necessária:** exclusão de conteúdo deve ser suportada? Conteúdo é uma entidade versionada e auditada — o padrão do sistema é usar `ARCHIVED` como estado final. Se exclusão for permitida, deve ser restrita a `SUPER_ADMIN` e `TENANT_ADMIN`, e apenas para conteúdo em `Draft` (nunca publicado). Documentar ADR se necessário.

**Implementação necessária:**

**F.1.1 — Adicionar ação "Arquivar" na lista com status dinâmico:**

```tsx
// ContentDataGrid.tsx — adicionar botão condicional por status:
{r.status === "Published" && (
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
      from: currentStatus,  // dinâmico, não hardcoded "Published"
      to: "Archived",
    });
}
```

**F.1.2 — Excluir: implementar endpoint no backend (se aprovado em decisão de design):**

```java
// ContentController.java
@DeleteMapping("/api/v1/products/{productId}/content/{contentId}")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void deleteContent(@PathVariable UUID productId,
                          @PathVariable UUID contentId,
                          Authentication authentication) {
  AuthenticatedUser caller = assertProductAccess(authentication, productId);
  contentService.deleteContent(productId, contentId, caller);
}
```

Regras do `ContentService.deleteContent()`:
- Apenas `SUPER_ADMIN` e `TENANT_ADMIN` podem excluir.
- Apenas conteúdo em `DRAFT` (nunca publicado) pode ser excluído.
- Conteúdo `PUBLISHED` ou `ARCHIVED` só pode ser arquivado — nunca excluído.
- Auditoria obrigatória: registrar evento de deleção.

Também adicionar `@DeleteMapping` ao **Bruno** com assertion de status 204 e verificação de não-retorno em `GET`.

**Critério de aceite:**
- [ ] Lista exibe botão "Arquivar" para conteúdos com status `Published`.
- [ ] Arquivar a partir da lista executa transição corretamente e remove o item da visualização atual.
- [ ] `archive()` usa `currentStatus` como `from`, não hardcoded `"Published"`.
- [ ] Decisão documentada sobre suporte a exclusão (ADR ou nota no contrato).
- [ ] Se exclusão aprovada: `DELETE /content/{contentId}` retorna 204; apenas Draft excluível; apenas roles admin.
- [ ] Bruno: `POST .../transition` com `from` errado → 422 com erro descritivo.

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

## Seção Z — Critérios de aceite globais da sprint

```bash
# Backend standalone:
cd backend && mvn verify -q
# → BUILD SUCCESS + jacoco-check PASSED

# Bruno contrato:
cd bruno && bru run --env local
# → 0 FAILED (inclui DELETE /content/{id} se implementado)

# TypeScript:
npm run typecheck
# → 0 erros
```

### Regras de ouro desta sprint

1. **Ações de ciclo de vida acessíveis sem sair da lista** — arquivar não deve exigir navegar para o editor.
2. **`from` nas transições sempre dinâmico** — nunca hardcoded com status presumido.
3. **Todo handler de ação tem catch com toast.error** — sem silêncio em erros.
4. **Exclusão de conteúdo exige ADR** — decisão documentada antes de implementar.
