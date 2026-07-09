import { useState, type ComponentType } from "react";
import { useNavigate } from "react-router";
import { Badge, Button, Card, PageHeader, SkeletonLines, PartialErrorWidget } from "../../../shared/components/Primitives";
import { toast } from "../../../core/notifications/toast";
import { productsService } from "../services/productsService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { useAuth } from "../../../core/auth/useAuth";
import type { ModuleState } from "../../../shared/types";
import type { ModuleCatalogItem } from "../contracts/responses";
import { KNOWLEDGE_GRAPH_DEPENDENCIES } from "../../../core/products/moduleDefaults";

/** Rota de destino ao abrir um módulo já habilitado */
const MODULE_ROUTES: Record<string, string> = {
  "Conteúdo":       "/content",
  "Assets":         "/assets",
  "Forms":          "/forms",
  "Analytics":      "/analytics",
  "SEO":            "/settings/product",
  "Knowledge Graph":"/knowledge",
  "Pages":          "/pages",
};

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

export function ModuleCatalog({ compact = false, productId }: { compact?: boolean; productId?: string }) {
  const navigate = useNavigate();
  const { effectiveProduct, updateProduct } = useAuth();
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

  const missingKnowledgeGraphDependencies = KNOWLEDGE_GRAPH_DEPENDENCIES.filter(
    (dependency) => list.find((item) => item.name === dependency)?.state !== "habilitado",
  );
  const knowledgeGraphDependencyMessage = `Knowledge Graph exige os módulos ${KNOWLEDGE_GRAPH_DEPENDENCIES.join(" e ")} habilitados primeiro.`;

  const blockedByDependency = (m: ModuleCatalogItem) =>
    m.name === "Knowledge Graph" && missingKnowledgeGraphDependencies.length > 0
      ? knowledgeGraphDependencyMessage
      : null;

  const syncProductSnapshot = async () => {
    if (!productId) return;
    const product = (await productsService.listProducts()).find((item) => item.id === productId || item.name === productId);
    const target = product ?? effectiveProduct;
    if (!target) return;
    updateProduct(productId, {
      name: target.name,
      type: target.type,
      status: target.status,
      modules: target.modulesList ?? [],
    });
  };

  const handleAction = async (m: ModuleCatalogItem) => {
    if (m.state === "habilitado") {
      const route = MODULE_ROUTES[m.name];
      if (route) {
        navigate(route);
      } else {
        toast.success("Módulo habilitado", { description: m.name });
      }
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
    await productsService.enableModule(m.name, productId);
    await syncProductSnapshot();
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
      toast.error(knowledgeGraphDependencyMessage);
      return;
    }
    await Promise.all(Array.from(selected).map((name) => productsService.enableModule(name, productId)));
    await syncProductSnapshot();
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
