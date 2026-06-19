import { useState } from "react";
import { useNavigate } from "react-router";
import { Pencil, Plus, Trash2 } from "lucide-react";
import { Badge, Button, Card, Field, PageHeader, PartialErrorWidget, SkeletonLines } from "../../../shared/components/Primitives";
import { ConfirmDialog } from "../../../shared/components/ConfirmDialog";
import { BackLink } from "../../../shared/components/BackLink";
import { CreateTenantWizardModal } from "../components/CreateTenantWizardModal";
import { EditTenantModal } from "../components/EditTenantModal";
import { TenantContextMenu, type ContextMenuTarget } from "../components/TenantContextMenu";
import { tenantsService } from "../../../core/tenants/services/tenantsService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { toast } from "../../../core/notifications/toast";
import type { TenantOption } from "../../../shared/types";

export function TenantsManagement() {
  const navigate = useNavigate();
  const [reloadKey, setReloadKey] = useState(0);
  const { data: tenants, loading, error } = useAsyncData(() => tenantsService.listTenants(), [reloadKey]);
  const refresh = () => setReloadKey((k) => k + 1);

  const [showCreateWizard, setShowCreateWizard] = useState(false);
  const [editingTenant, setEditingTenant] = useState<TenantOption | null>(null);
  const [pendingDelete, setPendingDelete] = useState<TenantOption | null>(null);
  const [confirmationText, setConfirmationText] = useState("");
  const [deleting, setDeleting] = useState(false);
  const [contextMenu, setContextMenu] = useState<{ tenant: TenantOption; position: ContextMenuTarget } | null>(null);

  const handleDelete = async () => {
    if (!pendingDelete) return;
    setDeleting(true);
    try {
      await tenantsService.remove(pendingDelete.id, { confirmationText });
      toast.success(`${pendingDelete.name} foi excluído.`, { description: "Produtos e usuários deste tenant perderam acesso." });
      setPendingDelete(null);
      setConfirmationText("");
      refresh();
    } finally {
      setDeleting(false);
    }
  };

  return (
    <>
      <BackLink to="/dashboard" label="Voltar ao Dashboard" />
      <PageHeader title="Gestão de Tenants" desc="Tela central do Super Admin: todos os tenants da plataforma. Botão direito sobre um tenant para editar ou excluir." badge="Super Admin">
        <Button primary onClick={() => setShowCreateWizard(true)}><Plus size={15} />Criar Tenant</Button>
      </PageHeader>
      {loading ? (
        <SkeletonLines />
      ) : error || !tenants ? (
        <PartialErrorWidget />
      ) : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {tenants.map((t) => (
            <Card key={t.id} onContextMenu={(e) => { e.preventDefault(); setContextMenu({ tenant: t, position: { x: e.clientX, y: e.clientY } }); }}>
              <div className="flex items-start justify-between gap-3">
                <div><h3 className="font-semibold">{t.name}</h3><p className="text-sm text-muted-foreground">Plano {t.plan}</p></div>
                <Badge tone={t.status === "ativo" ? "green" : "red"}>{t.status}</Badge>
              </div>
              <div className="mt-4 grid grid-cols-2 gap-2 text-sm">
                <div className="rounded-lg bg-muted p-3"><p className="text-xs text-muted-foreground">Produtos</p><p className="font-semibold">{t.productCount}</p></div>
                <div className="rounded-lg bg-muted p-3"><p className="text-xs text-muted-foreground">Último acesso</p><p className="truncate font-semibold">{t.lastAccess}</p></div>
              </div>
              <div className="mt-4 flex flex-wrap gap-2">
                <Button primary onClick={() => navigate(`/products/new?tenantId=${t.id}`)}><Plus size={15} />Criar produto</Button>
                <Button onClick={() => setEditingTenant(t)}><Pencil size={14} />Editar</Button>
                <button onClick={() => setPendingDelete(t)} className="inline-flex items-center gap-2 rounded-lg border border-destructive/30 px-3 py-2 text-sm text-destructive transition hover:bg-destructive/10 active:scale-[0.97]"><Trash2 size={14} />Excluir</button>
              </div>
            </Card>
          ))}
        </div>
      )}

      {contextMenu && (
        <TenantContextMenu
          position={contextMenu.position}
          onEdit={() => { setEditingTenant(contextMenu.tenant); setContextMenu(null); }}
          onDelete={() => { setPendingDelete(contextMenu.tenant); setContextMenu(null); }}
          onClose={() => setContextMenu(null)}
        />
      )}

      {showCreateWizard && (
        <CreateTenantWizardModal onClose={() => setShowCreateWizard(false)} onDone={refresh} />
      )}

      {editingTenant && (
        <EditTenantModal tenant={editingTenant} onClose={() => setEditingTenant(null)} onSaved={() => { setEditingTenant(null); refresh(); }} />
      )}

      {pendingDelete && (
        <ConfirmDialog
          title={`Excluir ${pendingDelete.name}?`}
          desc={`Esta ação é irreversível. Todos os ${pendingDelete.productCount} produtos deste tenant e o acesso de todos os usuários associados a eles serão removidos imediatamente. Digite o nome do tenant para confirmar.`}
          danger
          loading={deleting}
          confirmDisabled={confirmationText.trim() !== pendingDelete.name}
          onCancel={() => { setPendingDelete(null); setConfirmationText(""); }}
          onConfirm={handleDelete}
        >
          <Field label={`Digite "${pendingDelete.name}" para confirmar`} value={confirmationText} onChange={setConfirmationText} />
        </ConfirmDialog>
      )}
    </>
  );
}
