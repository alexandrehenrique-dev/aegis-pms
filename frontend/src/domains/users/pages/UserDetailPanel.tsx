import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router";
import { AnimatePresence } from "motion/react";
import { Loader2 } from "lucide-react";
import { Button, Card, PageHeader } from "../../../shared/components/Primitives";
import { ConfirmDialog } from "../../../shared/components/ConfirmDialog";
import { SettingsSection } from "../../settings/components/SettingsBits";
import { AuditTimeline } from "../../audit/pages/AuditTimeline";
import { toast } from "../../../core/notifications/toast";
import { usersService } from "../services/usersService";
import { useAuth } from "../../../core/auth/useAuth";
import type { UserStatus, UserSummary } from "../contracts/responses";

function initialsOf(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  const first = parts[0]?.[0] ?? "";
  const last = parts.length > 1 ? parts[parts.length - 1][0] : "";
  return (first + last).toUpperCase() || "U";
}

function permissionsFor(user: UserSummary): string[] {
  const productsLabel = user.products === "Todos" ? "Todos os produtos" : user.products;
  return [
    user.role,
    productsLabel,
    user.status === "ativo" ? "Permissões efetivas completas" : "Permissões suspensas",
    user.inviteStatus === "pendente" ? "Convite pendente" : "Segurança habilitada",
  ];
}

export function UserDetailPanel() {
  const navigate = useNavigate();
  const { id } = useParams();
  const { authUser, effectiveTenant } = useAuth();
  const userId = id ?? "";
  const isSelf = authUser?.id === userId;
  const [user, setUser] = useState<UserSummary | null>(null);
  const [status, setStatus] = useState<UserStatus>("ativo");
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [resending, setResending] = useState(false);
  const [confirmBlock, setConfirmBlock] = useState(false);
  const [blocking, setBlocking] = useState(false);
  const [confirmRemove, setConfirmRemove] = useState(false);
  const [removing, setRemoving] = useState(false);
  const [restoring, setRestoring] = useState(false);
  const [isLastActiveAdmin, setIsLastActiveAdmin] = useState(false);

  useEffect(() => {
    let cancelled = false;
    if (!userId || !effectiveTenant?.id) return;
    setLoading(true);
    setLoadError(null);
    usersService.getUser(userId, effectiveTenant.id)
      .then((loaded) => {
        if (cancelled) return;
        setUser(loaded);
        setStatus(loaded.status);
      })
      .catch(() => {
        if (!cancelled) setLoadError("Não foi possível carregar este usuário.");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => { cancelled = true; };
  }, [effectiveTenant?.id, userId]);

  useEffect(() => {
    if (!userId || !effectiveTenant?.id) return;
    usersService.isLastActiveAdmin(userId, effectiveTenant.id).then(setIsLastActiveAdmin);
  }, [effectiveTenant?.id, userId]);

  // ADR-0020: nunca trancar o último admin ativo do tenant fora da plataforma.
  const lockedOut = isSelf || isLastActiveAdmin;
  const lockedOutReason = isSelf
    ? "Não é possível remover/bloquear a própria conta."
    : "Não é possível remover o último administrador ativo.";

  const handleResend = async () => {
    setResending(true);
    try {
      await usersService.resendInvite(userId, effectiveTenant?.id);
      toast.success("Convite reenviado!", { description: user?.email });
    } finally {
      setResending(false);
    }
  };

  const handleBlock = async () => {
    setBlocking(true);
    try {
      await usersService.blockUser(userId, effectiveTenant?.id);
      setStatus("bloqueado");
      toast.success("Usuário bloqueado.", { description: `${user?.name ?? "Usuário"} perdeu acesso à plataforma.` });
      setConfirmBlock(false);
    } finally {
      setBlocking(false);
    }
  };

  const handleRemove = async () => {
    setRemoving(true);
    try {
      await usersService.removeUser(userId, effectiveTenant?.id);
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
      await usersService.restoreUser(userId, effectiveTenant?.id);
      setStatus("ativo");
      setUser((current) => current ? { ...current, status: "ativo" } : current);
      toast.success("Acesso restaurado.", { description: "Um e-mail de redefinição de senha foi enviado ao usuário." });
    } finally {
      setRestoring(false);
    }
  };

  if (loading) {
    return <div className="grid min-h-[320px] place-items-center text-muted-foreground"><Loader2 className="animate-spin" /></div>;
  }

  if (loadError || !user) {
    return (
      <Card>
        <h2 className="text-lg font-semibold">Usuário não encontrado</h2>
        <p className="mt-1 text-sm text-muted-foreground">{loadError ?? "Verifique o tenant selecionado e tente novamente."}</p>
      </Card>
    );
  }

  const initials = initialsOf(user.name);

  return (
    <>
      <AnimatePresence>
        {confirmBlock && <ConfirmDialog title="Bloquear este usuário?" desc={`${user.name} perderá acesso imediato à plataforma até ser desbloqueado.`} danger loading={blocking} onConfirm={handleBlock} onCancel={() => setConfirmBlock(false)} />}
        {confirmRemove && <ConfirmDialog title={`Remover ${user.name} do tenant?`} desc={`${user.name} perderá acesso à plataforma. A remoção não é permanente — você pode restaurar o acesso a qualquer momento.`} danger loading={removing} onConfirm={handleRemove} onCancel={() => setConfirmRemove(false)} />}
      </AnimatePresence>
      <PageHeader title={user.name} module="Users" desc="Perfil, papéis, produtos, permissões efetivas e atividade recente." badge={user.role}>
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
              <div className="grid h-14 w-14 place-items-center rounded-full bg-primary text-white font-semibold">{initials}</div>
              <div><h2 className="text-xl font-semibold">{user.name}</h2><p className="text-sm text-muted-foreground">{user.email} · {status} · último acesso {user.lastAccess}</p></div>
            </div>
          </Card>
          <SettingsSection title="Papéis e produtos permitidos" items={permissionsFor({ ...user, status })} />
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
