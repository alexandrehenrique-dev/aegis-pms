import { useNavigate } from "react-router";
import { Filter } from "lucide-react";
import { Button, Card, PageHeader } from "../../../shared/components/Primitives";
import { forms } from "../mocks/forms.mocks";
import { FormStatusBadge } from "../components/FormBadges";

export function FormsList() {
  const navigate = useNavigate();
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
              <tr key={f[0]} className="border-t border-border hover:bg-muted/40">
                <td className="p-3 font-medium">{f[0]}</td>
                <td className="p-3">{f[1]}</td>
                <td className="p-3"><FormStatusBadge status={f[2]} /></td>
                {f.slice(3, 7).map((c) => <td key={c} className="p-3">{c}</td>)}
                <td className="p-3">
                  <div className="flex flex-wrap gap-1">
                    <Button onClick={() => navigate(`/forms/${f[0].toLowerCase().replace(/\s+/g, "-")}`)}>Editar</Button>
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
            <Card key={f[0]}>
              <div className="flex justify-between">
                <div><h3 className="font-semibold">{f[0]}</h3><p className="text-sm text-muted-foreground">{f[1]} · {f[3]} respostas · {f[4]}</p></div>
                <FormStatusBadge status={f[2]} />
              </div>
              <div className="mt-3 flex gap-2">
                <Button onClick={() => navigate(`/forms/${f[0].toLowerCase().replace(/\s+/g, "-")}`)}>Editar</Button>
                <Button onClick={() => navigate("/forms/submissions")}>Submissions</Button>
              </div>
            </Card>
          ))}
        </div>
      </div>
    </>
  );
}
