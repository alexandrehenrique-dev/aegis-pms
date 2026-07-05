import { useNavigate } from "react-router";
import { AlertTriangle, ChevronRight } from "lucide-react";
import { Button, Card, PageHeader, PartialErrorWidget, PermissionHint, SkeletonLines } from "../../../shared/components/Primitives";
import { settingsService } from "../services/settingsService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { SettingsCard } from "../components/SettingsBits";
import { useAuth } from "../../../core/auth/useAuth";
import { usersService } from "../../users/services/usersService";
import type { SettingCard as SettingCardType } from "../contracts/responses";

function needsAttention(card: SettingCardType): boolean {
  const status = card.status.toLowerCase();
  return status.includes("atenção") || status.includes("pendente") || status.includes("crítico") || status.includes("alterado");
}

export function SettingsOverview() {
  const navigate = useNavigate();
  const { effectiveProduct, effectiveTenant } = useAuth();
  const { data: settingCards, loading, error } = useAsyncData(() => settingsService.listSettingCards(effectiveProduct?.id), [effectiveProduct?.id]);
  const { data: tenantUsers } = useAsyncData(() => usersService.listUsers(effectiveTenant?.id), [effectiveTenant?.id]);
  const pendingInvites = (tenantUsers ?? []).filter((user) => user.status === "convidado" || user.inviteStatus === "pendente").length;
  const attnItems: [string, string][] = [
    ...(pendingInvites > 0 ? [[`${pendingInvites} convite${pendingInvites > 1 ? "s" : ""} pendente${pendingInvites > 1 ? "s" : ""}.`, "/users"] as [string, string]] : []),
    ...((settingCards ?? []).filter(needsAttention).map((card) => [`${card.name}: ${card.status}.`, card.name === "Auditoria" ? "/audit" : "/settings/security"] as [string, string])),
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
          {attnItems.length === 0 && <p className="text-sm text-muted-foreground">Nenhuma pendência administrativa para o produto atual.</p>}
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
