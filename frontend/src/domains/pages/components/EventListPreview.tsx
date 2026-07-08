import { useEffect, useState } from "react";
import { formatDateTime } from "../../../shared/utils/formatDateTime";
import { eventsService } from "../services/eventsService";

type SelectedEvent = { id: string; title: string; date: string; location: string; type: string };

function asStr(v: unknown, fallback = ""): string {
  return typeof v === "string" ? v : fallback;
}

function asArray(v: unknown): Record<string, unknown>[] {
  return Array.isArray(v) ? (v as Record<string, unknown>[]) : [];
}

/**
 * I.1 (BUG-SPRINT-05) — `content.selectedEvents` guarda os objetos completos
 * escolhidos no editor, mas pode chegar vazio (ex.: navegação direta ao
 * preview logo após reload) enquanto `content.selectedEventIds` ainda tem os
 * ids selecionados. Nesse caso, re-busca os eventos do produto e filtra
 * pelos ids — em vez de exibir "nenhum evento selecionado" para uma seleção
 * que na verdade existe, só não foi hidratada.
 */
export function EventListPreview({ content, productSlug }: { content: Record<string, unknown>; productSlug?: string }) {
  const storedEvents = asArray(content.selectedEvents) as unknown as SelectedEvent[];
  const storedIds = Array.isArray(content.selectedEventIds) ? (content.selectedEventIds as string[]) : [];
  const [events, setEvents] = useState<SelectedEvent[]>(storedEvents);
  const [hydrating, setHydrating] = useState(false);

  useEffect(() => {
    if (storedEvents.length > 0 || storedIds.length === 0 || !productSlug) {
      setEvents(storedEvents);
      return;
    }
    let active = true;
    setHydrating(true);
    eventsService.listEvents(productSlug)
      .then((all) => {
        if (!active) return;
        setEvents(all.filter((e) => storedIds.includes(e.id)).map((ev) => ({ id: ev.id, title: ev.title, date: ev.date, location: ev.location, type: ev.type })));
      })
      .catch(() => { if (active) setEvents([]); })
      .finally(() => { if (active) setHydrating(false); });
    return () => { active = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [productSlug, storedIds.join(",")]);

  return (
    <div className="p-6">
      <h3 className="text-xl font-semibold">{asStr(content.title, "Agenda")}</h3>
      {hydrating ? (
        <p className="mt-2 text-sm text-muted-foreground">Carregando eventos selecionados...</p>
      ) : events.length > 0 ? (
        <div className="mt-3 space-y-2">
          {events.map((ev, i) => (
            <div key={ev.id ?? i} className="rounded-lg border border-border p-3">
              <p className="font-medium">{asStr(ev.title, "Evento")}</p>
              <p className="text-sm text-muted-foreground">{formatDateTime(asStr(ev.date))} · {asStr(ev.location, "local a definir")}</p>
            </div>
          ))}
        </div>
      ) : (
        <p className="mt-2 text-sm text-muted-foreground">
          {storedIds.length > 0
            ? `${storedIds.length} evento(s) selecionado(s) — não foi possível carregá-los agora.`
            : "Nenhum evento selecionado ainda (ver editor)."}
        </p>
      )}
    </div>
  );
}
