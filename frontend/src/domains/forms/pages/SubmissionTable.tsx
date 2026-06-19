import { useNavigate } from "react-router";
import { Button, Card, PageHeader, SkeletonLines, PartialErrorWidget } from "../../../shared/components/Primitives";
import { formsService } from "../services/formsService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { LeadStatusBadge } from "../components/FormBadges";

export function SubmissionTable() {
  const navigate = useNavigate();
  const { data: submissionsData, loading, error } = useAsyncData(() => formsService.listSubmissions(), []);

  if (loading) return <SkeletonLines />;
  if (error || !submissionsData) return <PartialErrorWidget />;

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
              <tr key={r.email} className="border-t border-border hover:bg-muted/40">
                <td className="p-3">{r.date}</td>
                <td className="p-3">{r.name}</td>
                <td className="p-3">{r.email}</td>
                <td className="p-3">{r.source}</td>
                <td className="p-3"><LeadStatusBadge status={r.status} /></td>
                <td className="p-3">{r.owner}</td>
                <td className="p-3">{r.score}</td>
                <td className="p-3"><Button onClick={() => navigate("/forms/submissions/1")}>Abrir</Button></td>
              </tr>
            ))}
          </tbody>
        </table>
        <div className="grid gap-3 p-3 lg:hidden">
          {submissionsData.map((r) => (
            <Card key={r.email}>
              <div className="flex justify-between">
                <div><h3 className="font-semibold">{r.name}</h3><p className="text-sm text-muted-foreground">{r.email} · {r.source}</p></div>
                <LeadStatusBadge status={r.status} />
              </div>
              <Button onClick={() => navigate("/forms/submissions/1")}>Abrir</Button>
            </Card>
          ))}
        </div>
      </div>
    </>
  );
}
