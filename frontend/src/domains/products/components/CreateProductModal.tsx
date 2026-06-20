import { useState } from "react";
import { motion } from "motion/react";
import { CheckCircle2, Loader2, X } from "lucide-react";
import { Badge, Button, Field, SelectLike, fade } from "../../../shared/components/Primitives";
import { slugify } from "../../../shared/utils/slugify";
import { PRODUCT_TYPES } from "../../../core/products/moduleDefaults";
import { useModuleSelection } from "../hooks/useModuleSelection";
import { ModuleCheckboxList } from "./ModuleCheckboxList";
import { productsService } from "../services/productsService";
import type { ProductSummary } from "../contracts/responses";

/**
 * Criar produto a partir de `ProductSelectScreen` — mesma funcionalidade de
 * `CreateProductForm` (tipo, módulos iniciais por tipo), mas como modal de
 * passo único, espelhando o padrão de `CreateTenantWizardModal` em
 * `TenantSelectScreen`: o tenant já está selecionado nesta tela, então não
 * há etapa de escolha de tenant nem de atribuição de usuário (quem cria já
 * é quem vai operar o produto).
 */
export function CreateProductModal({ tenantId, tenantName, onClose, onCreated }: {
  tenantId: string; tenantName: string; onClose: () => void; onCreated: (product: ProductSummary) => void;
}) {
  const [saving, setSaving] = useState(false);
  const [name, setName] = useState("Novo Produto");
  const [slug, setSlug] = useState("novo-produto");
  const [touchedSlug, setTouchedSlug] = useState(false);
  const { type, setType, moduleOptions, selectedModules, toggleModule, selectedList } = useModuleSelection("Site Institucional");

  const handleNameChange = (v: string) => {
    setName(v);
    if (!touchedSlug) setSlug(slugify(v));
  };

  const handleCreate = async () => {
    setSaving(true);
    try {
      const created = await productsService.create({
        name, slug, type, language: "pt-BR",
        description: "Produto institucional com conteúdo, formulários, SEO e assets governados.",
        template: "Produto operacional padrão", initialModules: selectedList,
        tenantId, assetStorageStrategy: "local",
      });
      onCreated(created);
    } finally {
      setSaving(false);
    }
  };

  return (
    <motion.div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30 backdrop-blur-[3px] p-4" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={onClose}>
      <motion.div {...fade} className="w-full max-w-lg rounded-2xl border border-border bg-card p-6 shadow-[0_24px_80px_rgba(0,0,0,0.2)]" onClick={(e) => e.stopPropagation()}>
        <div className="mb-5 flex items-start justify-between">
          <div>
            <div className="flex items-center gap-2"><h2 className="font-semibold">Criar Produto</h2><Badge tone="violet">{tenantName}</Badge></div>
            <p className="mt-0.5 text-xs text-muted-foreground">Módulos iniciais pré-marcados pelo tipo escolhido — ajuste antes de confirmar.</p>
          </div>
          <button onClick={onClose} className="rounded-lg p-1 transition hover:bg-muted"><X size={17} /></button>
        </div>

        <div className="space-y-3">
          <Field label="Nome do produto" value={name} onChange={handleNameChange} />
          <Field label="Slug" value={slug} onChange={(v) => { setTouchedSlug(true); setSlug(slugify(v)); }} />
          <SelectLike label="Tipo" value={type} options={PRODUCT_TYPES} onChange={setType} />
          <div>
            <p className="mb-2 text-sm font-medium">Módulos iniciais</p>
            <ModuleCheckboxList options={moduleOptions} selected={selectedModules} onToggle={toggleModule} />
          </div>
        </div>

        <div className="mt-5 flex justify-end gap-2">
          <Button onClick={onClose}>Cancelar</Button>
          <Button primary onClick={handleCreate} disabled={saving || !name.trim() || !slug.trim()}>
            {saving ? <Loader2 size={15} className="animate-spin" /> : <CheckCircle2 size={15} />}
            {saving ? "Criando..." : "Criar produto"}
          </Button>
        </div>
      </motion.div>
    </motion.div>
  );
}
