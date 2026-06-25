import { useEffect, useState } from "react";
import { useNavigate } from "react-router";
import { AnimatePresence } from "motion/react";
import { Loader2 } from "lucide-react";
import { Button, Card, PageHeader } from "../../../shared/components/Primitives";
import { ConfirmDialog } from "../../../shared/components/ConfirmDialog";
import { SettingsSection } from "../../settings/components/SettingsBits";
import { AuditTimeline } from "../../audit/pages/AuditTimeline";
import { toast } from "../../../core/notifications/toast";
import { usersService } from "../services/usersService";
import { useAuth } from "../../../core/auth/AuthContext";
import type { UserStatus } from "../contracts/responses";

export function UserDetailPanel() {
  const navigate = useNavigate();
  const { authUser } = useAuth();
  const email = "ana@byop.com";
  const isSelf = authUser?.email === email;
  const [status, setStatus] = useState<UserStatus>("ativo");
  const [resending, setResending] = useState(false);
  const [confirmBlock, setConfirmBlock] = useState(false);
  const [blocking, setBlocking] = useState(false);
  const [confirmRemove, setConfirmRemove] = useState(false);
  const [removing, setRemoving] = useState(false);
  const [restoring, setRestoring] = useState(false);
  const [isLastActiveAdmin, setIsLastActiveAdmin] = useState(false);

  useEffect(() => {
    usersService.isLastActiveAdmin(email).then(setIsLastActiveAdmin);
  }, []);

  // ADR-0020: nunca trancar o último admin ativo do tenant fora da plataforma.
  const lockedOut = isSelf || isLastActiveAdmin;
  const lockedOutReason = isSelf
    ? "Não é possível remover/bloquear a própria conta."
    : "Não é possível remover o último administrador ativo.";

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
      setStatus("bloqueado");
      toast.success("Usuário bloqueado.", { description: "Ana Martins perdeu acesso à plataforma." });
      setConfirmBlock(false);
    } finally {
      setBlocking(false);
    }
  };

  const handleRemove = async () => {
    setRemoving(true);
    try {
      await usersService.removeUser(email);
      setStatus("removido");
      toast.success("Usuário removido.", { description: "Você pode restaurar o acesso a qualquer momento." });
      setConfirmRemove(false);
    } finally {
      setRemoving(false);
    }
  };

  const handleRestore = async () => {
    setRestoring(true);
    try {
      await usersService.restoreUser(email);
      setStatus("ativo");
      toast.success("Acesso restaurado.", { description: "Um e-mail de redefinição de senha foi enviado ao usuário." });
    } finally {
      setRestoring(false);
    }
  };

  return (
    <>
      <AnimatePresence>
        {confirmBlock && <ConfirmDialog title="Bloquear este usuário?" desc="Ana Martins perderá acesso imediato à plataforma até ser desbloqueada." danger loading={blocking} onConfirm={handleBlock} onCancel={() => setConfirmBlock(false)} />}
        {confirmRemove && <ConfirmDialog title="Remover Ana Martins do tenant?" desc="Ana Martins perderá acesso à plataforma. A remoção não é permanente — você pode restaurar o acesso a qualquer momento." danger loading={removing} onConfirm={handleRemove} onCancel={() => setConfirmRemove(false)} />}
      </AnimatePresence>
      <PageHeader title="Ana Martins" module="Users" desc="Perfil, papéis, produtos, permissões efetivas e atividade recente." badge="Tenant Admin">
        {status === "removido" || status === "bloqueado" ? (
          <Button onClick={handleRestore} disabled={restoring}>{restoring && <Loader2 size={15} className="animate-spin" />}{restoring ? "Restaurando..." : "Restaurar acesso"}</Button>
        ) : (
          <Button onClick={handleResend} disabled={resending}>{resending && <Loader2 size={15} className="animate-spin" />}{resending ? "Reenviando..." : "Reenviar convite"}</Button>
        )}
        {status !== "removido" && (
          <Button onClick={() => setConfirmBlock(true)} disabled={status === "bloqueado" || lockedOut} title={lockedOut ? lockedOutReason : undefined}>
            {status === "bloqueado" ? "Bloqueado" : "Bloquear"}
          </Button>
        )}
        {status !== "removido" && (
          <Button onClick={() => setConfirmRemove(true)} disabled={lockedOut} title={lockedOut ? lockedOutReason : undefined}>
            Remover do tenant
          </Button>
        )}
        <Button primary onClick={() => navigate("/settings/permissions")}>Editar permissões</Button>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
        <div className="space-y-4">
          <Card>
            <div className="flex gap-4">
              <div className="grid h-14 w-14 place-items-center rounded-full bg-primary text-white font-semibold">AM</div>
              <div><h2 className="text-xl font-semibold">Ana Martins</h2><p className="text-sm text-muted-foreground">ana@byop.com · {status} · último acesso há 8 min</p></div>
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
