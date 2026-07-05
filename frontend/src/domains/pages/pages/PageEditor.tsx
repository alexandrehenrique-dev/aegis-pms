import { useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router";
import { AnimatePresence } from "motion/react";
import { DndProvider } from "react-dnd";
import { HTML5Backend } from "react-dnd-html5-backend";
import { Badge, Button, EmptyState, PageHeader } from "../../../shared/components/Primitives";
import { UnsavedChangesBanner } from "../../../shared/components/Banners";
import { ConfirmDialog } from "../../../shared/components/ConfirmDialog";
import { FloatingSaveStatus, type SaveStatus } from "../../../shared/components/FloatingSaveStatus";
import { toast } from "../../../core/notifications/toast";
import { ContentStructureTree, BlockEditorCanvas, PropertiesPanel } from "../components/EditorPanels";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { pagesService } from "../services/pagesService";
import { DEFAULT_BLOCK_CONTENT } from "../blockDefaults";
import type { BlockType, Page } from "../contracts/responses";

const CONTENT_SAVE_DEBOUNCE_MS = 500;

/**
 * Editor de blocos de uma `Page` institucional (domínio `pages`) — movido de
 * `domains/content/pages/ContentEditor.tsx` na Sprint 15, Tarefa A.1: era o
 * mesmo componente servindo `/content/{id}/editor` tanto para `Page` quanto
 * para `Content`, e quem editava um artigo/post acabava no motor de blocos
 * de página por engano. `domains/content` agora tem seu próprio editor
 * dedicado (`ContentEditor.tsx` + `ContentArticleEditor.tsx`).
 *
 * Quem chega a esta tela já passou pelo gate de rota (`RequireRole` +
 * `roleBlockedRoutePrefixes["/pages/*\/editor"]`, ver core/permissions/roles.ts)
 * — hoje só `viewer` é bloqueado. Editor, Product Manager, Tenant Admin e
 * Super Admin podem adicionar, editar e remover blocos; não há gating
 * adicional por ação dentro do editor porque nenhum documento (004, V1)
 * distingue essas ações dentro do mesmo papel "Editor de Conteúdo" (07.04).
 */
export function PageEditor() {
  const navigate = useNavigate();
  const { id: pageSlug } = useParams<{ id: string }>();
  const [saveStatus, setSaveStatus] = useState<SaveStatus>("idle");
  const saveTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const pendingContentPatch = useRef<{ sectionId: string; patch: Record<string, unknown> } | null>(null);
  const contentDebounceTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const { product } = useCurrentProduct();
  const productSlug = product ? product.id : "p1";

  const { data: foundPage, loading: loadingPage } = useAsyncData(
    () => (pageSlug ? pagesService.getPageBySlug(productSlug, pageSlug) : Promise.resolve(undefined)),
    [productSlug, pageSlug],
  );
  const [page, setPage] = useState<Page | null>(null);
  const [selectedSectionId, setSelectedSectionId] = useState<string | null>(null);
  const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null);
  const [deletingBlock, setDeletingBlock] = useState(false);

  useEffect(() => {
    setPage(foundPage ?? null);
    setSelectedSectionId(foundPage?.sections[0]?.id ?? null);
  }, [foundPage]);

  // Limpa qualquer escrita pendente do debounce ao trocar de página/desmontar
  // — evita persistir um patch de uma seção que já não está mais selecionada.
  useEffect(() => () => { if (contentDebounceTimer.current) clearTimeout(contentDebounceTimer.current); }, []);

  const selectedSection = page?.sections.find((s) => s.id === selectedSectionId) ?? null;
  const pendingDeleteSection = page?.sections.find((s) => s.id === pendingDeleteId) ?? null;

  const triggerSave = () => {
    setSaveStatus("dirty");
    if (saveTimer.current) clearTimeout(saveTimer.current);
    saveTimer.current = setTimeout(() => {
      setSaveStatus("saving");
      setTimeout(() => {
        setSaveStatus("saved");
        setTimeout(() => setSaveStatus("idle"), 3000);
      }, 1100);
    }, 1800);
  };

  // Sempre busca a página de volta no service após mutar, em vez de remontar
  // o array de seções manualmente: pagesService é a fonte da verdade e
  // devolve cópias isoladas (ver pagesService.ts), então isto nunca duplica
  // ou perde itens, independente de quantas mutações ocorrerem em sequência.
  const refreshPage = async (current: Page) => {
    const fresh = await pagesService.getPage(current.productSlug, current.id);
    setPage(fresh ?? null);
    return fresh ?? null;
  };

  /**
   * Debounçado (Sprint 15, Tarefa F) — antes, cada tecla digitada num campo de
   * bloco chamava `pagesService.updateSection` + `refreshPage` (duas
   * "chamadas de API" mockadas logadas via `devLog`) imediatamente. Agora o
   * patch mais recente fica pendente em `pendingContentPatch` e só é
   * persistido `CONTENT_SAVE_DEBOUNCE_MS` depois da última tecla.
   *
   * O `setPage` otimista abaixo (Sprint 18 — bug encontrado em teste manual)
   * é obrigatório, não cosmético: os campos de bloco são controlados por
   * `section.content`, que só refletia o patch depois do debounce + round-trip
   * assíncrono do mock. Cada tecla disparava `setSaveStatus("dirty")` — um
   * re-render síncrono — e o React reescreve `<input value>` a cada commit
   * independente de diff, então o caractere recém-digitado era sobrescrito de
   * volta ao valor antigo antes da tecla seguinte. Resultado: digitar mais
   * rápido que `CONTENT_SAVE_DEBOUNCE_MS` perdia quase todos os caracteres,
   * sobrevivendo só o último. Atualizar `page` localmente a cada tecla resolve
   * isso sem tocar no debounce, que continua só controlando a persistência.
   */
  const handleChangeContent = (patch: Record<string, unknown>) => {
    if (!page || !selectedSectionId) return;
    setSaveStatus("dirty");
    setPage((prev) => (prev ? { ...prev, sections: prev.sections.map((s) => (s.id === selectedSectionId ? { ...s, content: { ...s.content, ...patch } } : s)) } : prev));
    const mergedPatch = { ...pendingContentPatch.current?.patch, ...patch };
    pendingContentPatch.current = { sectionId: selectedSectionId, patch: mergedPatch };
    if (contentDebounceTimer.current) clearTimeout(contentDebounceTimer.current);
    contentDebounceTimer.current = setTimeout(async () => {
      const pending = pendingContentPatch.current;
      pendingContentPatch.current = null;
      if (!pending || !page) return;
      await pagesService.updateSection(page.productSlug, page.id, pending.sectionId, { content: { ...selectedSection?.content, ...pending.patch } });
      await refreshPage(page);
      triggerSave();
    }, CONTENT_SAVE_DEBOUNCE_MS);
  };

  const handleAddBlock = async (type: BlockType) => {
    if (!page) return;
    const label = `Novo bloco ${page.sections.length + 1}`;
    const created = await pagesService.createSection(page.productSlug, page.id, { type, label, content: DEFAULT_BLOCK_CONTENT[type] });
    await refreshPage(page);
    setSelectedSectionId(created.id);
    toast.success("Bloco adicionado", { description: `${label} (${type})` });
    triggerSave();
  };

  const handleReorderSections = async (sectionIds: string[]) => {
    if (!page) return;
    await pagesService.reorderSections(page.productSlug, page.id, { sectionIds });
    await refreshPage(page);
    triggerSave();
  };

  const handleDeleteBlock = async () => {
    if (!page || !pendingDeleteId) return;
    setDeletingBlock(true);
    try {
      await pagesService.deleteSection(page.productSlug, page.id, pendingDeleteId);
      const fresh = await refreshPage(page);
      if (selectedSectionId === pendingDeleteId) setSelectedSectionId(fresh?.sections[0]?.id ?? null);
      toast.success("Bloco removido", { description: pendingDeleteSection?.label });
      setPendingDeleteId(null);
      triggerSave();
    } finally {
      setDeletingBlock(false);
    }
  };

  const handleSubmitForReview = () => {
    setSaveStatus("idle");
    toast.success("Enviado para revisão.", { description: "Rafael Lima será notificado." });
    // Tarefa C.1 — antes ficava preso na tela de edição de um conteúdo que já
    // não está mais em edição; agora volta para o Kanban onde o item aparece
    // na coluna "Em revisão".
    navigate("/content/workflow");
  };

  if (!loadingPage && !page) {
    return (
      <EmptyState
        title="Página não encontrada"
        description={`Nenhuma página com slug "${pageSlug}" existe em ${productSlug}. Volte para a lista de páginas e escolha uma existente, ou crie uma nova.`}
      />
    );
  }

  return (
    <DndProvider backend={HTML5Backend}>
      <AnimatePresence>{saveStatus !== "idle" && <FloatingSaveStatus key={saveStatus} status={saveStatus} onRetry={triggerSave} />}</AnimatePresence>
      <PageHeader title={`${page?.title ?? "Página"} — Editar`} desc="Edite blocos, propriedades, SEO e publicação com rastreabilidade." badge={page ? page.status : "draft"}>
        <Button onClick={() => navigate(`/content/${page?.slug}/preview`)} disabled={!page}>Preview</Button>
        <Button onClick={triggerSave}>Salvar rascunho</Button>
        <Button primary onClick={handleSubmitForReview}>Enviar para revisão</Button>
      </PageHeader>
      {saveStatus === "dirty" && <UnsavedChangesBanner />}
      <div className="mb-4 xl:hidden">
        <div className="flex gap-2 overflow-auto pb-1">{["1 Estrutura", "2 Conteúdo", "3 Propriedades", "4 Preview", "5 Publicação"].map((x) => <Badge key={x} tone="blue">{x}</Badge>)}</div>
      </div>
      <div className="grid gap-4 xl:grid-cols-[280px_1fr_340px]">
        <ContentStructureTree page={page} selectedId={selectedSectionId} onSelect={setSelectedSectionId} onAddBlock={handleAddBlock} onRequestDelete={setPendingDeleteId} onReorder={handleReorderSections} />
        <div className="space-y-4"><BlockEditorCanvas section={selectedSection} productSlug={productSlug} onChangeContent={handleChangeContent} onRequestDelete={setPendingDeleteId} /></div>
        <PropertiesPanel page={page} section={selectedSection} />
      </div>
      {pendingDeleteSection && (
        <ConfirmDialog
          title={`Remover bloco "${pendingDeleteSection.label}"?`}
          desc="Esta ação é irreversível: o bloco e seu conteúdo saem imediatamente da página."
          danger
          loading={deletingBlock}
          onCancel={() => setPendingDeleteId(null)}
          onConfirm={handleDeleteBlock}
        />
      )}
    </DndProvider>
  );
}
