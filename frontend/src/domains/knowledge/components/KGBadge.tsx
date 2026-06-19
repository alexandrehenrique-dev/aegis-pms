import { kgColor } from "../mocks/knowledge.mocks";

export function KGBadge({ type }: { type: string }) {
  const c = kgColor[type] || "#374151";
  return <span style={{ color: c, backgroundColor: `${c}18` }} className="rounded-full px-2 py-0.5 text-[10px] font-bold">{type}</span>;
}
