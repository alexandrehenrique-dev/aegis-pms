import { useState } from "react";
import { motion } from "motion/react";
import { Loader2, X } from "lucide-react";
import { Button, Field, fade } from "../../../shared/components/Primitives";
import { tenantsService } from "../../../core/tenants/services/tenantsService";
import { toast } from "../../../core/notifications/toast";
import type { TenantOption } from "../../../shared/types";

export function EditTenantModal({ tenant, onClose, onSaved }: { tenant: TenantOption; onClose: () => void; onSaved: () => void }) {
  const [name, setName] = useState(tenant.name);
  const [plan, setPlan] = useState(tenant.plan);
  const [status, setStatus] = useState<"ativo" | "suspenso">(tenant.status);
  const [saving, setSaving] = useState(false);

  const handleSave = async () => {
    setSaving(true);
    try {
      await tenantsService.update(tenant.id, { name, plan, status });
      toast.success("Tenant atualizado com sucesso!");
      onSaved();
    } finally {
      setSaving(false);
    }
  };

  return (
    <motion.div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30 backdrop-blur-[3px] p-4" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={onClose}>
      <motion.div {...fade} className="w-full max-w-sm rounded-2xl border border-border bg-card p-6 shadow-[0_24px_80px_rgba(0,0,0,0.2)]" onClick={(e) => e.stopPropagation()}>
        <div className="mb-4 flex items-start justify-between">
          <h2 className="font-semibold">Editar {tenant.name}</h2>
          <button onClick={onClose} className="rounded-lg p-1 transition hover:bg-muted"><X size={17} /></button>
        </div>
        <div className="space-y-3">
          <Field label="Nome do tenant" value={name} onChange={setName} />
          <Field label="Plano / tier" value={plan} onChange={setPlan} />
          <div>
            <p className="mb-1 text-sm font-medium">Status</p>
            <div className="flex gap-1.5">
              {(["ativo", "suspenso"] as const).map((s) => (
                <button key={s} onClick={() => setStatus(s)} className={`rounded-xl border px-3 py-1.5 text-sm capitalize transition ${status === s ? "border-primary bg-primary/5 text-primary font-medium" : "border-border bg-card text-muted-foreground hover:bg-muted"}`}>{s}</button>
              ))}
            </div>
          </div>
        </div>
        <div className="mt-5 flex justify-end gap-2">
          <Button onClick={onClose}>Cancelar</Button>
          <Button primary onClick={handleSave} disabled={saving || !name.trim()}>{saving && <Loader2 size={15} className="animate-spin" />}{saving ? "Salvando..." : "Salvar alterações"}</Button>
        </div>
      </motion.div>
    </motion.div>
  );
}
