import { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router";
import { Loader2 } from "lucide-react";
import { Button, Card, Field, PageHeader, SelectLike } from "../../../shared/components/Primitives";
import { UnsavedChangesBanner } from "../../../shared/components/Banners";
import { ConfirmDialog } from "../../../shared/components/ConfirmDialog";
import { toast } from "../../../core/notifications/toast";
import { tenantsService } from "../../../core/tenants/services/tenantsService";
import { useAuth } from "../../../core/auth/useAuth";

const PLANS = ["Starter", "Pro", "Enterprise"];
const STATUSES = [
  { value: "ativo", label: "Ativo", desc: "Tenant operacional — todos os produtos e usuários acessíveis." },
  { value: "suspenso", label: "Suspenso", desc: "Acesso bloqueado para todos os usuários do tenant." },
] as const;

type TenantStatus = "ativo" | "suspenso";

export function TenantSettings() {
  const navigate = useNavigate();
  const { effectiveTenant } = useAuth();

  const [name, setName] = useState(effectiveTenant?.name ?? "");
  const [plan, setPlan] = useState(effectiveTenant?.plan ?? "Pro");
  const [status, setStatus] = useState<TenantStatus>((effectiveTenant?.status as TenantStatus) ?? "ativo");
  const [saving, setSaving] = useState(false);
  const [dirty, setDirty] = useState(false);
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [confirmationText, setConfirmationText] = useState("");
  const [deleting, setDeleting] = useState(false);

  // Sincroniza quando o tenant muda. Guarda por id em vez de depender do objeto
  // inteiro para não resetar os campos em edição a cada atualização otimista
  // de `effectiveTenant`.
  const syncedTenantId = useRef<string | undefined>(undefined);
  useEffect(() => {
    if (!effectiveTenant || effectiveTenant.id === syncedTenantId.current) return;
    syncedTenantId.current = effectiveTenant.id;
    setName(effectiveTenant.name);
    setPlan(effectiveTenant.plan);
    setStatus((effectiveTenant.status as TenantStatus) ?? "ativo");
    setDirty(false);
  }, [effectiveTenant]);

  const markDirty = <T,>(setter: (v: T) => void) => (v: T) => { setter(v); setDirty(true); };

  const handleSave = async () => {
    if (!effectiveTenant) return;
    setSaving(true);
    try {
      await tenantsService.update(effectiveTenant.id, { name, plan, status });
      setDirty(false);
      toast.success("Tenant atualizado!", { description: `${name} foi salvo com sucesso.` });
    } catch {
      toast.error("Erro ao salvar tenant", { description: "Verifique sua conexão e tente novamente." });
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteTenant = async () => {
    if (!effectiveTenant) return;
    setDeleting(true);
    try {
      await tenantsService.remove(effectiveTenant.id, { confirmationText });
      toast.success(`${effectiveTenant.name} foi excluído.`, {
        description: "Produtos e usuários deste tenant perderam acesso.",
      });
      navigate("/select-tenant");
    } catch {
      toast.error("Erro ao excluir tenant");
    } finally {
      setDeleting(false);
    }
  };

  if (!effectiveTenant) {
    return (
      <div className="rounded-xl border border-border bg-card p-8 text-center text-muted-foreground">
        Nenhum tenant selecionado.
      </div>
    );
  }

  return (
    <>
      <PageHeader
        title="Configurações do tenant"
        module="Configurações"
        desc={`Identidade, plano e governança de "${effectiveTenant.name}".`}
        badge="Tenant"
      >
        <Button onClick={() => { setDirty(false); navigate(-1); }}>Cancelar</Button>
        <Button primary onClick={handleSave} disabled={saving || !dirty}>
          {saving && <Loader2 size={15} className="animate-spin" />}
          {saving ? "Salvando..." : "Salvar tenant"}
        </Button>
      </PageHeader>

      {dirty && <UnsavedChangesBanner />}

      <div className="grid gap-4">
        {/* Dados gerais */}
        <Card>
          <h2 className="mb-4 text-lg font-semibold">Dados gerais</h2>
          <div className="grid gap-4 md:grid-cols-2">
            <Field
              label="Nome do tenant"
              value={name}
              onChange={markDirty(setName)}
            />
            <Field
              label="Identificador (slug)"
              value={effectiveTenant.id}
              locked
            />
            <SelectLike
              label="Plano"
              value={plan}
              options={PLANS}
              onChange={markDirty(setPlan)}
            />
            <div>
              <p className="mb-1 text-sm font-medium">Produtos</p>
              <div className="rounded-xl border border-border bg-muted px-3 py-2.5 text-sm text-muted-foreground">
                {effectiveTenant.productCount} produto{effectiveTenant.productCount !== 1 ? "s" : ""} cadastrado{effectiveTenant.productCount !== 1 ? "s" : ""}
              </div>
            </div>
          </div>
        </Card>

        {/* Status */}
        <Card>
          <h2 className="mb-3 text-lg font-semibold">Status do tenant</h2>
          <div className="space-y-2">
            {STATUSES.map((s) => (
              <button
                key={s.value}
                onClick={() => markDirty(setStatus)(s.value)}
                className={`flex w-full items-start gap-3 rounded-xl border p-3 text-left transition ${
                  status === s.value
                    ? "border-primary bg-primary/5"
                    : "border-border hover:bg-muted"
                }`}
              >
                <span className={`mt-0.5 h-4 w-4 shrink-0 rounded-full border-2 ${status === s.value ? "border-primary bg-primary" : "border-muted-foreground"}`} />
                <span>
                  <span className="font-medium">{s.label}</span>
                  <span className="mt-0.5 block text-xs text-muted-foreground">{s.desc}</span>
                </span>
              </button>
            ))}
          </div>
        </Card>

        {/* Zona de perigo */}
        <Card className="border-destructive/30">
          <h2 className="mb-1 text-lg font-semibold text-destructive">Zona de perigo</h2>
          <p className="mb-3 text-sm text-muted-foreground">
            Excluir este tenant é <strong>irreversível</strong>: todos os produtos, conteúdos,
            assets e o acesso de todos os usuários associados são removidos imediatamente.
          </p>
          <Button
            onClick={() => setConfirmingDelete(true)}
            className="border-destructive text-destructive hover:bg-destructive/10"
          >
            Excluir tenant
          </Button>
        </Card>
      </div>

      {confirmingDelete && (
        <ConfirmDialog
          title={`Excluir ${effectiveTenant.name}?`}
          desc="Esta ação é irreversível. Digite o nome do tenant para confirmar."
          danger
          loading={deleting}
          confirmDisabled={confirmationText.trim() !== effectiveTenant.name}
          onCancel={() => { setConfirmingDelete(false); setConfirmationText(""); }}
          onConfirm={handleDeleteTenant}
        >
          <Field
            label={`Digite "${effectiveTenant.name}" para confirmar`}
            value={confirmationText}
            onChange={setConfirmationText}
          />
        </ConfirmDialog>
      )}
    </>
  );
}
