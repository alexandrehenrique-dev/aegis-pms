import { useNavigate } from "react-router";
import { Filter } from "lucide-react";
import { Button, Card, PageHeader, SkeletonLines, PartialErrorWidget } from "../../../shared/components/Primitives";
import { formsService } from "../services/formsService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { FormStatusBadge } from "../components/FormBadges";

export function FormsList() {
  const navigate = useNavigate();
  const { data: forms, loading, error } = useAsyncData(() => formsService.listForms(), []);

  if (loading) return <SkeletonLines />;
  if (error || !forms) return <PartialErrorWidget />;

  return (
    <>
      <PageHeader title="Lista de Formulários" module="Forms" desc="Tabela operacional de formulários como ativos de aquisição e relacionamento." badge="Forms">
        <Button><Filter size={15} />Tipo / Status</Button>
        <Button>Exportar</Button>
        <Button primary onClick={() => navigate("/forms/new")}>Novo formulário</Button>
      </PageHeader>
      <div className="overflow-hidden rounded-2xl border border-border bg-card">
        <table className="hidden w-full text-left text-sm lg:table">
          <thead className="bg-muted text-xs text-muted-foreground"><tr>{["Nome", "Tipo", "Status", "Respostas", "Conversão", "Última atividade", "Publicação", "Ações"].map((h) => <th key={h} className="p-3 font-medium">{h}</th>)}</tr></thead>
          <tbody>
            {forms.map((f) => (
              <tr key={f.name} className="border-t border-border hover:bg-muted/40">
                <td className="p-3 font-medium">{f.name}</td>
                <td className="p-3">{f.type}</td>
                <td className="p-3"><FormStatusBadge status={f.status} /></td>
                <td className="p-3">{f.responses}</td>
                <td className="p-3">{f.conversion}</td>
                <td className="p-3">{f.lastActivity}</td>
                <td className="p-3">{f.publication}</td>
                <td className="p-3">
                  <div className="flex flex-wrap gap-1">
                    <Button onClick={() => navigate(`/forms/${f.name.toLowerCase().replace(/\s+/g, "-")}`)}>Editar</Button>
                    <Button onClick={() => navigate("/forms/preview")}>Preview</Button>
                    <Button onClick={() => navigate("/forms/submissions")}>Submissions</Button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <div className="grid gap-3 p-3 lg:hidden">
          {forms.map((f) => (
            <Card key={f.name}>
              <div className="flex justify-between">
                <div><h3 className="font-semibold">{f.name}</h3><p className="text-sm text-muted-foreground">{f.type} · {f.responses} respostas · {f.conversion}</p></div>
                <FormStatusBadge status={f.status} />
              </div>
              <div className="mt-3 flex gap-2">
                <Button onClick={() => navigate(`/forms/${f.name.toLowerCase().replace(/\s+/g, "-")}`)}>Editar</Button>
                <Button onClick={() => navigate("/forms/submissions")}>Submissions</Button>
              </div>
            </Card>
          ))}
        </div>
      </div>
    </>
  );
}
