import { useState } from "react";
import { CheckCircle2, Loader2 } from "lucide-react";
import { Badge, Button, Card, Field, PageHeader, SelectLike } from "../../../shared/components/Primitives";
import { toast } from "../../../core/notifications/toast";

function ProductSummaryPanel({ slug }: { slug: string }) {
  return (
    <Card>
      <h2 className="text-lg font-semibold">Resumo do produto</h2>
      <p className="mt-2 text-sm text-muted-foreground">BYOP → {slug} → módulos iniciais.</p>
      <div className="mt-4 space-y-3 text-sm">
        <div className="flex justify-between"><span>Identificador</span><b className="font-mono">byop/{slug}</b></div>
        <div className="flex justify-between"><span>Status inicial</span><Badge tone="amber">Configurando</Badge></div>
        <div className="flex justify-between"><span>Módulos</span><b>6</b></div>
      </div>
    </Card>
  );
}

export function CreateProductForm() {
  const [saving, setSaving] = useState(false);
  const [slug, setSlug] = useState("novo-produto");
  const handleCreate = () => {
    setSaving(true);
    setTimeout(() => {
      setSaving(false);
      toast.success("Produto criado com sucesso!", { description: `byop/${slug} · 6 módulos iniciais habilitados` });
    }, 900);
  };
  return (
    <>
      <PageHeader title="Novo Produto" desc="Crie um produto digital com módulos iniciais e identidade operacional.">
        <Button>Cancelar</Button>
        <Button primary onClick={handleCreate}>{saving && <Loader2 size={15} className="animate-spin" />}{saving ? "Criando..." : "Criar produto"}</Button>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[1fr_340px]">
        <Card>
          <div className="grid gap-4 md:grid-cols-2">
            <Field label="Nome do produto" value="Maestro Beton" />
            <Field label="Slug" value={slug} onChange={setSlug} />
            <SelectLike label="Tipo" value="Site Institucional" />
            <SelectLike label="Idioma padrão" value="Português (Brasil)" />
            <div className="md:col-span-2"><Field label="Descrição" value="Produto institucional com conteúdo, formulários, SEO e assets governados." textarea /></div>
            <SelectLike label="Template inicial" value="Produto operacional padrão" />
            <div>
              <p className="mb-2 text-sm font-medium">Módulos iniciais</p>
              <div className="flex flex-wrap gap-2">{["Conteúdo", "Assets", "Forms", "Analytics", "SEO", "Workflow"].map((m) => <Badge key={m} tone="green">{m}</Badge>)}</div>
            </div>
          </div>
          <div className="mt-4 rounded-xl bg-muted p-3 text-sm"><CheckCircle2 size={16} className="mb-2 text-primary" />Slug disponível: <b>{slug}</b></div>
        </Card>
        <ProductSummaryPanel slug={slug} />
      </div>
    </>
  );
}
