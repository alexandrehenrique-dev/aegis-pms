import { Button, Card, PageHeader } from "../../../shared/components/Primitives";
import { SettingsSection } from "../../settings/components/SettingsBits";
import { AuditTimeline } from "../../audit/pages/AuditTimeline";

export function UserDetailPanel() {
  return (
    <>
      <PageHeader title="Ana Martins" module="Users" desc="Perfil, papéis, produtos, permissões efetivas e atividade recente." badge="Tenant Admin">
        <Button>Reenviar convite</Button>
        <Button>Bloquear</Button>
        <Button primary>Editar permissões</Button>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
        <div className="space-y-4">
          <Card>
            <div className="flex gap-4">
              <div className="grid h-14 w-14 place-items-center rounded-full bg-primary text-white font-semibold">AM</div>
              <div><h2 className="text-xl font-semibold">Ana Martins</h2><p className="text-sm text-muted-foreground">ana@byop.com · ativo · último acesso há 8 min</p></div>
            </div>
          </Card>
          <SettingsSection title="Papéis e produtos permitidos" items={["Tenant Admin", "Todos os produtos", "Permissões efetivas completas", "Segurança habilitada"]} />
          <Card><h2 className="mb-3 text-lg font-semibold">Atividade recente</h2><AuditTimeline compact /></Card>
        </div>
        <Card>
          <h2 className="mb-3 text-lg font-semibold">Segurança e convites</h2>
          <div className="space-y-2 text-sm">
            <div className="rounded-lg bg-muted p-3">Sessão ativa em Chrome</div>
            <div className="rounded-lg bg-muted p-3">2FA futuro: pendente</div>
            <div className="rounded-lg bg-muted p-3">Convites: nenhum pendente</div>
          </div>
        </Card>
      </div>
    </>
  );
}
