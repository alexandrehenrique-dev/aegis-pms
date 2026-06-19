import { useNavigate } from "react-router";
import { Button } from "../../../shared/components/Primitives";
import { RiskBadge } from "../../../shared/components/RiskBadge";

export function AuditEventCard({ e }: { e: string[] }) {
  const navigate = useNavigate();
  return (
    <div className="mb-2 rounded-xl border border-border bg-card p-4">
      <div className="flex flex-col gap-2 md:flex-row md:items-center md:justify-between">
        <div>
          <p className="font-semibold">{e[0]} · {e[1]}</p>
          <p className="text-sm text-muted-foreground">{e[2]} · {e[3]} · {e[4]} · {e[5]}</p>
        </div>
        <div className="flex items-center gap-2">
          <RiskBadge risk={e[6]} />
          <Button onClick={() => navigate("/audit/1")}>Ver detalhe</Button>
        </div>
      </div>
    </div>
  );
}
