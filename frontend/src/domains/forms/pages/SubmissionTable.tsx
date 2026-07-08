import { useEffect, useState } from "react";
import { useNavigate } from "react-router";
import { Loader2 } from "lucide-react";
import { Button, Card, PageHeader, SelectLike, SkeletonLines, PartialErrorWidget } from "../../../shared/components/Primitives";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "../../../shared/components/ui/dialog";
import { formsService } from "../services/formsService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { LeadStatusBadge } from "../components/FormBadges";
import { toast } from "../../../core/notifications/toast";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";
import type { SubmissionSummary } from "../contracts/responses";

const OWNERS = ["Marina Costa", "João Alves", "Camila Rocha", "Pedro Lima"];

function exportSubmissionsCsv(rows: SubmissionSummary[]) {
  const header = ["Data", "Nome", "Email", "Origem", "Status", "Responsável", "Score"];
  const csv = [header, ...rows.map((r) => [r.date, r.name, r.email, r.source, r.status, r.owner, r.score])]
    .map((r) => r.map((c) => `"${c}"`).join(",")).join("\n");
  const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = "submissions.csv";
  a.click();
  URL.revokeObjectURL(url);
}

/**
 * H.1 (BUG-SPRINT-05) — igual ao padrão de `PublicationPanel`: o formulário é
 * escolhido via seletor dinâmico (primeiro formulário do produto como
 * padrão), nunca mais um `FORM_ID` fixo que causava 404 para qualquer
 * produto sem um formulário com esse id exato.
 */
export function SubmissionTable() {
  const navigate = useNavigate();
  const { product } = useCurrentProduct();
  const productId = product?.id ?? "";
  const [selectedFormId, setSelectedFormId] = useState<string | null>(null);
  const { data: forms } = useAsyncData(() => (productId ? formsService.listForms(productId) : Promise.resolve([])), [productId]);
  const { data: submissionsData, loading, error } = useAsyncData(
    () => (productId && selectedFormId ? formsService.listSubmissions(productId, selectedFormId) : Promise.resolve([])),
    [productId, selectedFormId],
  );
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [assignOpen, setAssignOpen] = useState(false);
  const [assignee, setAssignee] = useState(OWNERS[0]);
  const [assigning, setAssigning] = useState(false);

  useEffect(() => {
    if (!selectedFormId && forms && forms.length > 0) setSelectedFormId(forms[0].id);
  }, [forms, selectedFormId]);

  const selectedForm = forms?.find((f) => f.id === selectedFormId);

  const toggleSelect = (email: string) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(email)) next.delete(email); else next.add(email);
      return next;
    });
  };

  const handleAssignSelected = async () => {
    setAssigning(true);
    try {
      await formsService.assignSubmissions(Array.from(selected), assignee);
      toast.success(`${selected.size} lead(s) atribuído(s) a ${assignee}!`);
      setAssignOpen(false);
      setSelected(new Set());
    } finally {
      setAssigning(false);
    }
  };

  const openSubmission = (r: SubmissionSummary) => {
    if (!selectedFormId || !r.id) return;
    navigate(`/forms/${selectedFormId}/submissions/${r.id}`);
  };

  return (
    <>
      <Dialog open={assignOpen} onOpenChange={setAssignOpen}>
        <DialogContent>
          <DialogHeader><DialogTitle>Atribuir {selected.size} lead(s) selecionado(s)</DialogTitle></DialogHeader>
          <SelectLike label="Responsável" value={assignee} options={OWNERS} onChange={setAssignee} />
          <DialogFooter>
            <Button onClick={() => setAssignOpen(false)}>Cancelar</Button>
            <Button primary onClick={handleAssignSelected} disabled={assigning}>{assigning && <Loader2 size={15} className="animate-spin" />}{assigning ? "Atribuindo..." : "Confirmar"}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
      <PageHeader title="Submissions" module="Forms" desc="Submissão é evento operacional: atribua, qualifique e exporte respostas." badge="Submissions">
        {forms && forms.length > 0 && (
          <SelectLike label="" value={selectedForm?.name ?? "Selecione um formulário"} options={forms.map((f) => f.name)}
            onChange={(name) => setSelectedFormId(forms.find((f) => f.name === name)?.id ?? null)} />
        )}
        <Button onClick={() => exportSubmissionsCsv(submissionsData ?? [])} disabled={!submissionsData || submissionsData.length === 0}>Exportar</Button>
        <Button primary onClick={() => { if (selected.size === 0) { toast.error("Selecione ao menos um lead."); return; } setAssignOpen(true); }}>Atribuir selecionados</Button>
      </PageHeader>
      {!selectedFormId ? (
        <Card><p className="text-sm text-muted-foreground">Nenhum formulário neste produto ainda — crie um em "Forms" antes de ver submissões.</p></Card>
      ) : loading ? <SkeletonLines /> : error || !submissionsData ? <PartialErrorWidget /> : (
        <div className="overflow-hidden rounded-2xl border border-border bg-card">
          <table className="hidden w-full text-left text-sm lg:table">
            <thead className="bg-muted text-xs text-muted-foreground"><tr><th className="p-3" />{["Data", "Nome", "Email", "Origem", "Status", "Responsável", "Score", "Ações"].map((h) => <th key={h} className="p-3 font-medium">{h}</th>)}</tr></thead>
            <tbody>
              {submissionsData.map((r) => (
                <tr key={r.email} className="border-t border-border hover:bg-muted/40">
                  <td className="p-3"><input type="checkbox" checked={selected.has(r.email)} onChange={() => toggleSelect(r.email)} aria-label={`Selecionar ${r.name}`} /></td>
                  <td className="p-3">{r.date}</td>
                  <td className="p-3">{r.name}</td>
                  <td className="p-3">{r.email}</td>
                  <td className="p-3">{r.source}</td>
                  <td className="p-3"><LeadStatusBadge status={r.status} /></td>
                  <td className="p-3">{r.owner}</td>
                  <td className="p-3">{r.score}</td>
                  <td className="p-3"><Button onClick={() => openSubmission(r)} disabled={!r.id}>Abrir</Button></td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="grid gap-3 p-3 lg:hidden">
            {submissionsData.map((r) => (
              <Card key={r.email}>
                <div className="flex justify-between">
                  <div className="flex items-start gap-2">
                    <input type="checkbox" checked={selected.has(r.email)} onChange={() => toggleSelect(r.email)} aria-label={`Selecionar ${r.name}`} />
                    <div><h3 className="font-semibold">{r.name}</h3><p className="text-sm text-muted-foreground">{r.email} · {r.source}</p></div>
                  </div>
                  <LeadStatusBadge status={r.status} />
                </div>
                <Button onClick={() => openSubmission(r)} disabled={!r.id}>Abrir</Button>
              </Card>
            ))}
          </div>
        </div>
      )}
    </>
  );
}
