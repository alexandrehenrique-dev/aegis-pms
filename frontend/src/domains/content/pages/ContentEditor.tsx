import { useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router";
import { AnimatePresence } from "motion/react";
import { Badge, Button, EmptyState, PageHeader } from "../../../shared/components/Primitives";
import { UnsavedChangesBanner, ConflictAlert } from "../../../shared/components/Banners";
import { ConfirmDialog } from "../../../shared/components/ConfirmDialog";
import { FloatingSaveStatus, type SaveStatus } from "../../../shared/components/FloatingSaveStatus";
import { toast } from "../../../core/notifications/toast";
import { ContentStructureTree, BlockEditorCanvas, PropertiesPanel } from "../components/EditorPanels";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { slugify } from "../../../shared/utils/slugify";
import { pagesService } from "../../pages/services/pagesService";
import { DEFAULT_BLOCK_CONTENT } from "../../pages/blockDefaults";
import type { BlockType, Page } from "../../pages/contracts/responses";

/**
 * Quem chega a esta tela já passou pelo gate de rota (`RequireRole` +
 * `roleBlockedRoutePrefixes["/content/*\/editor"]`, ver core/permissions/roles.ts)
 * — hoje só `viewer` é bloqueado. Editor, Product Manager, Tenant Admin e
 * Super Admin podem adicionar, editar e remover blocos; não há gating
 * adicional por ação dentro do editor porque nenhum documento (004, V1)
 * distingue essas ações dentro do mesmo papel "Editor de Conteúdo" (07.04).
 */
export function ContentEditor() {
  const navigate = useNavigate();
  const { id: pageSlug } = useParams<{ id: string }>();
  const [saveStatus, setSaveStatus] = useState<SaveStatus>("idle");
  const saveTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const { product } = useCurrentProduct();
  const productSlug = product ? slugify(product.name) : "maestro-beton";

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

  const handleChangeContent = async (patch: Record<string, unknown>) => {
    if (!page || !selectedSectionId) return;
    await pagesService.updateSection(page.productSlug, page.id, selectedSectionId, { content: { ...selectedSection?.content, ...patch } });
    await refreshPage(page);
    triggerSave();
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

  if (!loadingPage && !page) {
    return (
      <EmptyState
        title="Página não encontrada"
        description={`Nenhuma página com slug "${pageSlug}" existe em ${productSlug}. Volte para a lista de páginas e escolha uma existente, ou crie uma nova.`}
      />
    );
  }

  return (
    <>
      <AnimatePresence>{saveStatus !== "idle" && <FloatingSaveStatus key={saveStatus} status={saveStatus} onRetry={triggerSave} />}</AnimatePresence>
      <PageHeader title={`${page?.title ?? "Página"} — Editar`} desc="Edite blocos, propriedades, SEO e publicação com rastreabilidade." badge={page ? page.status : "draft"}>
        <Button onClick={() => navigate(`/content/${page?.slug}/preview`)} disabled={!page}>Preview</Button>
        <Button onClick={triggerSave}>Salvar rascunho</Button>
        <Button primary onClick={() => { setSaveStatus("idle"); toast.success("Enviado para revisão.", { description: "Rafael Lima será notificado." }); }}>Enviar para revisão</Button>
      </PageHeader>
      {saveStatus === "dirty" && <UnsavedChangesBanner />}
      <div className="mb-4 xl:hidden">
        <div className="flex gap-2 overflow-auto pb-1">{["1 Estrutura", "2 Conteúdo", "3 Propriedades", "4 Preview", "5 Publicação"].map((x) => <Badge key={x} tone="blue">{x}</Badge>)}</div>
      </div>
      <div className="grid gap-4 xl:grid-cols-[280px_1fr_340px]">
        <ContentStructureTree page={page} selectedId={selectedSectionId} onSelect={setSelectedSectionId} onAddBlock={handleAddBlock} onRequestDelete={setPendingDeleteId} />
        <div className="space-y-4"><BlockEditorCanvas section={selectedSection} onChangeContent={handleChangeContent} onRequestDelete={setPendingDeleteId} /><ConflictAlert /></div>
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
    </>
  );
}
