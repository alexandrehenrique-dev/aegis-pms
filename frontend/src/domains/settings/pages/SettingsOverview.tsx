import { useNavigate } from "react-router";
import { AlertTriangle, ChevronRight } from "lucide-react";
import { Button, Card, PageHeader, PartialErrorWidget, PermissionHint, SkeletonLines } from "../../../shared/components/Primitives";
import { settingsService } from "../services/settingsService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { SettingsCard } from "../components/SettingsBits";
import { useAuth } from "../../../core/auth/useAuth";

export function SettingsOverview() {
  const navigate = useNavigate();
  const { effectiveProduct } = useAuth();
  const { data: settingCards, loading, error } = useAsyncData(() => settingsService.listSettingCards(effectiveProduct?.id), [effectiveProduct?.id]);
  const attnItems: [string, string][] = [
    ["2 usuários com convite pendente.", "/users"],
    ["1 integração sem configuração.", "/settings/security"],
    ["Permissões de Editor foram alteradas há 2 dias.", "/settings/permissions"],
    ["Auditoria possui eventos críticos recentes.", "/audit"],
  ];
  return (
    <>
      <PageHeader title="Configurações" desc="Controle administrativo, governança e segurança operacional do produto." badge="Governança">
        <Button onClick={() => navigate("/audit")}>Ver auditoria</Button>
        <Button primary onClick={() => navigate("/settings/product")}>Configurar produto</Button>
      </PageHeader>
      {loading ? <SkeletonLines /> : error || !settingCards ? <PartialErrorWidget /> : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">{settingCards.map((c, i) => <SettingsCard key={c.name} c={c} data-tour={i === 0 ? "settings-geral" : undefined} />)}</div>
      )}
      <div className="mt-4 grid gap-4 xl:grid-cols-[1fr_360px]">
        <Card>
          <h2 className="mb-3 text-lg font-semibold">Atenção administrativa</h2>
          {attnItems.map(([x, path]) => (
            <button key={x} onClick={() => navigate(path)} className="mb-2 flex w-full items-center justify-between rounded-xl border border-border p-3 text-left text-sm transition hover:border-primary/30 hover:bg-muted">
              <span className="flex items-start gap-2"><AlertTriangle size={14} className="mt-0.5 shrink-0 text-[#b45309]" />{x}</span>
              <ChevronRight size={13} className="shrink-0 text-muted-foreground" />
            </button>
          ))}
        </Card>
        <Card>
          <h2 className="mb-3 text-lg font-semibold">Estados</h2>
          <SkeletonLines />
          <div className="mt-3"><PermissionHint /></div>
          <PartialErrorWidget />
        </Card>
      </div>
    </>
  );
}
