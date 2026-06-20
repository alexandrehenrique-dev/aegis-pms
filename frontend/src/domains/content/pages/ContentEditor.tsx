import { useEffect, useRef, useState } from "react";
import { AnimatePresence } from "motion/react";
import { Badge, Button, PageHeader } from "../../../shared/components/Primitives";
import { UnsavedChangesBanner, ConflictAlert } from "../../../shared/components/Banners";
import { FloatingSaveStatus, type SaveStatus } from "../../../shared/components/FloatingSaveStatus";
import { toast } from "../../../core/notifications/toast";
import { ContentStructureTree, BlockEditorCanvas, PropertiesPanel } from "../components/EditorPanels";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { slugify } from "../../../shared/utils/slugify";
import { pagesService } from "../../pages/services/pagesService";
import type { BlockType, Page } from "../../pages/contracts/responses";

export function ContentEditor() {
  const [saveStatus, setSaveStatus] = useState<SaveStatus>("idle");
  const saveTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const { product } = useCurrentProduct();
  const productSlug = product ? slugify(product.name) : "maestro-beton";

  const { data: pages } = useAsyncData(() => pagesService.listPages(productSlug), [productSlug]);
  const [page, setPage] = useState<Page | null>(null);
  const [selectedSectionId, setSelectedSectionId] = useState<string | null>(null);

  useEffect(() => {
    const home = pages?.[0] ?? null;
    setPage(home);
    setSelectedSectionId(home?.sections[0]?.id ?? null);
  }, [pages]);

  const selectedSection = page?.sections.find((s) => s.id === selectedSectionId) ?? null;

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

  const handleChangeContent = async (patch: Record<string, unknown>) => {
    if (!page || !selectedSectionId) return;
    const updated = await pagesService.updateSection(page.productSlug, page.id, selectedSectionId, { content: { ...selectedSection?.content, ...patch } });
    setPage({ ...page, sections: page.sections.map((s) => (s.id === updated.id ? updated : s)) });
    triggerSave();
  };

  const handleAddBlock = async (type: BlockType) => {
    if (!page) return;
    const label = `Novo bloco ${page.sections.length + 1}`;
    const created = await pagesService.createSection(page.productSlug, page.id, { type, label, content: {} });
    setPage({ ...page, sections: [...page.sections, created] });
    setSelectedSectionId(created.id);
    toast.success("Bloco adicionado", { description: `${label} (${type})` });
    triggerSave();
  };

  return (
    <>
      <AnimatePresence>{saveStatus !== "idle" && <FloatingSaveStatus key={saveStatus} status={saveStatus} onRetry={triggerSave} />}</AnimatePresence>
      <PageHeader title={`${page?.title ?? "Página"} — Editar`} desc="Edite blocos, propriedades, SEO e publicação com rastreabilidade." badge={page ? page.status : "draft"}>
        <Button onClick={triggerSave}>Salvar rascunho</Button>
        <Button primary onClick={() => { setSaveStatus("idle"); toast.success("Enviado para revisão.", { description: "Rafael Lima será notificado." }); }}>Enviar para revisão</Button>
      </PageHeader>
      {saveStatus === "dirty" && <UnsavedChangesBanner />}
      <div className="mb-4 xl:hidden">
        <div className="flex gap-2 overflow-auto pb-1">{["1 Estrutura", "2 Conteúdo", "3 Propriedades", "4 Preview", "5 Publicação"].map((x) => <Badge key={x} tone="blue">{x}</Badge>)}</div>
      </div>
      <div className="grid gap-4 xl:grid-cols-[280px_1fr_340px]">
        <ContentStructureTree page={page} selectedId={selectedSectionId} onSelect={setSelectedSectionId} onAddBlock={handleAddBlock} />
        <div className="space-y-4"><BlockEditorCanvas section={selectedSection} onChangeContent={handleChangeContent} /><ConflictAlert /></div>
        <PropertiesPanel page={page} section={selectedSection} />
      </div>
    </>
  );
}
