import type { ComponentType } from "react";
import { Badge, Button, Card, PageHeader, SkeletonLines, PartialErrorWidget } from "../../../shared/components/Primitives";
import { productsService } from "../services/productsService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
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
  const { data: modules, loading, error } = useAsyncData(() => productsService.listModuleCatalog(), []);

  if (loading) return <SkeletonLines />;
  if (error || !modules) return <PartialErrorWidget />;

  return (
    <div>
      {!compact && (
        <PageHeader title="Catálogo de Módulos" module="Módulos" desc="Catálogo operacional de capacidades da plataforma. Não é loja; é controle de operação.">
          <Button>Loading state</Button>
          <Button primary>Habilitar selecionados</Button>
        </PageHeader>
      )}
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {modules.slice(0, compact ? 6 : modules.length).map((m) => (
          <ModuleCard key={m.name} Icon={m.Icon} name={m.name} desc={m.desc} state={m.state} maturity={m.maturity} dep={m.dependency} impact={m.impact} />
        ))}
      </div>
    </div>
  );
}
