import { useMemo, useState } from "react";
import { useNavigate } from "react-router";
import { Filter, Search } from "lucide-react";
import { Badge, Button, EmptyState, PageHeader, SkeletonLines, PartialErrorWidget } from "../../../shared/components/Primitives";
import { Popover, PopoverContent, PopoverTrigger } from "../../../shared/components/ui/popover";
import { contentService } from "../services/contentService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { ContentStatusBadge } from "../components/ContentStatusBadge";
import { ContentCardMobile } from "../components/ContentCardMobile";

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
  const [q, setQ] = useState("");
  const [sel, setSel] = useState<string[]>([]);
  const [status, setStatus] = useState<string | null>(null);
  const [lang, setLang] = useState<string | null>(null);
  const [type, setType] = useState<string | null>(null);
  const [author, setAuthor] = useState<string | null>(null);
  const { data: contents, loading, error } = useAsyncData(() => contentService.listContent(), []);

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
      <PageHeader title="Lista de Conteúdos" module="Conteúdo" desc="DataGrid operacional de páginas, seções, artigos, traduções e versões." badge="Conteúdo">
        <Popover>
          <PopoverTrigger asChild><Button><Filter size={15} />Status / Idioma / Tipo / Autor</Button></PopoverTrigger>
          <PopoverContent className="w-80">
            <FilterGroup label="Status" options={options.statuses} value={status} onChange={setStatus} />
            <FilterGroup label="Idioma" options={options.langs} value={lang} onChange={setLang} />
            <FilterGroup label="Tipo" options={options.types} value={type} onChange={setType} />
            <FilterGroup label="Autor" options={options.authors} value={author} onChange={setAuthor} />
          </PopoverContent>
        </Popover>
        <Button primary onClick={() => navigate("/content/new/editor")}>Novo conteúdo</Button>
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
              <thead className="bg-muted text-xs text-muted-foreground"><tr>{["", "Título", "Tipo", "Idioma", "Autor", "Status", "Última atualização", "Publicação", "Versão", "Ações"].map((h) => <th key={h} className="p-3 font-medium">{h}</th>)}</tr></thead>
              <tbody>
                {rows.map((r) => (
                  <tr key={r.title} className="border-t border-border hover:bg-muted/40">
                    <td className="p-3"><input type="checkbox" onChange={(e) => setSel(e.target.checked ? [...sel, r.title] : sel.filter((x) => x !== r.title))} /></td>
                    <td className="p-3">{r.title}</td>
                    <td className="p-3">{r.type}</td>
                    <td className="p-3">{r.lang}</td>
                    <td className="p-3">{r.author}</td>
                    <td className="p-3"><ContentStatusBadge status={r.status} /></td>
                    <td className="p-3">{r.updatedAt}</td>
                    <td className="p-3">{r.publication}</td>
                    <td className="p-3">{r.version}</td>
                    <td className="p-3">
                      <div className="flex gap-1">
                        <Button onClick={() => navigate(`/content/${r.title.toLowerCase()}/editor`)}>Abrir</Button>
                        <Button onClick={() => navigate(`/content/${r.title.toLowerCase()}/preview`)}>Preview</Button>
                        <Button onClick={() => navigate(`/content/${r.title.toLowerCase()}/versions`)}>Histórico</Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="grid gap-3 lg:hidden">{rows.map((r) => <ContentCardMobile key={r.title} row={r} />)}</div>
        </>
      )}
    </>
  );
}
