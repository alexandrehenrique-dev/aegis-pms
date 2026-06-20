import { useMemo, useState } from "react";
import { useNavigate, useSearchParams } from "react-router";
import { CheckCircle2, Loader2 } from "lucide-react";
import { Badge, Button, Card, Field, PageHeader, SelectLike } from "../../../shared/components/Primitives";
import { BackLink } from "../../../shared/components/BackLink";
import { toast } from "../../../core/notifications/toast";
import { productsService } from "../services/productsService";
import { tenantsService } from "../../../core/tenants/services/tenantsService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { KNOWLEDGE_GRAPH_DEPENDENCY, PRODUCT_TYPES, PRODUCT_TYPE_MODULE_DEFAULTS, type ProductTypeKey } from "../../../core/products/moduleDefaults";

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
  const [type, setType] = useState<ProductTypeKey>("Site Institucional");
  const [selectedModules, setSelectedModules] = useState<Set<string>>(
    () => new Set(PRODUCT_TYPE_MODULE_DEFAULTS["Site Institucional"].filter((m) => m.default).map((m) => m.key)),
  );

  const moduleOptions = useMemo(() => PRODUCT_TYPE_MODULE_DEFAULTS[type], [type]);

  const handleTypeChange = (next: string) => {
    const nextType = next as ProductTypeKey;
    setType(nextType);
    setSelectedModules(new Set(PRODUCT_TYPE_MODULE_DEFAULTS[nextType].filter((m) => m.default).map((m) => m.key)));
  };

  const toggleModule = (key: string) => {
    setSelectedModules((prev) => {
      const next = new Set(prev);
      if (next.has(key)) {
        next.delete(key);
        if (key === KNOWLEDGE_GRAPH_DEPENDENCY) next.delete("Knowledge Graph");
      } else {
        next.add(key);
      }
      return next;
    });
  };

  const knowledgeGraphBlocked = !selectedModules.has(KNOWLEDGE_GRAPH_DEPENDENCY);
  const initialModules = Array.from(selectedModules);

  const handleCreate = async () => {
    setSaving(true);
    try {
      const product = await productsService.create({
        name, slug, type, language: "pt-BR", description, template: "Produto operacional padrão",
        initialModules, tenantId: tenantId ?? undefined,
      });
      toast.success("Produto criado com sucesso!", { description: `byop/${slug} · ${initialModules.length} módulos iniciais habilitados` });
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
            <SelectLike label="Tipo" value={type} options={PRODUCT_TYPES} onChange={handleTypeChange} />
            <SelectLike label="Idioma padrão" value="Português (Brasil)" locked />
            <div className="md:col-span-2"><Field label="Descrição" value={description} onChange={setDescription} textarea /></div>
            <SelectLike label="Template inicial" value="Produto operacional padrão" locked />
            <div className="md:col-span-2">
              <p className="mb-2 text-sm font-medium">Módulos iniciais</p>
              <p className="mb-2 text-xs text-muted-foreground">Pré-marcados pelo tipo de produto escolhido — desmarque/marque antes de confirmar.</p>
              <div className="flex flex-wrap gap-2">
                {moduleOptions.map((m) => {
                  const isKnowledgeGraph = m.key === "Knowledge Graph";
                  const disabled = m.comingSoon || (isKnowledgeGraph && knowledgeGraphBlocked);
                  const checked = selectedModules.has(m.key) && !m.comingSoon;
                  return (
                    <label
                      key={m.key}
                      title={isKnowledgeGraph && knowledgeGraphBlocked ? `Exige o módulo ${KNOWLEDGE_GRAPH_DEPENDENCY} habilitado` : m.comingSoon ? "Ainda não implementado no backend" : undefined}
                      className={`flex items-center gap-2 rounded-full border border-border px-3 py-1.5 text-sm ${disabled ? "opacity-50" : ""}`}
                    >
                      <input type="checkbox" checked={checked} disabled={disabled} onChange={() => toggleModule(m.key)} aria-label={`Módulo ${m.key}`} />
                      {m.key}
                      {m.comingSoon && <Badge tone="amber">em breve</Badge>}
                    </label>
                  );
                })}
              </div>
            </div>
          </div>
          <div className="mt-4 rounded-xl bg-muted p-3 text-sm"><CheckCircle2 size={16} className="mb-2 text-primary" />Slug disponível: <b>{slug}</b></div>
        </Card>
        <ProductSummaryPanel slug={slug} tenantName={tenant?.name} moduleCount={initialModules.length} />
      </div>
    </>
  );
}
