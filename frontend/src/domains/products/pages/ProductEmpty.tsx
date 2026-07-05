import { useNavigate } from "react-router";
import { Button, Card, EmptyState, PageHeader } from "../../../shared/components/Primitives";
import { useAuth } from "../../../core/auth/useAuth";
import { getProductSlug } from "../../../shared/utils/productSlugs";

export function ProductEmpty() {
  const navigate = useNavigate();
  const { effectiveProduct } = useAuth();
  const productName = effectiveProduct?.name ?? "Produto";
  const slug = effectiveProduct?.name ? getProductSlug(effectiveProduct.name) : "";

  return (
    <>
      <PageHeader title={productName} desc="Produto recém-criado aguardando capacidades iniciais." badge="Sem módulos">
        <Button onClick={() => navigate("/settings/product")}>Editar dados do produto</Button>
        <Button primary onClick={() => navigate(`/products/${slug}/modules`)}>Habilitar módulos</Button>
      </PageHeader>
      <EmptyState title="Este produto ainda não possui módulos habilitados." description="Escolha as capacidades iniciais para começar a operar este produto digital." />
      <div className="mt-4 grid gap-3 md:grid-cols-3">
        {["Portal: Conteúdo + Forms + SEO", "SaaS: Analytics + Workflow", "Knowledge Base: Conteúdo + Graph"].map((s) => (
          <Card key={s}><p className="font-medium">{s}</p><p className="text-sm text-muted-foreground">Sugestão por tipo de produto.</p></Card>
        ))}
      </div>
    </>
  );
}
