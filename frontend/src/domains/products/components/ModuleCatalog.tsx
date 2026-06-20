import { useState, type ComponentType } from "react";
import { Badge, Button, Card, PageHeader, SkeletonLines, PartialErrorWidget } from "../../../shared/components/Primitives";
import { toast } from "../../../core/notifications/toast";
import { productsService } from "../services/productsService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import type { ModuleState } from "../../../shared/types";
import type { ModuleCatalogItem } from "../contracts/responses";
import { KNOWLEDGE_GRAPH_DEPENDENCY } from "../../../core/products/moduleDefaults";

function ModuleCard({ Icon, name, desc, state, maturity, dep, impact, selected, onToggleSelect, onAction }: {
  Icon: ComponentType<{ size?: number; className?: string }>; name: string; desc: string; state: ModuleState; maturity: string; dep: string; impact: string;
  selected: boolean; onToggleSelect: () => void; onAction: () => void;
}) {
  const tone = state === "habilitado" ? "green" : state === "dependência" ? "amber" : state === "sem permissão" ? "red" : "neutral";
  const cta = state === "habilitado" ? "Abrir" : state === "desabilitado" ? "Habilitar" : state === "dependência" ? "Resolver dependência" : state === "sem permissão" ? "Sem permissão" : "Ver roadmap";
  const canSelect = state === "desabilitado" || state === "dependência";
  return (
    <Card>
      <div className="flex items-start justify-between">
        <Icon size={20} className="text-primary" />
        <div className="flex items-center gap-2">
          {canSelect && <input type="checkbox" checked={selected} onChange={onToggleSelect} aria-label={`Selecionar ${name}`} />}
          <Badge tone={tone}>{state}</Badge>
        </div>
      </div>
      <h3 className="mt-4 font-semibold">{name}</h3>
      <p className="mt-1 text-sm text-muted-foreground">{desc}</p>
      <div className="mt-4 space-y-2 text-xs text-muted-foreground"><p><b>Dependências:</b> {dep}</p><p><b>Impacto:</b> {impact}</p></div>
      <div className="mt-4 flex items-center justify-between"><Badge tone="blue">{maturity}</Badge><Button disabled={state === "sem permissão"} onClick={onAction}>{cta}</Button></div>
    </Card>
  );
}

export function ModuleCatalog({ compact = false }: { compact?: boolean }) {
  const { data: loaded, loading, error } = useAsyncData(() => productsService.listModuleCatalog(), []);
  const [modules, setModules] = useState<ModuleCatalogItem[] | null>(null);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const list = modules ?? loaded;

  if (loading && !list) return <SkeletonLines />;
  if (error || !list) return <PartialErrorWidget />;

  const toggleSelect = (name: string) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(name)) next.delete(name); else next.add(name);
      return next;
    });
  };

  const contentEnabled = list.find((x) => x.name === KNOWLEDGE_GRAPH_DEPENDENCY)?.state === "habilitado";

  const blockedByDependency = (m: ModuleCatalogItem) =>
    m.name === "Knowledge Graph" && !contentEnabled
      ? `Knowledge Graph exige o módulo ${KNOWLEDGE_GRAPH_DEPENDENCY} habilitado primeiro.`
      : null;

  const handleAction = async (m: ModuleCatalogItem) => {
    if (m.state === "habilitado") {
      toast.success("Módulo já habilitado", { description: m.name });
      return;
    }
    if (m.state === "futuro") {
      toast("Disponível em breve", { description: `${m.name} ainda está no roadmap.` });
      return;
    }
    const blocked = blockedByDependency(m);
    if (blocked) {
      toast.error(blocked);
      return;
    }
    await productsService.enableModule(m.name);
    const refreshed = await productsService.listModuleCatalog();
    setModules(refreshed);
    toast.success("Módulo habilitado!", { description: m.name });
  };

  const handleEnableSelected = async () => {
    if (selected.size === 0) {
      toast.error("Selecione ao menos um módulo.");
      return;
    }
    const blocked = list.filter((m) => selected.has(m.name) && blockedByDependency(m));
    if (blocked.length > 0) {
      toast.error(`Knowledge Graph exige o módulo ${KNOWLEDGE_GRAPH_DEPENDENCY} habilitado primeiro.`);
      return;
    }
    await Promise.all(Array.from(selected).map((name) => productsService.enableModule(name)));
    const refreshed = await productsService.listModuleCatalog();
    setModules(refreshed);
    toast.success(`${selected.size} módulo(s) habilitado(s)!`);
    setSelected(new Set());
  };

  return (
    <div>
      {!compact && (
        <PageHeader title="Catálogo de Módulos" module="Módulos" desc="Catálogo operacional de capacidades da plataforma. Não é loja; é controle de operação.">
          <Button primary onClick={handleEnableSelected}>Habilitar selecionados</Button>
        </PageHeader>
      )}
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {list.slice(0, compact ? 6 : list.length).map((m) => (
          <ModuleCard key={m.name} Icon={m.Icon} name={m.name} desc={m.desc} state={m.state} maturity={m.maturity} dep={m.dependency} impact={m.impact} selected={selected.has(m.name)} onToggleSelect={() => toggleSelect(m.name)} onAction={() => handleAction(m)} />
        ))}
      </div>
    </div>
  );
}
