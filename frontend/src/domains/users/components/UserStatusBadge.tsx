import { Lock, Mail, UserX } from "lucide-react";
import { Badge } from "../../../shared/components/Primitives";

// "removido" (ADR-0020, soft delete) precisa ser visualmente distinto de
// "bloqueado": ambos perdem acesso, mas só "removido" saiu do tenant.
export function UserStatusBadge({ s }: { s: string }) {
  const tone = s === "ativo" ? "green" : s === "convidado" ? "blue" : s === "bloqueado" ? "amber" : s === "removido" ? "red" : "neutral";
  return (
    <Badge tone={tone}>
      <span className="inline-flex items-center gap-1">
        {s === "convidado" && <Mail size={11} />}
        {s === "bloqueado" && <Lock size={11} />}
        {s === "removido" && <UserX size={11} />}
        {s}
      </span>
    </Badge>
  );
}
