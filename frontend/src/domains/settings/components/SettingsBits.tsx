import { Button, Card } from "../../../shared/components/Primitives";
import { RiskBadge } from "../../../shared/components/RiskBadge";

export function SettingsCard({ c }: { c: string[] }) {
  return (
    <Card>
      <div className="flex items-start justify-between">
        <div><h3 className="font-semibold">{c[0]}</h3><p className="mt-1 text-sm text-muted-foreground">{c[1]}</p></div>
        <RiskBadge risk={c[5]} />
      </div>
      <div className="mt-4 grid gap-2 text-xs text-muted-foreground"><span>Status: {c[2]}</span><span>Última atualização: {c[3]}</span><span>Responsável: {c[4]}</span></div>
      <Button>Abrir</Button>
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
