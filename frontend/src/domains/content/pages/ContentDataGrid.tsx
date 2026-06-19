import { useState } from "react";
import { useNavigate } from "react-router";
import { Filter, Search } from "lucide-react";
import { Badge, Button, EmptyState, PageHeader } from "../../../shared/components/Primitives";
import { contents } from "../mocks/content.mocks";
import { ContentStatusBadge } from "../components/ContentStatusBadge";
import { ContentCardMobile } from "../components/ContentCardMobile";

export function ContentDataGrid() {
  const navigate = useNavigate();
  const [q, setQ] = useState("");
  const [sel, setSel] = useState<string[]>([]);
  const rows = contents.filter((r) => r[0].toLowerCase().includes(q.toLowerCase()));

  return (
    <>
      <PageHeader title="Lista de Conteúdos" module="Conteúdo" desc="DataGrid operacional de páginas, seções, artigos, traduções e versões." badge="Conteúdo">
        <Button><Filter size={15} />Status / Idioma / Tipo / Autor</Button>
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
                  <tr key={r[0]} className="border-t border-border hover:bg-muted/40">
                    <td className="p-3"><input type="checkbox" onChange={(e) => setSel(e.target.checked ? [...sel, r[0]] : sel.filter((x) => x !== r[0]))} /></td>
                    {r.slice(0, 4).map((c) => <td key={c} className="p-3">{c}</td>)}
                    <td className="p-3"><ContentStatusBadge status={r[4]} /></td>
                    {r.slice(5).map((c) => <td key={c} className="p-3">{c}</td>)}
                    <td className="p-3">
                      <div className="flex gap-1">
                        <Button onClick={() => navigate(`/content/${r[0].toLowerCase()}/editor`)}>Abrir</Button>
                        <Button onClick={() => navigate(`/content/${r[0].toLowerCase()}/preview`)}>Preview</Button>
                        <Button onClick={() => navigate(`/content/${r[0].toLowerCase()}/versions`)}>Histórico</Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="grid gap-3 lg:hidden">{rows.map((r) => <ContentCardMobile key={r[0]} row={r} />)}</div>
        </>
      )}
    </>
  );
}
