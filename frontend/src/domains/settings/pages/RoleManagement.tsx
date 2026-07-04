import { useState } from "react";
import { AnimatePresence } from "motion/react";
import { Loader2 } from "lucide-react";
import { Button, Card, Field, PageHeader } from "../../../shared/components/Primitives";
import { ConfirmDialog } from "../../../shared/components/ConfirmDialog";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "../../../shared/components/ui/dialog";
import { RiskBadge } from "../../../shared/components/RiskBadge";
import { PermissionImpactSummary } from "../components/PermissionBits";
import { toast } from "../../../core/notifications/toast";
import { settingsService } from "../services/settingsService";
import { useAuth } from "../../../core/auth/useAuth";

const DEFAULT_ROLES = [
  ["Super Admin", "Acesso global ao sistema", "1", "alto", "30 dias"],
  ["Tenant Admin", "Administra tenant e equipe", "2", "alto", "2 dias"],
  ["Product Manager", "Opera produto e aprova fluxos", "4", "médio", "ontem"],
  ["Editor", "Cria e revisa conteúdo", "8", "médio", "2 dias"],
  ["Viewer", "Somente leitura", "6", "baixo", "padrão"],
];

function RoleCard({ r, onEdit, onImpact }: { r: string[]; onEdit: () => void; onImpact: () => void }) {
  return (
    <Card>
      <div className="flex justify-between"><h3 className="font-semibold">{r[0]}</h3><RiskBadge risk={r[3]} /></div>
      <p className="mt-2 text-sm text-muted-foreground">{r[1]}</p>
      <p className="mt-3 text-sm">Usuários: <b>{r[2]}</b></p>
      <p className="text-xs text-muted-foreground">Última alteração: {r[4]}</p>
      <div className="mt-4 flex gap-2"><Button onClick={onEdit}>Editar</Button><Button onClick={onImpact}>Impacto</Button></div>
    </Card>
  );
}

export function RoleManagement() {
  const { effectiveTenant } = useAuth();
  const [roles, setRoles] = useState(DEFAULT_ROLES);
  const [confirmRestore, setConfirmRestore] = useState(false);
  const [restoring, setRestoring] = useState(false);
  const [editingRole, setEditingRole] = useState<string[] | null>(null);
  const [editDescription, setEditDescription] = useState("");
  const [impactRole, setImpactRole] = useState<string | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [newRoleName, setNewRoleName] = useState("");
  const [newRoleDesc, setNewRoleDesc] = useState("");
  const [creating, setCreating] = useState(false);

  const handleRestore = async () => {
    setRestoring(true);
    try {
      await settingsService.restoreDefaultRoles(effectiveTenant?.id);
      setRoles(DEFAULT_ROLES);
      toast.success("Roles restauradas ao padrão.");
      setConfirmRestore(false);
    } finally {
      setRestoring(false);
    }
  };

  const handleSaveEdit = () => {
    if (!editingRole) return;
    setRoles((prev) => prev.map((r) => (r[0] === editingRole[0] ? [r[0], editDescription, r[2], r[3], r[4]] : r)));
    toast.success("Role atualizada!", { description: editingRole[0] });
    setEditingRole(null);
  };

  const handleCreateRole = async () => {
    setCreating(true);
    try {
      await settingsService.createRole({ name: newRoleName, description: newRoleDesc });
      setRoles((prev) => [...prev, [newRoleName, newRoleDesc, "0", "baixo", "agora"]]);
      toast.success("Role criada!", { description: newRoleName });
      setCreateOpen(false);
      setNewRoleName("");
      setNewRoleDesc("");
    } finally {
      setCreating(false);
    }
  };

  return (
    <>
      <AnimatePresence>
        {confirmRestore && <ConfirmDialog title="Restaurar roles padrão?" desc="Roles customizadas criadas nesta sessão serão removidas." danger loading={restoring} onConfirm={handleRestore} onCancel={() => setConfirmRestore(false)} />}
      </AnimatePresence>
      <Dialog open={editingRole !== null} onOpenChange={(open) => !open && setEditingRole(null)}>
        <DialogContent>
          <DialogHeader><DialogTitle>Editar {editingRole?.[0]}</DialogTitle></DialogHeader>
          <Field label="Descrição" value={editDescription} onChange={setEditDescription} textarea />
          <DialogFooter>
            <Button onClick={() => setEditingRole(null)}>Cancelar</Button>
            <Button primary onClick={handleSaveEdit}>Salvar</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
      <Dialog open={impactRole !== null} onOpenChange={(open) => !open && setImpactRole(null)}>
        <DialogContent>
          <DialogHeader><DialogTitle>Impacto de {impactRole}</DialogTitle></DialogHeader>
          <PermissionImpactSummary />
        </DialogContent>
      </Dialog>
      <Dialog open={createOpen} onOpenChange={setCreateOpen}>
        <DialogContent>
          <DialogHeader><DialogTitle>Criar role</DialogTitle></DialogHeader>
          <Field label="Nome" value={newRoleName} onChange={setNewRoleName} />
          <Field label="Descrição" value={newRoleDesc} onChange={setNewRoleDesc} textarea />
          <DialogFooter>
            <Button onClick={() => setCreateOpen(false)}>Cancelar</Button>
            <Button primary onClick={handleCreateRole} disabled={creating || !newRoleName}>{creating && <Loader2 size={15} className="animate-spin" />}{creating ? "Criando..." : "Criar"}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
      <PageHeader title="Role Management" module="Permissions" desc="Gerencie papéis, risco e permissões principais." badge="Roles">
        <Button onClick={() => setConfirmRestore(true)}>Restaurar padrão</Button>
        <Button primary onClick={() => setCreateOpen(true)}>Criar role</Button>
      </PageHeader>
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
        {roles.map((r) => (
          <RoleCard key={r[0]} r={r} onEdit={() => { setEditingRole(r); setEditDescription(r[1]); }} onImpact={() => setImpactRole(r[0])} />
        ))}
      </div>
    </>
  );
}
