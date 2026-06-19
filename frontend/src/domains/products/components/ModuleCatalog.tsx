import type { ComponentType } from "react";
import { Badge, Button, Card, PageHeader } from "../../../shared/components/Primitives";
import { modules } from "../../dashboard/mocks/dashboard.mocks";
import type { ModuleState } from "../../../shared/types";

function ModuleCard({ Icon, name, desc, state, maturity, dep, impact }: { Icon: ComponentType<{ size?: number; className?: string }>; name: string; desc: string; state: ModuleState; maturity: string; dep: string; impact: string }) {
  const tone = state === "habilitado" ? "green" : state === "dependência" ? "amber" : state === "sem permissão" ? "red" : "neutral";
  const cta = state === "habilitado" ? "Abrir" : state === "desabilitado" ? "Habilitar" : state === "dependência" ? "Resolver dependência" : state === "sem permissão" ? "Sem permissão" : "Ver roadmap";
  return (
    <Card>
      <div className="flex items-start justify-between"><Icon size={20} className="text-primary" /><Badge tone={tone}>{state}</Badge></div>
      <h3 className="mt-4 font-semibold">{name}</h3>
      <p className="mt-1 text-sm text-muted-foreground">{desc}</p>
      <div className="mt-4 space-y-2 text-xs text-muted-foreground"><p><b>Dependências:</b> {dep}</p><p><b>Impacto:</b> {impact}</p></div>
      <div className="mt-4 flex items-center justify-between"><Badge tone="blue">{maturity}</Badge><Button>{cta}</Button></div>
    </Card>
  );
}

export function ModuleCatalog({ compact = false }: { compact?: boolean }) {
  return (
    <div>
      {!compact && (
        <PageHeader title="Catálogo de Módulos" module="Módulos" desc="Catálogo operacional de capacidades da plataforma. Não é loja; é controle de operação.">
          <Button>Loading state</Button>
          <Button primary>Habilitar selecionados</Button>
        </PageHeader>
      )}
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {modules.slice(0, compact ? 6 : modules.length).map(([Icon, name, desc, state, maturity, dep, impact]) => (
          <ModuleCard key={name as string} Icon={Icon as ComponentType<{ size?: number; className?: string }>} name={name as string} desc={desc as string} state={state as ModuleState} maturity={maturity as string} dep={dep as string} impact={impact as string} />
        ))}
      </div>
    </div>
  );
}
