import type { Ref } from "react";
import { useDrag } from "react-dnd";
import { GripVertical } from "lucide-react";
import { Badge } from "../../../shared/components/Primitives";
import { WF_DRAG, wfBadgeTone, type WFItem } from "../mocks/content.mocks";

export function WFCard({ item }: { item: WFItem }) {
  const [{ isDragging }, drag] = useDrag(() => ({ type: WF_DRAG, item: { id: item.id }, collect: (m) => ({ isDragging: m.isDragging() }) }));
  return (
    <div ref={drag as unknown as Ref<HTMLDivElement>} style={{ opacity: isDragging ? 0.35 : 1 }} className="mb-2 cursor-grab rounded-xl border border-border bg-card p-3 text-sm shadow-[0_1px_3px_rgba(0,0,0,0.04)] transition active:cursor-grabbing hover:border-primary/30 hover:shadow-[0_2px_10px_rgba(124,58,237,0.08)]">
      <div className="flex items-start justify-between gap-2"><b className="leading-snug">{item.title}</b><GripVertical size={14} className="mt-0.5 shrink-0 text-muted-foreground/40" /></div>
      <p className="mt-1 text-xs text-muted-foreground">{item.type} · {item.lang} · {item.author}</p>
      <div className="mt-2 flex items-center justify-between"><Badge tone={wfBadgeTone[item.status]}>{item.status}</Badge><span className="font-mono text-[10px] text-muted-foreground">{item.version}</span></div>
    </div>
  );
}
