import { useNavigate } from "react-router";
import { Button, Card, EmptyState, PageHeader } from "../../../shared/components/Primitives";

export function ProductEmpty() {
  const navigate = useNavigate();
  return (
    <>
      <PageHeader title="Conecta Talentos" desc="Produto recém-criado aguardando capacidades iniciais." badge="Sem módulos">
        <Button onClick={() => navigate("/settings/product")}>Editar dados do produto</Button>
        <Button primary onClick={() => navigate("/products/maestro-beton/modules")}>Habilitar módulos</Button>
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
