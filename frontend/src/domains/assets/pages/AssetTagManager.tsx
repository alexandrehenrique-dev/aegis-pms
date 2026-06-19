import { Badge, Button, Card, EmptyState, PageHeader, PartialErrorWidget, PermissionHint } from "../../../shared/components/Primitives";
import { assetTags } from "../mocks/assets.mocks";

export function AssetTagManager() {
  return (
    <>
      <PageHeader title="Tags de Assets" module="Assets" desc="Organize tags como estrutura operacional, não como decoração." badge="Tags">
        <Button>Mesclar tags</Button>
        <Button primary>Criar tag</Button>
      </PageHeader>
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        {assetTags.map((t, i) => (
          <Card key={t}>
            <div className="flex items-center justify-between"><h3 className="font-semibold">#{t}</h3><Badge tone={i % 3 === 0 ? "green" : "neutral"}>{i + 2} assets</Badge></div>
            <p className="mt-2 text-sm text-muted-foreground">Tag em uso no produto Maestro Beton.</p>
            <div className="mt-4 flex gap-2"><Button>Editar</Button><Button>Remover</Button></div>
          </Card>
        ))}
      </div>
      <div className="mt-4 grid gap-3 md:grid-cols-3">
        <EmptyState compact title="Sem tags" description="Estado previsto para produtos novos." />
        <PartialErrorWidget />
        <PermissionHint />
      </div>
    </>
  );
}
