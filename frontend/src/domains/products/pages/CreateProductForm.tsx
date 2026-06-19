import { useState } from "react";
import { useNavigate, useSearchParams } from "react-router";
import { CheckCircle2, Loader2 } from "lucide-react";
import { Badge, Button, Card, Field, PageHeader, SelectLike } from "../../../shared/components/Primitives";
import { BackLink } from "../../../shared/components/BackLink";
import { toast } from "../../../core/notifications/toast";
import { productsService } from "../services/productsService";
import { tenantsService } from "../../../core/tenants/services/tenantsService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";

const INITIAL_MODULES = ["Conteúdo", "Assets", "Forms", "Analytics", "SEO", "Workflow"];

function ProductSummaryPanel({ slug, tenantName }: { slug: string; tenantName?: string }) {
  return (
    <Card>
      <h2 className="text-lg font-semibold">Resumo do produto</h2>
      <p className="mt-2 text-sm text-muted-foreground">{tenantName ? `${tenantName} → ${slug}` : `BYOP → ${slug}`} → módulos iniciais.</p>
      <div className="mt-4 space-y-3 text-sm">
        <div className="flex justify-between"><span>Identificador</span><b className="font-mono">{tenantName ? slug : `byop/${slug}`}</b></div>
        <div className="flex justify-between"><span>Status inicial</span><Badge tone="amber">Configurando</Badge></div>
        <div className="flex justify-between"><span>Módulos</span><b>{INITIAL_MODULES.length}</b></div>
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

  const handleCreate = async () => {
    setSaving(true);
    try {
      const product = await productsService.create({
        name, slug, type: "Site Institucional", language: "pt-BR", description, template: "Produto operacional padrão",
        initialModules: INITIAL_MODULES, tenantId: tenantId ?? undefined,
      });
      toast.success("Produto criado com sucesso!", { description: `byop/${slug} · ${INITIAL_MODULES.length} módulos iniciais habilitados` });
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
            {tenantId && <SelectLike label="Tenant" value={tenant?.name ?? "Carregando..."} />}
            <Field label="Nome do produto" value={name} onChange={setName} />
            <Field label="Slug" value={slug} onChange={setSlug} />
            <SelectLike label="Tipo" value="Site Institucional" />
            <SelectLike label="Idioma padrão" value="Português (Brasil)" />
            <div className="md:col-span-2"><Field label="Descrição" value={description} onChange={setDescription} textarea /></div>
            <SelectLike label="Template inicial" value="Produto operacional padrão" />
            <div>
              <p className="mb-2 text-sm font-medium">Módulos iniciais</p>
              <div className="flex flex-wrap gap-2">{INITIAL_MODULES.map((m) => <Badge key={m} tone="green">{m}</Badge>)}</div>
            </div>
          </div>
          <div className="mt-4 rounded-xl bg-muted p-3 text-sm"><CheckCircle2 size={16} className="mb-2 text-primary" />Slug disponível: <b>{slug}</b></div>
        </Card>
        <ProductSummaryPanel slug={slug} tenantName={tenant?.name} />
      </div>
    </>
  );
}
