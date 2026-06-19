import { Badge } from "./Primitives";

export function RiskBadge({ risk }: { risk: string }) {
  return <Badge tone={risk === "alto" || risk === "alta" ? "red" : risk === "médio" || risk === "média" ? "amber" : "green"}>{risk}</Badge>;
}
