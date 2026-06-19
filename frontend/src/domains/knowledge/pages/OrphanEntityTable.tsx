import { Button, Card, PageHeader } from "../../../shared/components/Primitives";
import { KGBadge } from "../components/KGBadge";
import type { KGEntityType } from "../mocks/knowledge.mocks";

type OrphanRow = { id: string; label: string; type: KGEntityType; status: string; action: string };

export function OrphanEntityTable() {
  const rows: OrphanRow[] = [
    { id: "or1", label: "Imagem antiga.jpg", type: "Asset", status: "não utilizado", action: "Arquivar" },
    { id: "or2", label: "Form RSVP 2023", type: "Formulário", status: "desconectado", action: "Vincular" },
    { id: "or3", label: "SEO Evento", type: "SEO", status: "sem página", action: "Associar" },
    { id: "or4", label: "Categoria Legado", type: "Categoria", status: "sem uso", action: "Mesclar" },
    { id: "or5", label: "Página temporária", type: "Página", status: "sem vínculo", action: "Revisar" },
  ];
  return (
    <>
      <PageHeader title="Orphan Entities" desc="Entidades sem relações ativas. Limpeza recomendada para manter o grafo consistente." badge="Órfãos">
        <Button>Selecionar todos</Button>
        <Button primary>Resolver órfãos</Button>
      </PageHeader>
      <Card>
        {rows.map((r) => (
          <div key={r.id} className="mb-2 grid gap-2 rounded-xl border border-border p-3 text-sm md:grid-cols-4">
            <div><b>{r.label}</b><p className="text-xs text-muted-foreground">{r.status}</p></div>
            <KGBadge type={r.type} />
            <span className="text-muted-foreground">{r.action} ou vincular</span>
            <Button>{r.action}</Button>
          </div>
        ))}
      </Card>
    </>
  );
}
