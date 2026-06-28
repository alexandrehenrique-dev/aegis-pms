import { useState } from "react";
import { useNavigate } from "react-router";
import { Button, Card, PageHeader } from "../../../shared/components/Primitives";
import { OperationalTimeline } from "../../../shared/components/OperationalTimeline";
import { ModuleCatalog } from "../components/ModuleCatalog";
import { toast } from "../../../core/notifications/toast";
import { useAuth } from "../../../core/auth/AuthContext";

const TABS = ["Visão Geral", "Módulos", "Conteúdo", "Assets", "Forms", "Analytics", "Graph", "Configurações"] as const;
const TAB_ROUTES: Record<string, string> = { "Conteúdo": "/content", "Assets": "/assets", "Forms": "/forms", "Analytics": "/analytics", "Graph": "/knowledge/graph", "Configurações": "/settings/product" };

function GeneralPanel() {
  return (
    <Card>
      <h2 className="mb-4 text-lg font-semibold">Dados gerais</h2>
      <div className="grid gap-3 md:grid-cols-2">
        {[["Status", "Ativo"], ["Tipo", "Site Institucional"], ["Slug", "maestro-beton"], ["Idioma", "pt-BR"], ["Descrição", "Produto institucional governado"], ["Módulos", "Conteúdo, Assets, Forms, Analytics"]].map((r) => (
          <div key={r[0]} className="rounded-xl bg-muted p-3"><p className="text-xs text-muted-foreground">{r[0]}</p><p className="font-medium">{r[1]}</p></div>
        ))}
      </div>
    </Card>
  );
}

function DomainShortcutPanel({ tab }: { tab: string }) {
  const navigate = useNavigate();
  const route = TAB_ROUTES[tab];
  return (
    <Card>
      <h2 className="mb-2 text-lg font-semibold">{tab}</h2>
      <p className="text-sm text-muted-foreground">Acesse a área de {tab.toLowerCase()} deste produto para detalhes completos.</p>
      <Button primary onClick={() => route && navigate(route)}>Ir para {tab}</Button>
    </Card>
  );
}

export function ProductDetail() {
  const navigate = useNavigate();
  const [tab, setTab] = useState<string>(TABS[0]);
  const { effectiveProduct } = useAuth();

  const handleSaveView = () => {
    localStorage.setItem("products:lastView", tab);
    toast.success("Visão salva!", { description: `A aba "${tab}" será aberta por padrão na próxima visita.` });
  };

  return (
    <>
      <PageHeader title="Maestro Beton" module="Visão Geral" desc="Dados administrativos, módulos, equipe, domínios futuros e auditoria recente.">
        <Button onClick={() => navigate("/settings/product")}>Editar</Button>
        <Button primary onClick={handleSaveView}>Salvar visão</Button>
      </PageHeader>
      <div className="mb-4 flex gap-2 overflow-auto pb-1">
        {TABS.map((t) => (
          <button key={t} onClick={() => setTab(t)} className={`whitespace-nowrap rounded-lg border px-3 py-2 text-sm ${tab === t ? "border-primary bg-primary text-white" : "border-border bg-card"}`}>{t}</button>
        ))}
      </div>
      <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
        {tab === "Visão Geral" ? <GeneralPanel /> : tab === "Módulos" ? <ModuleCatalog compact productId={effectiveProduct?.id} /> : <DomainShortcutPanel tab={tab} />}
        <Card><h2 className="mb-3 text-lg font-semibold">Auditoria recente</h2><OperationalTimeline /></Card>
      </div>
    </>
  );
}
