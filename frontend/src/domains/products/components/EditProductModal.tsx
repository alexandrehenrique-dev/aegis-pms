import { useState } from "react";
import { motion } from "motion/react";
import { Loader2, X } from "lucide-react";
import { Button, Field, SelectLike, fade } from "../../../shared/components/Primitives";
import { PRODUCT_TYPES, type ProductTypeKey } from "../../../core/products/moduleDefaults";
import { useModuleSelection } from "../hooks/useModuleSelection";
import { ModuleCheckboxList } from "./ModuleCheckboxList";
import type { ProductStatus } from "../../../shared/types";
import type { EditableProduct } from "../contracts/responses";
import type { UpdateProductRequest } from "../contracts/requests";

const STATUSES: ProductStatus[] = ["Ativo", "Pendente", "Arquivado", "Sem módulos"];

/**
 * Editar Produto (docs/implementation/004_aegis_pms_screen_inventory.md,
 * 05.04) — nome, tipo, status e módulos. A seleção de módulos é
 * exatamente a mesma funcionalidade da criação (`CreateProductForm` via
 * `useModuleSelection`/`ModuleCheckboxList`), não uma versão reduzida.
 * `onSave` é injetado pelo chamador porque este modal é compartilhado
 * entre `ProductsList`/`ProductCard` (productsService) e
 * `ProductSelectScreen` (mutação via AuthContext) — ver `useProductActions`.
 */
export function EditProductModal({ product, onClose, onSave }: { product: EditableProduct; onClose: () => void; onSave: (req: UpdateProductRequest) => Promise<void> }) {
  const [name, setName] = useState(product.name);
  const [status, setStatus] = useState<ProductStatus>(product.status);
  const { type, setType, moduleOptions, selectedModules, toggleModule, selectedList } = useModuleSelection(product.type as ProductTypeKey, product.modulesList);
  const [saving, setSaving] = useState(false);

  const handleSave = async () => {
    setSaving(true);
    try {
      await onSave({ name, type, status, modules: selectedList });
      onClose();
    } finally {
      setSaving(false);
    }
  };

  return (
    <motion.div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30 backdrop-blur-[3px] p-4" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={onClose}>
      <motion.div {...fade} className="w-full max-w-lg rounded-2xl border border-border bg-card p-6 shadow-[0_24px_80px_rgba(0,0,0,0.2)]" onClick={(e) => e.stopPropagation()}>
        <div className="mb-4 flex items-start justify-between">
          <h2 className="font-semibold">Editar {product.name}</h2>
          <button onClick={onClose} className="rounded-lg p-1 transition hover:bg-muted"><X size={17} /></button>
        </div>
        <div className="space-y-3">
          <div className="grid gap-3 md:grid-cols-2">
            <Field label="Nome do produto" value={name} onChange={setName} />
            <SelectLike label="Tipo" value={type} options={PRODUCT_TYPES} onChange={setType} />
          </div>
          <div>
            <p className="mb-1 text-sm font-medium">Status</p>
            <div className="flex flex-wrap gap-1.5">
              {STATUSES.map((s) => (
                <button key={s} onClick={() => setStatus(s)} className={`rounded-xl border px-3 py-1.5 text-sm transition ${status === s ? "border-primary bg-primary/5 text-primary font-medium" : "border-border bg-card text-muted-foreground hover:bg-muted"}`}>{s}</button>
              ))}
            </div>
          </div>
          <div>
            <p className="mb-1 text-sm font-medium">Módulos</p>
            <p className="mb-2 text-xs text-muted-foreground">Mesma seleção da criação — desmarque/marque antes de salvar. Trocar o tipo reaplica os defaults daquele tipo.</p>
            <ModuleCheckboxList options={moduleOptions} selected={selectedModules} onToggle={toggleModule} />
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
