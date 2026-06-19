import { Badge, Button, Card, PageHeader } from "../../../shared/components/Primitives";

export function VersionTimeline({ compact = false }: { compact?: boolean }) {
  const body = (
    <div className="space-y-2">
      {["v18 Home hero ajustado", "v17 SEO description alterada", "v16 CTA secundário atualizado", "v15 Imagem hero alterada"].map((v, i) => (
        <div key={v} className="rounded-xl border border-border p-3 text-sm">
          <div className="flex justify-between"><b>{v}</b><Badge tone={i === 0 ? "green" : "neutral"}>{i === 0 ? "Atual" : "Antiga"}</Badge></div>
          <p className="text-muted-foreground">Marina Costa · há {i + 1} dias · diferença resumida disponível</p>
          <div className="mt-2 flex gap-2"><Button>Visualizar</Button><Button>Comparar</Button>{i > 0 && <Button>Restaurar</Button>}</div>
        </div>
      ))}
    </div>
  );
  if (compact) return body;
  return (
    <>
      <PageHeader title="Histórico de Versões" module="Conteúdo" desc="Histórico estilo Git simplificado com autor, data, comentário e ações." badge="Versões">
        <Button>Sem permissão para restaurar</Button>
      </PageHeader>
      <Card>{body}</Card>
    </>
  );
}
