import { useState } from "react";
import { motion } from "motion/react";
import { Loader2, X } from "lucide-react";
import { Button, Field, fade } from "../../../shared/components/Primitives";
import { productsService } from "../services/productsService";
import { toast } from "../../../core/notifications/toast";
import type { ProductStatus } from "../../../shared/types";
import type { ProductSummary } from "../contracts/responses";

const STATUSES: ProductStatus[] = ["Ativo", "Pendente", "Arquivado", "Sem módulos"];

/** Editar Produto (docs/implementation/004_aegis_pms_screen_inventory.md, 05.04) — nome, tipo e status. Acesso restrito a Tenant Admin/Super Admin (ver gating em ProductCard/ProductsList, espelhando o bloqueio de /products/new em core/permissions/roles.ts). */
export function EditProductModal({ product, onClose, onSaved }: { product: ProductSummary; onClose: () => void; onSaved: () => void }) {
  const [name, setName] = useState(product.name);
  const [type, setType] = useState(product.type);
  const [status, setStatus] = useState<ProductStatus>(product.status);
  const [saving, setSaving] = useState(false);

  const handleSave = async () => {
    setSaving(true);
    try {
      await productsService.update(product.id ?? product.name, { name, type, status });
      toast.success("Produto atualizado com sucesso!");
      onSaved();
    } finally {
      setSaving(false);
    }
  };

  return (
    <motion.div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30 backdrop-blur-[3px] p-4" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={onClose}>
      <motion.div {...fade} className="w-full max-w-sm rounded-2xl border border-border bg-card p-6 shadow-[0_24px_80px_rgba(0,0,0,0.2)]" onClick={(e) => e.stopPropagation()}>
        <div className="mb-4 flex items-start justify-between">
          <h2 className="font-semibold">Editar {product.name}</h2>
          <button onClick={onClose} className="rounded-lg p-1 transition hover:bg-muted"><X size={17} /></button>
        </div>
        <div className="space-y-3">
          <Field label="Nome do produto" value={name} onChange={setName} />
          <Field label="Tipo" value={type} onChange={setType} />
          <div>
            <p className="mb-1 text-sm font-medium">Status</p>
            <div className="flex flex-wrap gap-1.5">
              {STATUSES.map((s) => (
                <button key={s} onClick={() => setStatus(s)} className={`rounded-xl border px-3 py-1.5 text-sm transition ${status === s ? "border-primary bg-primary/5 text-primary font-medium" : "border-border bg-card text-muted-foreground hover:bg-muted"}`}>{s}</button>
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
