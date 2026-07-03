import { useState } from "react";
import { useNavigate } from "react-router";
import { Loader2 } from "lucide-react";
import { Button, Card, Field, PageHeader } from "../../../shared/components/Primitives";
import { UnsavedChangesBanner } from "../../../shared/components/Banners";
import { ConfirmDialog } from "../../../shared/components/ConfirmDialog";
import { SettingsSection } from "../components/SettingsBits";
import { toast } from "../../../core/notifications/toast";
import { tenantsService } from "../../../core/tenants/services/tenantsService";
import { useAuth } from "../../../core/auth/useAuth";

export function TenantSettings() {
  const navigate = useNavigate();
  const { effectiveTenant } = useAuth();
  const [saving, setSaving] = useState(false);
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [confirmationText, setConfirmationText] = useState("");
  const [deleting, setDeleting] = useState(false);

  const handleSave = async () => {
    setSaving(true);
    try {
      await tenantsService.update("t1", { name: "BYOP", plan: "Pro", status: "ativo" });
      toast.success("Configurações do tenant salvas!");
    } finally {
      setSaving(false);
    }
  };

  /** Mesmo fluxo de exclusão (confirmação por nome digitado) do Super Admin em `TenantSelectScreen.tsx` — aqui o Tenant Admin exclui o próprio tenant. */
  const handleDeleteTenant = async () => {
    if (!effectiveTenant) return;
    setDeleting(true);
    try {
      await tenantsService.remove(effectiveTenant.id, { confirmationText });
      toast.success(`${effectiveTenant.name} foi excluído.`, { description: "Produtos e usuários deste tenant perderam acesso." });
      navigate("/select-tenant");
    } finally {
      setDeleting(false);
    }
  };

  return (
    <>
      <PageHeader title="Tenant Settings" module="Configurações" desc="Configurações do tenant BYOP: identidade, governança e limites futuros." badge="Tenant BYOP">
        <Button onClick={() => navigate(-1)}>Cancelar</Button>
        <Button primary onClick={handleSave} disabled={saving}>{saving && <Loader2 size={15} className="animate-spin" />}{saving ? "Salvando..." : "Salvar tenant"}</Button>
      </PageHeader>
      <UnsavedChangesBanner />
      <div className="grid gap-4">
        <SettingsSection title="Dados gerais" items={["Nome", "Slug", "Descrição", "Status", "Idioma padrão"]} />
        <SettingsSection title="Branding do tenant" items={["Logo", "Cor institucional", "Assinatura visual"]} />
        <SettingsSection title="Governança" items={["Política de convites", "Regras de acesso", "Retenção futura", "Auditoria obrigatória"]} />
        <SettingsSection title="Limites futuros" items={["Produtos", "Usuários", "Storage", "Formulários"]} />
        <Card data-tour="settings-perigo" className="border-destructive/30">
          <h2 className="mb-1 text-lg font-semibold text-destructive">Zona de perigo</h2>
          <p className="mb-3 text-sm text-muted-foreground">Excluir este tenant é irreversível: todos os produtos e o acesso de todos os usuários associados são removidos imediatamente.</p>
          <Button onClick={() => setConfirmingDelete(true)} disabled={!effectiveTenant} className="border-destructive text-destructive hover:bg-destructive/10">Excluir tenant</Button>
        </Card>
      </div>
      {confirmingDelete && effectiveTenant && (
        <ConfirmDialog
          title={`Excluir ${effectiveTenant.name}?`}
          desc="Esta ação é irreversível. Digite o nome do tenant para confirmar."
          danger
          loading={deleting}
          confirmDisabled={confirmationText.trim() !== effectiveTenant.name}
          onCancel={() => { setConfirmingDelete(false); setConfirmationText(""); }}
          onConfirm={handleDeleteTenant}
        >
          <Field label={`Digite "${effectiveTenant.name}" para confirmar`} value={confirmationText} onChange={setConfirmationText} />
        </ConfirmDialog>
      )}
    </>
  );
}
