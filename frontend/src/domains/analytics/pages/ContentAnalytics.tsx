import { useState } from "react";
import { Button, Card, PageHeader } from "../../../shared/components/Primitives";
import { Popover, PopoverContent, PopoverTrigger } from "../../../shared/components/ui/popover";
import { ChartContainer } from "../components/AnalyticsBits";
import { ContentStatusBadge } from "../../content/components/ContentStatusBadge";

const LANGUAGES = ["PT-BR", "EN-US"];

function ContentAnalyticsTable() {
  const rows = [
    ["Home", "Página", "PT-BR", "12.840", "842", "Published", "há 2 dias", "Revisar SEO"],
    ["Galeria", "Bloco", "PT-BR", "5.420", "124", "In Review", "45 dias", "Atualizar conteúdo"],
    ["Sobre", "Página", "EN-US", "2.980", "88", "Draft", "12 dias", "Publicar tradução"],
    ["Agenda", "Artigo", "PT-BR", "1.402", "31", "Draft", "hoje", "Enviar revisão"],
  ];
  return (
    <Card>
      <h2 className="mb-3 text-lg font-semibold">Tabela de conteúdo</h2>
      <div className="hidden overflow-hidden rounded-xl border border-border lg:block">
        <table className="w-full text-left text-sm">
          <thead className="bg-muted text-xs text-muted-foreground"><tr>{["Conteúdo", "Tipo", "Idioma", "Views", "Conversões", "Status", "Última atualização", "Ação sugerida"].map((h) => <th className="p-3" key={h}>{h}</th>)}</tr></thead>
          <tbody>{rows.map((r) => <tr className="border-t border-border" key={r[0]}>{r.map((c, i) => <td className="p-3" key={c}>{i === 5 ? <ContentStatusBadge status={c} /> : c}</td>)}</tr>)}</tbody>
        </table>
      </div>
      <div className="grid gap-2 lg:hidden">{rows.map((r) => <div className="rounded-xl border border-border p-3" key={r[0]}><b>{r[0]}</b><p className="text-sm text-muted-foreground">{r[3]} views · {r[4]} conversões · {r[7]}</p></div>)}</div>
    </Card>
  );
}

export function ContentAnalytics() {
  const [lang, setLang] = useState<string | null>(null);
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
      <div className="mt-4"><ContentAnalyticsTable /></div>
    </>
  );
}
