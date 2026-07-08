import { useNavigate } from "react-router";
import { Button } from "../../../shared/components/Primitives";
import { RiskBadge } from "../../../shared/components/RiskBadge";
import type { AuditEvent } from "../contracts/responses";

export function AuditEventCard({ e }: { e: AuditEvent }) {
  const navigate = useNavigate();
  return (
    <div className="mb-2 rounded-xl border border-border bg-card p-4">
      <div className="flex flex-col gap-2 md:flex-row md:items-center md:justify-between">
        <div>
          <p className="font-semibold">{e.actor} · {e.action}</p>
          <p className="text-sm text-muted-foreground">{e.target} · {e.tenant} · {e.module} · {e.time}</p>
        </div>
        <div className="flex items-center gap-2">
          <RiskBadge risk={e.risk} />
          <Button onClick={() => navigate(`/audit/${e.id}`)} disabled={!e.id}>Ver detalhe</Button>
        </div>
      </div>
    </div>
  );
}
