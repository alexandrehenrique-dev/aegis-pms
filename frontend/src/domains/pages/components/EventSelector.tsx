import { useEffect, useMemo, useState } from "react";
import { Badge } from "../../../shared/components/Primitives";
import { formatDateTime } from "../../../shared/utils/formatDateTime";
import { eventsService } from "../services/eventsService";
import type { PageEvent } from "../contracts/events";

type PeriodFilter = "todos" | "semana" | "mes" | "ano";

const PERIOD_OPTIONS: { key: PeriodFilter; label: string }[] = [
  { key: "todos", label: "Todos" },
  { key: "semana", label: "Esta semana" },
  { key: "mes", label: "Este mês" },
  { key: "ano", label: "Este ano" },
];

/** Segunda-feira (00:00) da semana corrente, para alinhar o filtro "Esta semana" ao calendário em vez de uma janela rolante de 7 dias. */
function startOfWeek(now: Date): Date {
  const start = new Date(now);
  const isoWeekday = (now.getDay() + 6) % 7; // 0 = segunda
  start.setDate(now.getDate() - isoWeekday);
  start.setHours(0, 0, 0, 0);
  return start;
}

function matchesPeriod(dateStr: string, period: PeriodFilter, now: Date): boolean {
  if (period === "todos") return true;
  const date = new Date(dateStr);
  if (Number.isNaN(date.getTime())) return true;
  if (period === "ano") return date.getFullYear() === now.getFullYear();
  if (period === "mes") return date.getFullYear() === now.getFullYear() && date.getMonth() === now.getMonth();
  const weekStart = startOfWeek(now);
  const weekEnd = new Date(weekStart);
  weekEnd.setDate(weekStart.getDate() + 7);
  return date >= weekStart && date < weekEnd;
}

/**
 * Seleção de eventos já existentes (Sprint 13, Tarefa I.1) — antes, o bloco
 * `event-list` só oferecia "Gerenciar eventos" (criar do zero). Agora, ao
 * adicionar/editar o bloco, mostra primeiro os eventos já cadastrados no
 * produto (de qualquer página/fonte), com checkbox para escolher quais
 * aparecem especificamente neste bloco — cobre o caso de um evento
 * aparecer em mais de um lugar (ex.: agenda da Home E agenda dedicada).
 * `refreshKey` é incrementado pelo chamador quando `EventsManagerDrawer`
 * fecha — sem isso, um evento criado ali não aparecia aqui: os dois
 * componentes buscam a lista de forma independente, e este só buscava
 * uma vez, ao montar.
 */
export function EventSelector({ productSlug, selectedIds, onChange, refreshKey = 0 }: {
  productSlug: string;
  selectedIds: string[];
  /** `selectedEvents` traz os dados completos dos eventos marcados (E.7.2, BUG-SPRINT consolidado) — permite ao `BlockRenderer` mostrar título/data/local sem um fetch adicional no preview. */
  onChange: (ids: string[], selectedEvents: PageEvent[]) => void;
  refreshKey?: number;
}) {
  const [events, setEvents] = useState<PageEvent[]>([]);
  const [period, setPeriod] = useState<PeriodFilter>("todos");

  useEffect(() => {
    eventsService.listEvents(productSlug).then(setEvents);
  }, [productSlug, refreshKey]);

  const filtered = useMemo(() => {
    const now = new Date();
    return events.filter((ev) => matchesPeriod(ev.date, period, now));
  }, [events, period]);

  const toggle = (id: string) => {
    const nextIds = selectedIds.includes(id) ? selectedIds.filter((i) => i !== id) : [...selectedIds, id];
    onChange(nextIds, events.filter((ev) => nextIds.includes(ev.id)));
  };

  if (events.length === 0) {
    return <p className="text-sm text-muted-foreground">Nenhum evento cadastrado neste produto ainda — use "Gerenciar eventos" para criar o primeiro.</p>;
  }

  return (
    <div className="space-y-1.5">
      <p className="text-xs text-muted-foreground">Eventos já cadastrados no produto — marque os que devem aparecer neste bloco.</p>
      <div className="flex flex-wrap gap-1">
        {PERIOD_OPTIONS.map((opt) => (
          <button
            key={opt.key}
            type="button"
            onClick={() => setPeriod(opt.key)}
            className={`rounded-full border px-2.5 py-1 text-xs transition ${period === opt.key ? "border-primary bg-primary/5 text-primary" : "border-border text-muted-foreground hover:bg-muted"}`}
          >
            {opt.label}
          </button>
        ))}
      </div>
      {filtered.length === 0 ? (
        <p className="text-sm text-muted-foreground">Nenhum evento neste período.</p>
      ) : (
        filtered.map((ev) => (
          <label key={ev.id} className="flex items-center gap-2 rounded-lg border border-border p-2 text-sm">
            <input type="checkbox" checked={selectedIds.includes(ev.id)} onChange={() => toggle(ev.id)} className="accent-primary" />
            <span className="flex-1">{ev.title} <span className="text-xs text-muted-foreground">— {formatDateTime(ev.date)}</span></span>
            <Badge tone={ev.type === "private" ? "amber" : "green"}>{ev.type}</Badge>
          </label>
        ))
      )}
    </div>
  );
}
