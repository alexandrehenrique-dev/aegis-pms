import { useState } from "react";
import { CheckCircle2, Loader2, X } from "lucide-react";
import { Badge, Button, Field, SelectLike } from "../../../shared/components/Primitives";
import { ModalShell } from "../../../shared/components/ModalShell";
import { slugify } from "../../../shared/utils/slugify";
import { slugError, textLengthError } from "../../../shared/utils/validation";
import { PRODUCT_TYPES } from "../../../core/products/moduleDefaults";
import { useModuleSelection } from "../hooks/useModuleSelection";
import { ModuleCheckboxList } from "./ModuleCheckboxList";
import { StorageStrategyStep } from "./StorageStrategyStep";
import { productsService } from "../services/productsService";
import type { AssetStorageStrategy } from "../contracts/requests";
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
  const [assetStorageStrategy, setAssetStorageStrategy] = useState<AssetStorageStrategy>("local");
  const [s3Bucket, setS3Bucket] = useState("");
  const [s3Region, setS3Region] = useState("");
  const [touched, setTouched] = useState<{ name?: boolean; slug?: boolean }>({});

  const nameErr = textLengthError(name, 3, 100, "Nome do produto");
  const slugErr = slugError(slug);
  const hasErrors = !!nameErr || !!slugErr;

  const handleNameChange = (v: string) => {
    setName(v);
    if (!touchedSlug) setSlug(slugify(v));
  };

  const handleCreate = async () => {
    setTouched({ name: true, slug: true });
    if (hasErrors) return;
    setSaving(true);
    try {
      const created = await productsService.create({
        name, slug, type, language: "pt-BR",
        description: "Produto institucional com conteúdo, formulários, SEO e assets governados.",
        initialModules: selectedList,
        tenantId, assetStorageStrategy, s3Bucket: s3Bucket || undefined, s3Region: s3Region || undefined,
      });
      onCreated(created);
    } finally {
      setSaving(false);
    }
  };

  return (
    <ModalShell onClose={onClose} maxWidthClassName="max-w-lg">
      <div className="flex max-h-[85vh] flex-col">
        <div className="mb-5 flex items-start justify-between">
          <div>
            <div className="flex items-center gap-2"><h2 className="font-semibold">Criar Produto</h2><Badge tone="violet">{tenantName}</Badge></div>
            <p className="mt-0.5 text-xs text-muted-foreground">Módulos iniciais pré-marcados pelo tipo escolhido — ajuste antes de confirmar.</p>
          </div>
          <button onClick={onClose} className="rounded-lg p-1 transition hover:bg-muted"><X size={17} /></button>
        </div>

        <div className="min-h-0 flex-1 space-y-3 overflow-y-auto">
          <Field label="Nome do produto" value={name} onChange={handleNameChange} onBlur={() => setTouched((t) => ({ ...t, name: true }))} error={touched.name ? nameErr : undefined} />
          <Field label="Slug" value={slug} onChange={(v) => { setTouchedSlug(true); setSlug(slugify(v)); }} onBlur={() => setTouched((t) => ({ ...t, slug: true }))} error={touched.slug ? slugErr : undefined} />
          <SelectLike label="Tipo" value={type} options={PRODUCT_TYPES} onChange={setType} />
          <div>
            <p className="mb-2 text-sm font-medium">Módulos iniciais</p>
            <ModuleCheckboxList options={moduleOptions} selected={selectedModules} onToggle={toggleModule} />
          </div>
          <StorageStrategyStep
            strategy={assetStorageStrategy}
            onChange={setAssetStorageStrategy}
            s3Bucket={s3Bucket}
            onChangeBucket={setS3Bucket}
            s3Region={s3Region}
            onChangeRegion={setS3Region}
          />
        </div>

        <div className="mt-5 flex justify-end gap-2 border-t border-border pt-4">
          <Button onClick={onClose}>Cancelar</Button>
          <Button primary onClick={handleCreate} disabled={saving || hasErrors}>
            {saving ? <Loader2 size={15} className="animate-spin" /> : <CheckCircle2 size={15} />}
            {saving ? "Criando..." : "Criar produto"}
          </Button>
        </div>
      </div>
    </ModalShell>
  );
}
