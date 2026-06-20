import { useState, type MouseEvent } from "react";
import { useNavigate } from "react-router";
import { motion } from "motion/react";
import { Boxes, Clock3, LogOut, MoreVertical, Plus } from "lucide-react";
import { useAuth } from "../AuthContext";
import { roleLabels } from "../../permissions/roles";
import { tenantsService } from "../../tenants/services/tenantsService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { AegisLogo } from "../../../shared/components/AegisLogo";
import { Badge, Button, EmptyState, Field, fade } from "../../../shared/components/Primitives";
import { ConfirmDialog } from "../../../shared/components/ConfirmDialog";
import { MobileDrawerMenu } from "../../../shared/components/MobileDrawerMenu";
import { toast } from "../../notifications/toast";
// domains/tenants são consumidos aqui mesmo vivendo em core/auth: a gestão de
// tenants do Super Admin reaproveita esta tela (não existe /admin/tenants
// separado) e o wizard de onboarding inerentemente cruza para os domínios de
// produtos/usuários — ver CreateTenantWizardModal.
import { CreateTenantWizardModal } from "../../../domains/tenants/components/CreateTenantWizardModal";
import { EditTenantModal } from "../../../domains/tenants/components/EditTenantModal";
import { TenantContextMenu, type ContextMenuTarget } from "../../../domains/tenants/components/TenantContextMenu";
import type { TenantOption } from "../../../shared/types";

