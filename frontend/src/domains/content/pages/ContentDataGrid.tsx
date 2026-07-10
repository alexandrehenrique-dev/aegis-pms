import { useMemo, useState } from "react";
import { useNavigate } from "react-router";
import { AnimatePresence } from "motion/react";
import { Filter, Search, Trash2 } from "lucide-react";
import { Badge, Button, EmptyState, PageHeader, SkeletonLines, PartialErrorWidget } from "../../../shared/components/Primitives";
import { Popover, PopoverContent, PopoverTrigger } from "../../../shared/components/ui/popover";
import { ConfirmDialog } from "../../../shared/components/ConfirmDialog";
import { PermGate } from "../../../app/guards/PermGate";
import { useViewAsRole } from "../../../core/permissions/useViewAsRole";
import { toast } from "../../../core/notifications/toast";
import { contentService } from "../services/contentService";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { ContentStatusBadge } from "../components/ContentStatusBadge";
import { ContentCardMobile } from "../components/ContentCardMobile";
import { NewContentModal } from "../components/NewContentModal";
import type { ContentRow } from "../contracts/responses";

function FilterGroup({ label, options, value, onChange }: { label: string; options: string[]; value: string | null; onChange: (v: string | null) => void }) {
  return (
    <div className="mb-3">
      <p className="mb-1 text-sm font-medium">{label}</p>
      <div className="flex flex-wrap gap-1">
        <Button onClick={() => onChange(null)} primary={!value}>Todos</Button>
        {options.map((o) => <Button key={o} onClick={() => onChange(o)} primary={value === o}>{o}</Button>)}
      </div>
    </div>
  );
}

