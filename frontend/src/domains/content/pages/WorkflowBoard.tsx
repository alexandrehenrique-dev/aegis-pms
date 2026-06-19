import { useEffect, useState } from "react";
import { AnimatePresence } from "motion/react";
import { DndProvider } from "react-dnd";
import { HTML5Backend } from "react-dnd-html5-backend";
import { Badge, Button, Card, EmptyState, PageHeader, SkeletonLines, PartialErrorWidget } from "../../../shared/components/Primitives";
import { useViewAsRole } from "../../../core/permissions/ViewAsRoleContext";
import { roleLabels } from "../../../core/permissions/roles";
import { toast } from "../../../core/notifications/toast";
import { contentService } from "../services/contentService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import type { PendingDrop, WFEvent, WFItem, WFStatus } from "../mocks/content.mocks";
import { WFLane } from "../components/WFLane";
import { TransitionModal } from "../components/TransitionModal";

export function WorkflowBoard() {
  const { viewAsRole } = useViewAsRole();
  const { data: initialItems, loading, error } = useAsyncData(() => contentService.listWorkflowItems(), []);
  const [items, setItems] = useState<WFItem[]>([]);
  const [pending, setPending] = useState<PendingDrop | null>(null);
  const [events, setEvents] = useState<WFEvent[]>([]);
  const lanes: WFStatus[] = ["Draft", "In Review", "Published", "Archived"];

  useEffect(() => { if (initialItems) setItems(initialItems); }, [initialItems]);

  if (loading) return <SkeletonLines />;
  if (error) return <PartialErrorWidget />;

  const handleDrop = (itemId: string, toLane: WFStatus) => {
    const item = items.find((i) => i.id === itemId);
    if (!item || item.status === toLane) return;
    setPending({ item, from: item.status, to: toLane });
  };

  const handleConfirm = (comment: string) => {
    if (!pending) return;
    setItems((prev) => prev.map((i) => (i.id === pending.item.id ? { ...i, status: pending.to } : i)));
    const ev: WFEvent = { id: Date.now().toString(), text: `"${pending.item.title}" movido de ${pending.from} para ${pending.to}`, from: pending.from, to: pending.to, time: "agora", comment };
    setEvents((prev) => [ev, ...prev]);
    toast.success(`"${pending.item.title}" → ${pending.to}`, { description: comment || undefined, duration: 3000 });
    if (pending.to === "Archived") toast.info("Evento de auditoria registrado.", { duration: 2500 });
    setPending(null);
  };

  const canPublish = ["super_admin", "tenant_admin", "product_manager"].includes(viewAsRole);

  return (
    <DndProvider backend={HTML5Backend}>
      <AnimatePresence>{pending && <TransitionModal drop={pending} onConfirm={handleConfirm} onCancel={() => setPending(null)} viewAsRole={viewAsRole} />}</AnimatePresence>
      <PageHeader title="Workflow Editorial" desc="Arraste cards entre colunas para mover conteúdo pelo fluxo editorial." badge="Workflow">
        <Badge tone={canPublish ? "violet" : "amber"}>{canPublish ? "Publicação permitida" : "Somente Draft → In Review"}</Badge>
        <Button onClick={() => toast.info("Arraste cards entre colunas. O modal confirma cada mudança de status.", { duration: 4000 })}>Como funciona</Button>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[1fr_280px]">
        <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4 min-w-0">{lanes.map((l) => <WFLane key={l} lane={l} items={items.filter((i) => i.status === l)} onDrop={handleDrop} />)}</div>
        <div className="space-y-3">
          <Card>
            <h3 className="mb-3 font-semibold">Timeline da sessão</h3>
            {events.length === 0 ? <EmptyState compact title="Sem movimentações" description="Mova um card para ver o histórico aqui." /> : (
              <div className="space-y-2">
                {events.map((ev, i) => (
                  <div key={ev.id} className="flex gap-2.5">
                    <div className="mt-1 flex shrink-0 flex-col items-center"><span className="h-2 w-2 rounded-full bg-primary" />{i < events.length - 1 && <span className="mt-1 h-full min-h-[24px] w-px bg-border" />}</div>
                    <div className="pb-2 text-xs">
                      <p className="font-medium leading-snug">{ev.text}</p>
                      {ev.comment && <p className="mt-0.5 italic text-muted-foreground">"{ev.comment}"</p>}
                      <p className="mt-0.5 text-muted-foreground">{ev.time} · {roleLabels[viewAsRole]}</p>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </Card>
          <Card>
            <h3 className="mb-2 font-semibold">Regras do fluxo</h3>
            <div className="space-y-1.5 text-xs">
              {[["Draft → In Review", "Todos os editores"], ["In Review → Published", "PM ou superior"], ["Published → Archived", "Comentário obrigatório"], ["Archived → Draft", "Restaura como nova versão"]].map(([r, d]) => (
                <div key={r} className="flex justify-between"><span className="font-medium">{r}</span><span className="text-muted-foreground">{d}</span></div>
              ))}
            </div>
          </Card>
        </div>
      </div>
    </DndProvider>
  );
}
