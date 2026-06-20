import { useEffect, useState } from "react";
import { Badge } from "../../../shared/components/Primitives";
import { eventsService } from "../services/eventsService";
import type { PageEvent } from "../contracts/events";

/**
 * Seleção de eventos já existentes (Sprint 13, Tarefa I.1) — antes, o bloco
 * `event-list` só oferecia "Gerenciar eventos" (criar do zero). Agora, ao
 * adicionar/editar o bloco, mostra primeiro os eventos já cadastrados no
 * produto (de qualquer página/fonte), com checkbox para escolher quais
 * aparecem especificamente neste bloco — cobre o caso de um evento
 * aparecer em mais de um lugar (ex.: agenda da Home E agenda dedicada).
 */
export function EventSelector({ productSlug, selectedIds, onChange }: { productSlug: string; selectedIds: string[]; onChange: (ids: string[]) => void }) {
  const [events, setEvents] = useState<PageEvent[]>([]);

  useEffect(() => {
    eventsService.listEvents(productSlug).then(setEvents);
  }, [productSlug]);

  const toggle = (id: string) => onChange(selectedIds.includes(id) ? selectedIds.filter((i) => i !== id) : [...selectedIds, id]);

  if (events.length === 0) {
    return <p className="text-sm text-muted-foreground">Nenhum evento cadastrado neste produto ainda — use "Gerenciar eventos" para criar o primeiro.</p>;
  }

  return (
    <div className="space-y-1.5">
      <p className="text-xs text-muted-foreground">Eventos já cadastrados no produto — marque os que devem aparecer neste bloco.</p>
      {events.map((ev) => (
        <label key={ev.id} className="flex items-center gap-2 rounded-lg border border-border p-2 text-sm">
          <input type="checkbox" checked={selectedIds.includes(ev.id)} onChange={() => toggle(ev.id)} className="accent-primary" />
          <span className="flex-1">{ev.title} <span className="text-xs text-muted-foreground">— {ev.date}</span></span>
          <Badge tone={ev.type === "private" ? "amber" : "green"}>{ev.type}</Badge>
        </label>
      ))}
    </div>
  );
}