export function TenantSelectScreen() {
  const navigate = useNavigate();
  const { authUser, userTenants, selectTenant, logout } = useAuth();
  const [q, setQ] = useState("");
  const isSuperAdmin = authUser?.role === "super_admin";

  const [reloadKey, setReloadKey] = useState(0);
  const refresh = () => setReloadKey((k) => k + 1);
  const { data: allTenants } = useAsyncData(() => tenantsService.listTenants(), [reloadKey]);

  const [showCreateWizard, setShowCreateWizard] = useState(false);
  const [editingTenant, setEditingTenant] = useState<TenantOption | null>(null);
  const [pendingDelete, setPendingDelete] = useState<TenantOption | null>(null);
  const [confirmationText, setConfirmationText] = useState("");
  const [deleting, setDeleting] = useState(false);
  const [contextMenu, setContextMenu] = useState<{ tenant: TenantOption; position: ContextMenuTarget } | null>(null);

  if (!authUser) return null;

  // Super Admin gerencia a plataforma inteira (tenantsService); demais
  // papéis veem só os tenants aos quais já têm acesso (userTenants do login).
  const tenants = isSuperAdmin ? (allTenants ?? []) : userTenants;
  const filtered = tenants.filter((t) => t.name.toLowerCase().includes(q.toLowerCase()));

  const handleSelect = (t: TenantOption) => {
    if (t.status === "suspenso") return;
    selectTenant(t);
    navigate("/select-product");
  };

  const handleLogout = () => { logout(); navigate("/login"); };

  // Reaproveitado pelo botão direito (desktop) e pelo "⋮" visível em mobile
  // (botão direito/long-press não é confiável em touch) — ver ContextActionMenu.
  const openContextMenu = (t: TenantOption, e: MouseEvent) => {
    if (!isSuperAdmin) return;
    e.preventDefault();
    setContextMenu({ tenant: t, position: { x: e.clientX, y: e.clientY } });
  };

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
    <div className="min-h-screen bg-background">
      <header className="flex items-center justify-between border-b border-border bg-card px-6 py-4 shadow-[0_1px_0_rgba(0,0,0,0.06)] dark:shadow-none">
        <div className="flex items-center gap-2.5"><AegisLogo size="sm" /><span className="font-semibold tracking-[-.02em]">Aegis</span><Badge tone="violet">Product OS</Badge></div>
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2 rounded-xl border border-border bg-muted/40 px-3 py-1.5 text-sm">
            <div className="grid h-6 w-6 place-items-center rounded-full bg-primary text-primary-foreground text-xs font-semibold">{authUser.initials}</div>
            <span className="text-muted-foreground hidden sm:block">{authUser.name}</span>
            <Badge tone="violet">{roleLabels[authUser.role]}</Badge>
          </div>
          <button onClick={handleLogout} className="flex items-center gap-1.5 rounded-xl border border-border bg-card px-3 py-1.5 text-sm transition hover:bg-muted"><LogOut size={14} />Sair</button>
        </div>
      </header>
      <main className="mx-auto max-w-3xl px-4 py-12">
        <motion.div {...fade}>
          <div className="flex flex-wrap items-end justify-between gap-3">
            <div>
              <h1 className="text-2xl font-semibold tracking-[-.02em]">{isSuperAdmin ? "Gestão de Tenants" : "Selecione seu espaço de trabalho"}</h1>
              <p className="mt-1 text-sm text-muted-foreground">
                {isSuperAdmin ? "Todos os tenants da plataforma. Botão direito sobre um tenant para editar ou excluir." : `Você tem acesso a ${userTenants.length} tenant${userTenants.length !== 1 ? "s" : ""}.`}
              </p>
            </div>
            {isSuperAdmin && (
              <div className="hidden lg:block"><Button primary onClick={() => setShowCreateWizard(true)}><Plus size={15} />Criar Tenant</Button></div>
            )}
          </div>
          <div className="mt-6 flex flex-wrap items-center gap-2">
            <div className="flex min-w-[200px] flex-1 items-center gap-2 rounded-xl border border-border bg-card px-3 py-2.5">
              <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Buscar tenant..." className="w-full bg-transparent text-sm outline-none" />
            </div>
            {isSuperAdmin && (
              <div className="lg:hidden">
                <MobileDrawerMenu label="Ações de tenants" title="Ações">
                  <Button primary onClick={() => setShowCreateWizard(true)} className="w-full"><Plus size={15} />Criar Tenant</Button>
                </MobileDrawerMenu>
              </div>
            )}
          </div>
          <div className="mt-4 grid gap-3 md:grid-cols-2">
            {filtered.length === 0 ? <EmptyState compact title="Nenhum tenant encontrado" description="Ajuste a busca." /> : filtered.map((t) => (
              <div key={t.id} className="relative">
                <button
                  onClick={() => handleSelect(t)}
                  onContextMenu={(e) => openContextMenu(t, e)}
                  disabled={t.status === "suspenso"}
                  className="group w-full rounded-2xl border border-border bg-card p-5 text-left transition hover:border-primary hover:shadow-[0_8px_30px_rgba(15,61,46,.08)] disabled:opacity-50"
                >
                  <div className="flex items-start justify-between">
                    <div className="flex items-center gap-3">
                      <div className="grid h-10 w-10 place-items-center rounded-xl bg-primary/10 font-semibold text-primary">{t.name.charAt(0)}</div>
                      <div><p className="font-semibold">{t.name}</p><p className="text-xs text-muted-foreground">{t.plan}</p></div>
                    </div>
                    <Badge tone={t.status === "ativo" ? "green" : "red"}>{t.status}</Badge>
                  </div>
                  <div className="mt-4 grid grid-cols-2 gap-2 text-xs text-muted-foreground">
                    <span className="flex items-center gap-1"><Boxes size={12} />{t.productCount} produto{t.productCount !== 1 ? "s" : ""}</span>
                    <span className="flex items-center gap-1"><Clock3 size={12} />{t.lastAccess}</span>
                  </div>
                  {t.status === "suspenso" ? <p className="mt-3 text-xs text-destructive">Tenant suspenso. Contate o suporte.</p> : <div className="mt-4 flex justify-end"><span className="text-sm text-primary opacity-0 transition group-hover:opacity-100">Entrar →</span></div>}
                </button>
                {/* Botão direito não é confiável em touch — em mobile, este "⋮" visível abre o mesmo menu. Fica fora do <button> do card para não criar botão-dentro-de-botão. */}
                {isSuperAdmin && (
                  <button onClick={(e) => openContextMenu(t, e)} aria-label={`Ações de ${t.name}`} className="absolute right-2 top-2 rounded-lg p-1.5 text-muted-foreground transition hover:bg-muted lg:hidden">
                    <MoreVertical size={16} />
                  </button>
                )}
              </div>
            ))}
          </div>
        </motion.div>
      </main>

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
          desc={`Esta ação é irreversível. Todos os ${pendingDelete.productCount} produto${pendingDelete.productCount !== 1 ? "s" : ""} deste tenant e o acesso de todos os usuários associados a eles serão removidos imediatamente. Digite o nome do tenant para confirmar.`}
          danger
          loading={deleting}
          confirmDisabled={confirmationText.trim() !== pendingDelete.name}
          onCancel={() => { setPendingDelete(null); setConfirmationText(""); }}
          onConfirm={handleDelete}
        >
          <Field label={`Digite "${pendingDelete.name}" para confirmar`} value={confirmationText} onChange={setConfirmationText} />
        </ConfirmDialog>
      )}
    </div>
  );
}
