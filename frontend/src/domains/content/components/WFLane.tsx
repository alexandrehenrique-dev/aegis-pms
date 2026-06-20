import type { Ref } from "react";
import { useDrop } from "react-dnd";
import { Badge, EmptyState } from "../../../shared/components/Primitives";
import { WF_DRAG, wfBadgeTone, wfLaneBorder, type WFItem, type WFStatus } from "../mocks/content.mocks";
import { WFCard } from "./WFCard";

export function WFLane({ lane, items, onDrop }: { lane: WFStatus; items: WFItem[]; onDrop: (id: string, to: WFStatus) => void }) {
  const [{ isOver }, drop] = useDrop(() => ({ accept: WF_DRAG, drop: (d: { id: string }) => onDrop(d.id, lane), collect: (m) => ({ isOver: m.isOver() }) }), [lane, onDrop]);
  return (
    <div ref={drop as unknown as Ref<HTMLDivElement>} className={`flex flex-col rounded-2xl border-t-2 border border-border bg-card p-3 transition-all ${wfLaneBorder[lane]} ${isOver ? "ring-2 ring-primary/25 bg-primary/[0.015]" : ""}`} style={{ minHeight: 360 }}>
      <div className="mb-3 flex items-center justify-between"><h3 className="font-semibold">{lane}</h3><Badge tone={wfBadgeTone[lane]}>{items.length}</Badge></div>
      <div className="flex-1 space-y-0">{items.length ? items.map((i) => <WFCard key={i.id} item={i} />) : <EmptyState compact title="Lane vazia" description="Arraste um card para cá." />}</div>
      <div className={`mt-2 rounded-lg border border-dashed p-2 text-center text-xs transition-colors ${isOver ? "border-primary/50 bg-primary/5 text-primary" : "border-border text-muted-foreground"}`}>{isOver ? "↓ Soltar aqui" : "Arraste para cá"}</div>
    </div>
  );
}
