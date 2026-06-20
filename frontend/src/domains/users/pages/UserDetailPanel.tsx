import { useState } from "react";
import { useNavigate } from "react-router";
import { AnimatePresence } from "motion/react";
import { Loader2 } from "lucide-react";
import { Button, Card, PageHeader } from "../../../shared/components/Primitives";
import { ConfirmDialog } from "../../../shared/components/ConfirmDialog";
import { SettingsSection } from "../../settings/components/SettingsBits";
import { AuditTimeline } from "../../audit/pages/AuditTimeline";
import { toast } from "../../../core/notifications/toast";
import { usersService } from "../services/usersService";

export function UserDetailPanel() {
  const navigate = useNavigate();
  const email = "ana@byop.com";
  const [resending, setResending] = useState(false);
  const [confirmBlock, setConfirmBlock] = useState(false);
  const [blocking, setBlocking] = useState(false);
  const [blocked, setBlocked] = useState(false);

  const handleResend = async () => {
    setResending(true);
    try {
      await usersService.resendInvite(email);
      toast.success("Convite reenviado!", { description: email });
    } finally {
      setResending(false);
    }
  };

  const handleBlock = async () => {
    setBlocking(true);
    try {
      await usersService.blockUser(email);
      setBlocked(true);
      toast.success("Usuário bloqueado.", { description: "Ana Martins perdeu acesso à plataforma." });
      setConfirmBlock(false);
    } finally {
      setBlocking(false);
    }
  };

  return (
    <>
      <AnimatePresence>
        {confirmBlock && <ConfirmDialog title="Bloquear este usuário?" desc="Ana Martins perderá acesso imediato à plataforma até ser desbloqueada." danger loading={blocking} onConfirm={handleBlock} onCancel={() => setConfirmBlock(false)} />}
      </AnimatePresence>
      <PageHeader title="Ana Martins" module="Users" desc="Perfil, papéis, produtos, permissões efetivas e atividade recente." badge="Tenant Admin">
        <Button onClick={handleResend} disabled={resending}>{resending && <Loader2 size={15} className="animate-spin" />}{resending ? "Reenviando..." : "Reenviar convite"}</Button>
        <Button onClick={() => setConfirmBlock(true)} disabled={blocked}>{blocked ? "Bloqueado" : "Bloquear"}</Button>
        <Button primary onClick={() => navigate("/settings/permissions")}>Editar permissões</Button>
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
