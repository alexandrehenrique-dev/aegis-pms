import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router";
import { AnimatePresence } from "motion/react";
import { Filter, Trash2 } from "lucide-react";
import { Button, Card, PageHeader, SkeletonLines, PartialErrorWidget } from "../../../shared/components/Primitives";
import { Popover, PopoverContent, PopoverTrigger } from "../../../shared/components/ui/popover";
import { ConfirmDialog } from "../../../shared/components/ConfirmDialog";
import { toast } from "../../../core/notifications/toast";
import { formsService } from "../services/formsService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { FormStatusBadge } from "../components/FormBadges";
import type { FormSummary } from "../contracts/responses";

function exportFormsCsv(forms: FormSummary[]) {
  const header = ["Nome", "Tipo", "Status", "Respostas", "Conversão", "Última atividade", "Publicação"];
  const rows = forms.map((f) => [f.name, f.type, f.status, f.responses, f.conversion, f.lastActivity, f.publication]);
  // Exportação do que está na tela (mock); ainda não fala com um endpoint real.
  const csv = [header, ...rows].map((r) => r.map((c) => `"${c}"`).join(",")).join("\n");
  const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = "formularios.csv";
  a.click();
  URL.revokeObjectURL(url);
}

export function FormsList() {
  const navigate = useNavigate();
  const { data: loadedForms, loading, error } = useAsyncData(() => formsService.listForms(), []);
  const [forms, setForms] = useState<FormSummary[]>([]);
  const [typeFilter, setTypeFilter] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<string | null>(null);
  const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => { setForms(loadedForms ?? []); }, [loadedForms]);

  const types = useMemo(() => Array.from(new Set(forms.map((f) => f.type))), [forms]);
  const statuses = useMemo(() => Array.from(new Set(forms.map((f) => f.status))), [forms]);
  const filtered = useMemo(() => forms.filter((f) => (!typeFilter || f.type === typeFilter) && (!statusFilter || f.status === statusFilter)), [forms, typeFilter, statusFilter]);
  const pendingDeleteForm = forms.find((f) => f.id === pendingDeleteId) ?? null;

  const handleDelete = async () => {
    if (!pendingDeleteForm) return;
    setDeleting(true);
    setDeleteError(null);
    try {
      await formsService.removeForm(pendingDeleteForm.id, pendingDeleteForm.productSlug);
      setForms((prev) => prev.filter((f) => f.id !== pendingDeleteForm.id));
      toast.success("Formulário removido", { description: pendingDeleteForm.name });
      setPendingDeleteId(null);
    } catch (e) {
      setDeleteError((e as { message?: string })?.message ?? "Não foi possível excluir este formulário.");
    } finally {
      setDeleting(false);
    }
  };

  if (loading) return <SkeletonLines />;
  if (error || !loadedForms) return <PartialErrorWidget />;

  return (
    <>
      <AnimatePresence>
        {pendingDeleteForm && (
          <ConfirmDialog
            title={`Excluir formulário "${pendingDeleteForm.name}"?`}
            desc={deleteError ?? "Esta ação é irreversível. Se o formulário estiver em uso numa página, a exclusão será bloqueada."}
            danger
            loading={deleting}
            onCancel={() => { setPendingDeleteId(null); setDeleteError(null); }}
            onConfirm={handleDelete}
          />
        )}
      </AnimatePresence>
      <PageHeader title="Lista de Formulários" module="Forms" desc="Tabela operacional de formulários como ativos de aquisição e relacionamento." badge="Forms">
        <Popover>
          <PopoverTrigger asChild><Button><Filter size={15} />Tipo / Status</Button></PopoverTrigger>
          <PopoverContent>
            <p className="mb-2 text-sm font-medium">Tipo</p>
            <div className="mb-3 flex flex-wrap gap-1">
              <Button onClick={() => setTypeFilter(null)} primary={!typeFilter}>Todos</Button>
              {types.map((t) => <Button key={t} onClick={() => setTypeFilter(t)} primary={typeFilter === t}>{t}</Button>)}
            </div>
            <p className="mb-2 text-sm font-medium">Status</p>
            <div className="flex flex-wrap gap-1">
              <Button onClick={() => setStatusFilter(null)} primary={!statusFilter}>Todos</Button>
              {statuses.map((s) => <Button key={s} onClick={() => setStatusFilter(s)} primary={statusFilter === s}>{s}</Button>)}
            </div>
          </PopoverContent>
        </Popover>
        <Button onClick={() => exportFormsCsv(filtered)}>Exportar</Button>
        <Button primary onClick={() => navigate("/forms/new")}>Novo formulário</Button>
      </PageHeader>
      <div className="overflow-hidden rounded-2xl border border-border bg-card">
        <table className="hidden w-full text-left text-sm lg:table">
          <thead className="bg-muted text-xs text-muted-foreground"><tr>{["Nome", "Tipo", "Status", "Respostas", "Conversão", "Última atividade", "Publicação", "Ações"].map((h) => <th key={h} className="p-3 font-medium">{h}</th>)}</tr></thead>
          <tbody>
            {filtered.map((f) => (
              <tr key={f.id} className="border-t border-border hover:bg-muted/40">
                <td className="p-3 font-medium">{f.name}</td>
                <td className="p-3">{f.type}</td>
                <td className="p-3"><FormStatusBadge status={f.status} /></td>
                <td className="p-3">{f.responses}</td>
                <td className="p-3">{f.conversion}</td>
                <td className="p-3">{f.lastActivity}</td>
                <td className="p-3">{f.publication}</td>
                <td className="p-3">
                  <div className="flex flex-wrap gap-1">
                    <Button onClick={() => navigate(`/forms/${f.id}`)}>Editar</Button>
                    <Button onClick={() => navigate("/forms/preview")}>Preview</Button>
                    <Button onClick={() => navigate("/forms/submissions")}>Submissions</Button>
                    <button onClick={() => setPendingDeleteId(f.id)} aria-label={`Excluir ${f.name}`} className="rounded-lg p-2 text-muted-foreground transition hover:bg-destructive/10 hover:text-destructive"><Trash2 size={14} /></button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <div className="grid gap-3 p-3 lg:hidden">
          {filtered.map((f) => (
            <Card key={f.id}>
              <div className="flex justify-between">
                <div><h3 className="font-semibold">{f.name}</h3><p className="text-sm text-muted-foreground">{f.type} · {f.responses} respostas · {f.conversion}</p></div>
                <FormStatusBadge status={f.status} />
              </div>
              <div className="mt-3 flex gap-2">
                <Button onClick={() => navigate(`/forms/${f.id}`)}>Editar</Button>
                <Button onClick={() => navigate("/forms/submissions")}>Submissions</Button>
                <button onClick={() => setPendingDeleteId(f.id)} aria-label={`Excluir ${f.name}`} className="rounded-lg p-2 text-muted-foreground transition hover:bg-destructive/10 hover:text-destructive"><Trash2 size={14} /></button>
              </div>
            </Card>
          ))}
        </div>
      </div>
    </>
  );
}
