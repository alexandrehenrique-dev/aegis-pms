import { useState } from "react";
import { useNavigate, useSearchParams } from "react-router";
import { CheckCircle2, Loader2 } from "lucide-react";
import { Badge, Button, Card, Field, PageHeader, SelectLike } from "../../../shared/components/Primitives";
import { BackLink } from "../../../shared/components/BackLink";
import { toast } from "../../../core/notifications/toast";
import { productsService } from "../services/productsService";
import { tenantsService } from "../../../core/tenants/services/tenantsService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { PRODUCT_TYPES } from "../../../core/products/moduleDefaults";
import { useModuleSelection } from "../hooks/useModuleSelection";
import { ModuleCheckboxList } from "../components/ModuleCheckboxList";
import type { AssetStorageStrategy } from "../contracts/requests";

function StorageStrategyStep({ strategy, onChange, s3Bucket, onChangeBucket, s3Region, onChangeRegion }: {
  strategy: AssetStorageStrategy; onChange: (s: AssetStorageStrategy) => void;
  s3Bucket: string; onChangeBucket: (v: string) => void; s3Region: string; onChangeRegion: (v: string) => void;
}) {
  return (
    <div className="md:col-span-2">
      <p className="mb-2 text-sm font-medium">Armazenamento de assets</p>
      <div className="grid gap-3 md:grid-cols-2">
        <button
          type="button"
          onClick={() => onChange("local")}
          className={`rounded-xl border p-3 text-left text-sm ${strategy === "local" ? "border-primary bg-muted" : "border-border"}`}
        >
          <p className="font-medium">Local <Badge tone="green">recomendado</Badge></p>
          <p className="mt-1 text-xs text-muted-foreground">Os arquivos do produto ficam guardados no próprio servidor.</p>
        </button>
        <button
          type="button"
          onClick={() => onChange("s3")}
          className={`rounded-xl border p-3 text-left text-sm ${strategy === "s3" ? "border-primary bg-muted" : "border-border"}`}
        >
          <p className="font-medium">Bucket externo (S3)</p>
          <p className="mt-1 text-xs text-muted-foreground">Para quem já usa ou vai usar um provedor de nuvem próprio — exige configuração adicional.</p>
        </button>
      </div>
      {strategy === "s3" && (
        <div className="mt-3 grid gap-3 md:grid-cols-2">
          <Field label="Bucket (opcional, configurável depois)" value={s3Bucket} onChange={onChangeBucket} />
          <Field label="Região (opcional, configurável depois)" value={s3Region} onChange={onChangeRegion} />
        </div>
      )}
    </div>
  );
}

function ProductSummaryPanel({ slug, tenantName, moduleCount }: { slug: string; tenantName?: string; moduleCount: number }) {
  return (
    <Card>
      <h2 className="text-lg font-semibold">Resumo do produto</h2>
      <p className="mt-2 text-sm text-muted-foreground">{tenantName ? `${tenantName} → ${slug}` : `BYOP → ${slug}`} → módulos iniciais.</p>
      <div className="mt-4 space-y-3 text-sm">
        <div className="flex justify-between"><span>Identificador</span><b className="font-mono">{tenantName ? slug : `byop/${slug}`}</b></div>
        <div className="flex justify-between"><span>Status inicial</span><Badge tone="amber">Configurando</Badge></div>
        <div className="flex justify-between"><span>Módulos</span><b>{moduleCount}</b></div>
      </div>
    </Card>
  );
}

export function CreateProductForm() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const tenantId = params.get("tenantId");
  const { data: tenant } = useAsyncData(() => (tenantId ? tenantsService.getTenant(tenantId) : Promise.resolve(undefined)), [tenantId]);

  const [saving, setSaving] = useState(false);
  const [name, setName] = useState("Novo Produto");
  const [slug, setSlug] = useState("novo-produto");
  const [description, setDescription] = useState("Produto institucional com conteúdo, formulários, SEO e assets governados.");
  const { type, setType, moduleOptions, selectedModules, toggleModule, selectedList } = useModuleSelection("Site Institucional");
  const [assetStorageStrategy, setAssetStorageStrategy] = useState<AssetStorageStrategy>("local");
  const [s3Bucket, setS3Bucket] = useState("");
  const [s3Region, setS3Region] = useState("");

  const handleCreate = async () => {
    setSaving(true);
    try {
      const product = await productsService.create({
        name, slug, type, language: "pt-BR", description, template: "Produto operacional padrão",
        initialModules: selectedList, tenantId: tenantId ?? undefined,
        assetStorageStrategy, s3Bucket: s3Bucket || undefined, s3Region: s3Region || undefined,
      });
      toast.success("Produto criado com sucesso!", { description: `byop/${slug} · ${selectedList.length} módulos iniciais habilitados` });
      if (tenantId) navigate(`/admin/tenants/${tenantId}/products/${product.id}/assign-user`);
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      {tenantId && <BackLink to="/admin/tenants" label="Voltar para Gestão de Tenants" />}
      <PageHeader title="Novo Produto" desc={tenantId ? "Passo 2 de 3: criar o primeiro produto do tenant. O próximo passo é atribuí-lo a um usuário." : "Crie um produto digital com módulos iniciais e identidade operacional."}>
        <Button onClick={() => navigate(tenantId ? "/admin/tenants" : "/products")}>Cancelar</Button>
        <Button primary onClick={handleCreate} disabled={saving}>{saving && <Loader2 size={15} className="animate-spin" />}{saving ? "Criando..." : "Criar produto"}</Button>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[1fr_340px]">
        <Card>
          <div className="grid gap-4 md:grid-cols-2">
            {/* fixo por enquanto: tenant/idioma/template do wizard são fixos no MVP — ADR/Sprint 09 */}
            {tenantId && <SelectLike label="Tenant" value={tenant?.name ?? "Carregando..."} locked />}
            <Field label="Nome do produto" value={name} onChange={setName} />
            <Field label="Slug" value={slug} onChange={setSlug} />
            <SelectLike label="Tipo" value={type} options={PRODUCT_TYPES} onChange={setType} />
            <SelectLike label="Idioma padrão" value="Português (Brasil)" locked />
            <div className="md:col-span-2"><Field label="Descrição" value={description} onChange={setDescription} textarea /></div>
            <SelectLike label="Template inicial" value="Produto operacional padrão" locked />
            <div className="md:col-span-2">
              <p className="mb-2 text-sm font-medium">Módulos iniciais</p>
              <p className="mb-2 text-xs text-muted-foreground">Pré-marcados pelo tipo de produto escolhido — desmarque/marque antes de confirmar.</p>
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
          <div className="mt-4 rounded-xl bg-muted p-3 text-sm"><CheckCircle2 size={16} className="mb-2 text-primary" />Slug disponível: <b>{slug}</b></div>
        </Card>
        <ProductSummaryPanel slug={slug} tenantName={tenant?.name} moduleCount={selectedList.length} />
      </div>
    </>
  );
}
