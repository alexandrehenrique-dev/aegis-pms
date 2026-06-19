import { useNavigate } from "react-router";
import { Button, Card, PageHeader } from "../../../shared/components/Primitives";
import { submissionsData } from "../mocks/forms.mocks";
import { LeadStatusBadge } from "../components/FormBadges";

export function SubmissionTable() {
  const navigate = useNavigate();
  return (
    <>
      <PageHeader title="Submissions" module="Forms" desc="Submissão é evento operacional: atribua, qualifique e exporte respostas." badge="Submissions">
        <Button>Exportar</Button>
        <Button primary>Atribuir selecionados</Button>
      </PageHeader>
      <div className="overflow-hidden rounded-2xl border border-border bg-card">
        <table className="hidden w-full text-left text-sm lg:table">
          <thead className="bg-muted text-xs text-muted-foreground"><tr>{["Data", "Nome", "Email", "Origem", "Status", "Responsável", "Score", "Ações"].map((h) => <th key={h} className="p-3 font-medium">{h}</th>)}</tr></thead>
          <tbody>
            {submissionsData.map((r) => (
              <tr key={r[1]} className="border-t border-border hover:bg-muted/40">
                {r.slice(0, 4).map((c) => <td key={c} className="p-3">{c}</td>)}
                <td className="p-3"><LeadStatusBadge status={r[4]} /></td>
                {r.slice(5).map((c) => <td key={c} className="p-3">{c}</td>)}
                <td className="p-3"><Button onClick={() => navigate("/forms/submissions/1")}>Abrir</Button></td>
              </tr>
            ))}
          </tbody>
        </table>
        <div className="grid gap-3 p-3 lg:hidden">
          {submissionsData.map((r) => (
            <Card key={r[1]}>
              <div className="flex justify-between">
                <div><h3 className="font-semibold">{r[1]}</h3><p className="text-sm text-muted-foreground">{r[2]} · {r[3]}</p></div>
                <LeadStatusBadge status={r[4]} />
              </div>
              <Button onClick={() => navigate("/forms/submissions/1")}>Abrir</Button>
            </Card>
          ))}
        </div>
      </div>
    </>
  );
}
