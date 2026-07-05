import { useState } from "react";
import { Button, Card, EmptyState, PageHeader, PartialErrorWidget, SkeletonLines } from "../../../shared/components/Primitives";
import { Popover, PopoverContent, PopoverTrigger } from "../../../shared/components/ui/popover";
import { ChartContainer } from "../components/AnalyticsBits";
import { ContentStatusBadge } from "../../content/components/ContentStatusBadge";
import { contentService } from "../../content/services/contentService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";
import type { ContentRow } from "../../content/contracts/responses";

const LANGUAGES = ["PT-BR", "EN-US"];

function ContentAnalyticsTable({ rows }: { rows: ContentRow[] }) {
  return (
    <Card>
      <h2 className="mb-3 text-lg font-semibold">Tabela de conteúdo</h2>
      <div className="hidden overflow-hidden rounded-xl border border-border lg:block">
        <table className="w-full text-left text-sm">
          <thead className="bg-muted text-xs text-muted-foreground"><tr>{["Conteúdo", "Tipo", "Idioma", "Views", "Conversões", "Status", "Última atualização", "Ação sugerida"].map((h) => <th className="p-3" key={h}>{h}</th>)}</tr></thead>
          <tbody>{rows.map((row) => <tr className="border-t border-border" key={row.id}><td className="p-3">{row.title}</td><td className="p-3">{row.type}</td><td className="p-3">{row.lang}</td><td className="p-3">—</td><td className="p-3">—</td><td className="p-3"><ContentStatusBadge status={row.status} /></td><td className="p-3">{row.updatedAt}</td><td className="p-3">{row.status === "Draft" ? "Enviar revisão" : "Acompanhar"}</td></tr>)}</tbody>
        </table>
      </div>
      <div className="grid gap-2 lg:hidden">{rows.map((row) => <div className="rounded-xl border border-border p-3" key={row.id}><b>{row.title}</b><p className="text-sm text-muted-foreground">views — · conversões — · {row.status}</p></div>)}</div>
    </Card>
  );
}

export function ContentAnalytics() {
  const { product } = useCurrentProduct();
  const productId = product?.id ?? "";
  const { data: contents, loading, error } = useAsyncData(() => (productId ? contentService.listContent(productId) : Promise.resolve([])), [productId]);
  const [lang, setLang] = useState<string | null>(null);
  const visibleContents = lang ? (contents ?? []).filter((item) => item.lang === lang) : (contents ?? []);
  return (
    <>
      <PageHeader title="Content Analytics" module="Analytics" desc="Mede impacto, queda, atualização, SEO e status editorial do conteúdo." badge="Conteúdo">
        <Popover>
          <PopoverTrigger asChild><Button>Filtrar idioma{lang ? `: ${lang}` : ""}</Button></PopoverTrigger>
          <PopoverContent>
            <div className="flex flex-col gap-1">
              <Button onClick={() => setLang(null)} primary={!lang}>Todos</Button>
              {LANGUAGES.map((l) => <Button key={l} onClick={() => setLang(l)} primary={lang === l}>{l}</Button>)}
            </div>
          </PopoverContent>
        </Popover>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-2">
        <ChartContainer title="Visitas por página" type="bar" />
        <ChartContainer title="Evolução de publicações" type="line" />
        <ChartContainer title="Status editorial" type="donut" />
        <ChartContainer title="Revisados vs publicados" type="stacked" />
      </div>
      <div className="mt-4">
        {loading ? <SkeletonLines /> : error ? <PartialErrorWidget /> : visibleContents.length === 0 ? (
          <EmptyState title="Sem conteúdo para analisar" description="Quando o produto tiver conteúdo, a tabela aparece aqui." />
        ) : (
          <ContentAnalyticsTable rows={visibleContents} />
        )}
      </div>
    </>
  );
}
