import { Badge } from "../../../shared/components/Primitives";

export function UserStatusBadge({ s }: { s: string }) {
  return <Badge tone={s === "ativo" ? "green" : s === "convidado" ? "blue" : s === "bloqueado" ? "red" : "neutral"}>{s}</Badge>;
}
