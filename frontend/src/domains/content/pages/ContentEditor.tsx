import { useRef, useState } from "react";
import { AnimatePresence } from "motion/react";
import { Badge, Button, PageHeader } from "../../../shared/components/Primitives";
import { UnsavedChangesBanner, ConflictAlert } from "../../../shared/components/Banners";
import { FloatingSaveStatus, type SaveStatus } from "../../../shared/components/FloatingSaveStatus";
import { toast } from "../../../core/notifications/toast";
import { ContentStructureTree, BlockEditorCanvas, PropertiesPanel } from "../components/EditorPanels";

export function ContentEditor() {
  const [saveStatus, setSaveStatus] = useState<SaveStatus>("idle");
  const saveTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

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

  return (
    <>
      <AnimatePresence>{saveStatus !== "idle" && <FloatingSaveStatus key={saveStatus} status={saveStatus} onRetry={triggerSave} />}</AnimatePresence>
      <PageHeader title="Home — Editar" desc="Edite blocos, propriedades, SEO e publicação com rastreabilidade." badge="Draft">
        <Button onClick={triggerSave}>Salvar rascunho</Button>
        <Button primary onClick={() => { setSaveStatus("idle"); toast.success("Enviado para revisão.", { description: "Rafael Lima será notificado." }); }}>Enviar para revisão</Button>
      </PageHeader>
      {saveStatus === "dirty" && <UnsavedChangesBanner />}
      <div className="mb-4 xl:hidden">
        <div className="flex gap-2 overflow-auto pb-1">{["1 Estrutura", "2 Conteúdo", "3 Propriedades", "4 Preview", "5 Publicação"].map((x) => <Badge key={x} tone="blue">{x}</Badge>)}</div>
      </div>
      <div className="grid gap-4 xl:grid-cols-[280px_1fr_340px]" onInput={triggerSave}>
        <ContentStructureTree />
        <div className="space-y-4"><BlockEditorCanvas /><ConflictAlert /></div>
        <PropertiesPanel />
      </div>
    </>
  );
}
