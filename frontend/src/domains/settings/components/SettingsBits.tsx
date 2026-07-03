import { useNavigate } from "react-router";
import { Button, Card } from "../../../shared/components/Primitives";
import { RiskBadge } from "../../../shared/components/RiskBadge";
import type { SettingCard as SettingCardType } from "../contracts/responses";

const SETTINGS_ROUTES: Record<string, string> = {
  "Produto": "/settings/product", "Tenant": "/settings/tenant", "Equipe": "/users",
  "Permissões": "/settings/permissions", "Integrações": "/settings/security", "Segurança": "/settings/security",
  "Auditoria": "/audit", "SEO": "/settings/product", "Domínios futuros": "/settings/product",
};

export function SettingsCard({ c, "data-tour": dataTour }: { c: SettingCardType; "data-tour"?: string }) {
  const navigate = useNavigate();
  return (
    <Card data-tour={dataTour}>
      <div className="flex items-start justify-between">
        <div><h3 className="font-semibold">{c.name}</h3><p className="mt-1 text-sm text-muted-foreground">{c.description}</p></div>
        <RiskBadge risk={c.risk} />
      </div>
      <div className="mt-4 grid gap-2 text-xs text-muted-foreground"><span>Status: {c.status}</span><span>Última atualização: {c.lastUpdated}</span><span>Responsável: {c.owner}</span></div>
      <Button onClick={() => navigate(SETTINGS_ROUTES[c.name] ?? "/settings")}>Abrir</Button>
    </Card>
  );
}

export function SettingsSection({ title, items }: { title: string; items: string[] }) {
  return (
    <Card>
      <h2 className="mb-3 text-lg font-semibold">{title}</h2>
      <div className="grid gap-3 md:grid-cols-2">{items.map((i) => <div key={i} className="rounded-xl border border-border p-3 text-sm"><b>{i}</b><p className="text-muted-foreground">Configuração com auditoria e controle de risco.</p></div>)}</div>
    </Card>
  );
}