export function ContentDataGrid() {
  const navigate = useNavigate();
  const { viewAsRole } = useViewAsRole();
  const canEdit = ["product_manager", "editor"].includes(viewAsRole);
  const canDelete = viewAsRole === "super_admin";
  const [q, setQ] = useState("");
  const [sel, setSel] = useState<string[]>([]);
  const [status, setStatus] = useState<string | null>(null);
  const [lang, setLang] = useState<string | null>(null);
  const [type, setType] = useState<string | null>(null);
  const [author, setAuthor] = useState<string | null>(null);
  const [showNewContent, setShowNewContent] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);
  const [pendingDelete, setPendingDelete] = useState<ContentRow | null>(null);
  const [deleting, setDeleting] = useState(false);
  const { product } = useCurrentProduct();
  const productId = product?.id ?? "p1";
  const { data: contents, loading, error } = useAsyncData(() => contentService.listContent(productId), [productId, refreshKey]);

  /** F.1.1 (BUG-SPRINT consolidado) — `from` sempre o status real da linha, nunca "Published" fixo (falhava para conteúdo em "In Review"). */
  const handleArchive = async (row: ContentRow) => {
    try {
      await contentService.transitionContent(row.id, row.status, "Archived", "");
      toast.success("Conteúdo arquivado", { description: row.title });
      setRefreshKey((k) => k + 1);
    } catch (err: unknown) {
      toast.error("Falha ao arquivar", {
        description: (err as { message?: string }).message ?? "Tente novamente.",
      });
    }
  };

  /** F.1.2 (BUG-SPRINT consolidado) — exclusão definitiva, restrita pelo backend a Draft nunca publicado + SUPER_ADMIN; o gate de role aqui é só UX, a regra de verdade é sempre no backend. */
  const handleDelete = async () => {
    if (!pendingDelete) return;
    setDeleting(true);
    try {
      await contentService.deleteContent(pendingDelete.id);
      toast.success("Conteúdo excluído", { description: pendingDelete.title });
      setPendingDelete(null);
      setRefreshKey((k) => k + 1);
    } catch (err: unknown) {
      toast.error("Falha ao excluir", {
        description: (err as { message?: string }).message ?? "Tente novamente.",
      });
    } finally {
      setDeleting(false);
    }
  };

  const options = useMemo(() => ({
    statuses: Array.from(new Set((contents ?? []).map((r) => r.status))),
    langs: Array.from(new Set((contents ?? []).map((r) => r.lang))),
    types: Array.from(new Set((contents ?? []).map((r) => r.type))),
    authors: Array.from(new Set((contents ?? []).map((r) => r.author))),
  }), [contents]);

  if (loading) return <SkeletonLines />;
  if (error || !contents) return <PartialErrorWidget />;

  const rows = contents.filter((r) =>
    r.title.toLowerCase().includes(q.toLowerCase()) &&
    (!status || r.status === status) && (!lang || r.lang === lang) && (!type || r.type === type) && (!author || r.author === author)
  );

  return (
    <>
      <AnimatePresence>{showNewContent && <NewContentModal onClose={() => setShowNewContent(false)} />}</AnimatePresence>
      <AnimatePresence>
        {pendingDelete && (
          <ConfirmDialog
            title={`Excluir permanentemente "${pendingDelete.title}"?`}
            desc="Só é possível excluir rascunhos que nunca foram publicados. Esta ação é irreversível."
            danger
            loading={deleting}
            onCancel={() => setPendingDelete(null)}
            onConfirm={handleDelete}
          />
        )}
      </AnimatePresence>
      <PageHeader title="Lista de Conteúdos" module="Conteúdo" desc="DataGrid operacional de artigos, traduções e versões." badge="Conteúdo">
        <Popover>
          <PopoverTrigger asChild><Button><Filter size={15} />Status / Idioma / Tipo / Autor</Button></PopoverTrigger>
          <PopoverContent className="w-80">
            <FilterGroup label="Status" options={options.statuses} value={status} onChange={setStatus} />
            <FilterGroup label="Idioma" options={options.langs} value={lang} onChange={setLang} />
            <FilterGroup label="Tipo" options={options.types} value={type} onChange={setType} />
            <FilterGroup label="Autor" options={options.authors} value={author} onChange={setAuthor} />
          </PopoverContent>
        </Popover>
        <PermGate allowed={canEdit}><Button primary onClick={() => setShowNewContent(true)}>Novo conteúdo</Button></PermGate>
      </PageHeader>
      <div className="mb-4 rounded-2xl border border-border bg-card p-3">
        <div className="flex items-center gap-2 rounded-xl border border-border px-3 py-2">
          <Search size={16} />
          <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Buscar conteúdo..." className="w-full bg-transparent text-sm outline-none" />
        </div>
        <div className="mt-3 flex flex-wrap gap-2 text-xs">
          <Badge>Ordenação</Badge>
          <Badge>Paginação 1–9 de 42</Badge>
          <Badge>Ações em lote {sel.length ? `ativas (${sel.length})` : ""}</Badge>
          <Badge tone="blue">Exportar</Badge>
        </div>
      </div>
      {rows.length === 0 ? <EmptyState title="Busca sem resultado" description="Ajuste a busca ou filtros do DataGrid." /> : (
        <>
          <div className="hidden overflow-hidden rounded-2xl border border-border bg-card lg:block">
            <table className="w-full text-left text-sm">
              <thead data-tour="content-list" className="bg-muted text-xs text-muted-foreground"><tr>{["", "Título", "Tipo", "Idioma", "Autor", "Status", "Última atualização", "Publicação", "Versão", "Ações"].map((h) => <th key={h} className="p-3 font-medium">{h}</th>)}</tr></thead>
              <tbody>
                {rows.map((r) => (
                  <tr key={r.id} className="border-t border-border hover:bg-muted/40">
                    <td className="p-3"><input type="checkbox" onChange={(e) => setSel(e.target.checked ? [...sel, r.id] : sel.filter((x) => x !== r.id))} /></td>
                    <td className="p-3">{r.title}</td>
                    <td className="p-3">{r.type}</td>
                    <td className="p-3">{r.lang}</td>
                    <td className="p-3">{r.author}</td>
                    <td className="p-3"><ContentStatusBadge status={r.status} /></td>
                    <td className="p-3">{r.updatedAt}</td>
                    <td className="p-3">{r.publication}</td>
                    <td className="p-3">{r.version}</td>
                    <td className="p-3">
                      <div className="flex flex-wrap gap-1">
                        <PermGate allowed={canEdit}><Button onClick={() => navigate(`/content/${r.id}/editor`)}>Abrir</Button></PermGate>
                        <Button onClick={() => navigate(`/content/${r.id}/preview`)}>Preview</Button>
                        <Button onClick={() => navigate(`/content/${r.id}/versions`)}>Histórico</Button>
                        {(r.status === "Published" || r.status === "In Review") && (
                          <PermGate allowed={canEdit}><Button onClick={() => handleArchive(r)}>Arquivar</Button></PermGate>
                        )}
                        {r.status === "Draft" && (
                          <PermGate allowed={canDelete}>
                            <button onClick={() => setPendingDelete(r)} aria-label={`Excluir ${r.title}`} className="rounded-lg p-2 text-muted-foreground transition hover:bg-destructive/10 hover:text-destructive"><Trash2 size={14} /></button>
                          </PermGate>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="grid gap-3 lg:hidden">{rows.map((r) => (
            <ContentCardMobile key={r.id} row={r} onArchive={() => handleArchive(r)} onRequestDelete={() => setPendingDelete(r)} canDelete={canDelete} />
          ))}</div>
        </>
      )}
    </>
  );
}
