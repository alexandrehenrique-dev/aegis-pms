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
import { PRODUCT_PAGE_SKELETONS } from "../../../core/products/productTemplates";
import { useModuleSelection } from "../hooks/useModuleSelection";
import { ModuleCheckboxList } from "../components/ModuleCheckboxList";
import { StorageStrategyStep } from "../components/StorageStrategyStep";
import { slugify } from "../../../shared/utils/slugify";
import { slugError, textLengthError } from "../../../shared/utils/validation";
import type { AssetStorageStrategy } from "../contracts/requests";

function PageSkeletonPreview({ type }: { type: string }) {
  const skeleton = PRODUCT_PAGE_SKELETONS[type as keyof typeof PRODUCT_PAGE_SKELETONS];
  if (skeleton) {
    return (
      <p className="text-sm text-muted-foreground">
        Este produto nasce com {skeleton.length} página{skeleton.length === 1 ? "" : "s"}: {skeleton.map((p) => p.title).join(", ")}.
      </p>
    );
  }
  if (type === "Custom") {
    return <p className="text-sm text-muted-foreground">Este produto nasce em branco — sem páginas, sem módulos pré-habilitados.</p>;
  }
  return <p className="text-sm text-muted-foreground">Este produto nasce sem páginas — módulos recomendados já vêm pré-marcados.</p>;
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
  const [touchedSlug, setTouchedSlug] = useState(false);
  const [touched, setTouched] = useState<{ name?: boolean; slug?: boolean }>({});
  const [slugTakenErr, setSlugTakenErr] = useState<string | undefined>();
  const [checkingSlug, setCheckingSlug] = useState(false);

  const nameErr = textLengthError(name, 3, 100, "Nome do produto");
  const slugErr = slugError(slug) || slugTakenErr;
  const hasErrors = !!nameErr || !!slugErr || checkingSlug;

  const handleNameChange = (v: string) => {
    setName(v);
    if (!touchedSlug) setSlug(slugify(v));
  };

  const handleSlugBlur = async () => {
    setTouched((t) => ({ ...t, slug: true }));
    setSlugTakenErr(undefined);
    if (slugError(slug)) return;
    setCheckingSlug(true);
    try {
      const available = await productsService.checkSlugAvailable(slug);
      if (!available) setSlugTakenErr("Este identificador já está em uso neste tenant.");
    } finally {
      setCheckingSlug(false);
    }
  };

  const handleCreate = async () => {
    setTouched({ name: true, slug: true });
    if (hasErrors) return;
    setSaving(true);
    try {
      const product = await productsService.create({
        name, slug, type, language: "pt-BR", description,
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
        <Button primary onClick={handleCreate} disabled={saving || hasErrors}>{saving && <Loader2 size={15} className="animate-spin" />}{saving ? "Criando..." : "Criar produto"}</Button>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[1fr_340px]">
        <Card>
          <div className="grid gap-4 md:grid-cols-2">
            {/* fixo por enquanto: tenant/idioma do wizard são fixos no MVP — ADR/Sprint 09 */}
            {tenantId && <SelectLike label="Tenant" value={tenant?.name ?? "Carregando..."} locked />}
            <Field label="Nome do produto" value={name} onChange={handleNameChange} onBlur={() => setTouched((t) => ({ ...t, name: true }))} error={touched.name ? nameErr : undefined} />
            <Field label="Slug" value={slug} onChange={(v) => { setTouchedSlug(true); setSlugTakenErr(undefined); setSlug(slugify(v)); }} onBlur={handleSlugBlur} error={touched.slug ? (checkingSlug ? "Verificando disponibilidade..." : slugErr) : undefined} />
            <SelectLike label="Tipo" value={type} options={PRODUCT_TYPES} onChange={setType} />
            <SelectLike label="Idioma padrão" value="Português (Brasil)" locked />
            <div className="md:col-span-2"><Field label="Descrição" value={description} onChange={setDescription} textarea /></div>
            <div className="md:col-span-2"><PageSkeletonPreview type={type} /></div>
            <div className="md:col-span-2">
              <p className="mb-2 text-sm font-medium">Módulos iniciais</p>
              <p className="mb-2 text-xs text-muted-foreground">Pré-marcados pelo tipo de produto escolhido — desmarque/marque antes de confirmar.</p>
              <ModuleCheckboxList options={moduleOptions} selected={selectedModules} onToggle={toggleModule} />
            </div>
            <div className="md:col-span-2">
              <StorageStrategyStep
                strategy={assetStorageStrategy}
                onChange={setAssetStorageStrategy}
                s3Bucket={s3Bucket}
                onChangeBucket={setS3Bucket}
                s3Region={s3Region}
                onChangeRegion={setS3Region}
              />
            </div>
          </div>
          <div className="mt-4 rounded-xl bg-muted p-3 text-sm">
            <CheckCircle2 size={16} className={`mb-2 ${slugTakenErr ? "text-destructive" : "text-primary"}`} />
            {slugTakenErr ? "Slug indisponível: " : "Slug disponível: "}<b>{slug}</b>
          </div>
        </Card>
        <ProductSummaryPanel slug={slug} tenantName={tenant?.name} moduleCount={selectedList.length} />
      </div>
    </>
  );
}
