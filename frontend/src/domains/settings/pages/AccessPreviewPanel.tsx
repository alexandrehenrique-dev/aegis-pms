import { Button, Card, PageHeader, SelectLike } from "../../../shared/components/Primitives";

export function AccessPreviewPanel() {
  return (
    <>
      <PageHeader title="Access Preview" module="Permissions" desc="Veja exatamente o que um usuário ou papel consegue visualizar e executar." badge="Editor em Maestro Beton">
        <Button>Selecionar usuário</Button>
        <Button primary>Gerar preview</Button>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[360px_1fr]">
        <Card>
          <h2 className="mb-3 text-lg font-semibold">Entrada</h2>
          <SelectLike label="Usuário ou role" value="Editor" />
          <SelectLike label="Produto" value="Maestro Beton" />
          <SelectLike label="Módulo" value="Conteúdo" />
        </Card>
        <div className="grid gap-4 md:grid-cols-2">
          <Card><h2 className="mb-3 text-lg font-semibold">Pode</h2>{["criar conteúdo", "editar conteúdo", "enviar revisão", "usar assets"].map((x) => <div key={x} className="mb-2 rounded-lg bg-[#ede9fe] p-3 text-sm text-primary">{x}</div>)}</Card>
          <Card><h2 className="mb-3 text-lg font-semibold">Não pode</h2>{["publicar", "alterar permissões", "acessar auditoria completa", "editar tenant"].map((x) => <div key={x} className="mb-2 rounded-lg bg-muted p-3 text-sm text-muted-foreground">{x}</div>)}</Card>
        </div>
      </div>
    </>
  );
}
