import { useEffect, useState } from "react";
import { Plus, Trash2 } from "lucide-react";
import { Drawer, DrawerContent, DrawerHeader, DrawerTitle, DrawerDescription, DrawerFooter } from "../../../shared/components/ui/drawer";
import { Button, Badge, Field, SelectLike } from "../../../shared/components/Primitives";
import { ConfirmDialog } from "../../../shared/components/ConfirmDialog";
import { toast } from "../../../core/notifications/toast";
import { eventsService } from "../services/eventsService";
import type { CreateEventRequest, EventVisibility, PageEvent } from "../contracts/events";

const VISIBILITY_OPTIONS: EventVisibility[] = ["public", "public-summary", "private"];
const TYPE_OPTIONS: PageEvent["type"][] = ["public", "private"];

const EMPTY_EVENT: CreateEventRequest = { title: "", date: "", location: "", type: "public", visibility: "public", description: "" };

/** Preview de como o evento aparece publicamente, respeitando a regra de privacidade do contrato Maestro Beton (Seção 12). */
function PublicPreview({ event }: { event: CreateEventRequest }) {
  if (event.visibility === "private") {
    return <p className="text-sm text-muted-foreground">Data reservada</p>;
  }
  if (event.visibility === "public-summary") {
    return <p className="text-sm"><b>{event.title || "Evento"}</b> — {event.date || "data a definir"}</p>;
  }
  return (
    <div className="text-sm">
      <p className="font-medium">{event.title || "Evento"}</p>
      <p className="text-muted-foreground">{event.date || "data a definir"} · {event.location || "local a definir"}</p>
      <p className="mt-1 text-muted-foreground">{event.description}</p>
    </div>
  );
}

function EventForm({ initial, onSave, onCancel, saving }: { initial: CreateEventRequest; onSave: (req: CreateEventRequest) => void; onCancel: () => void; saving: boolean }) {
  const [draft, setDraft] = useState<CreateEventRequest>(initial);
  const patch = (p: Partial<CreateEventRequest>) => setDraft((d) => ({ ...d, ...p }));

  return (
    <div className="space-y-3">
      <Field label="Título" value={draft.title} onChange={(v) => patch({ title: v })} />
      <Field label="Data" type="date" value={draft.date} onChange={(v) => patch({ date: v })} />
      <Field label="Local" value={draft.location} onChange={(v) => patch({ location: v })} />
      <SelectLike label="Tipo" value={draft.type} options={TYPE_OPTIONS} onChange={(v) => patch({ type: v as PageEvent["type"] })} />
      <SelectLike label="Visibilidade" value={draft.visibility} options={VISIBILITY_OPTIONS} onChange={(v) => patch({ visibility: v as EventVisibility })} />
      <Field label="Descrição" value={draft.description} onChange={(v) => patch({ description: v })} textarea />
      <div className="rounded-lg border border-border bg-muted/30 p-3">
        <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-muted-foreground">Preview público</p>
        <PublicPreview event={draft} />
      </div>
      <div className="flex justify-end gap-2">
        <Button onClick={onCancel}>Cancelar</Button>
        <Button primary onClick={() => onSave(draft)} disabled={saving || !draft.title.trim()}>{saving ? "Salvando..." : "Salvar evento"}</Button>
      </div>
    </div>
  );
}

/**
 * Drawer de gestão de eventos do bloco `event-list` (Sprint 12, Tarefa E) —
 * o bloco referencia uma fonte externa (`source.contentType === "evento"`)
 * mas, até esta sprint, não havia nenhum lugar para criar/editar/excluir
 * esses eventos. Abre direto do editor do bloco, sem saltar de tela.
 */
export function EventsManagerDrawer({ productSlug, open, onOpenChange }: { productSlug: string; open: boolean; onOpenChange: (open: boolean) => void }) {
  const [events, setEvents] = useState<PageEvent[]>([]);
  const [editing, setEditing] = useState<PageEvent | "new" | null>(null);
  const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const refresh = () => { eventsService.listEvents(productSlug).then(setEvents); };
  useEffect(() => {
    if (open) refresh();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, productSlug]);

  const handleSave = async (req: CreateEventRequest) => {
    setSaving(true);
    try {
      if (editing && editing !== "new") {
        await eventsService.updateEvent(productSlug, editing.id, req);
        toast.success("Evento atualizado", { description: req.title });
      } else {
        await eventsService.createEvent(productSlug, req);
        toast.success("Evento criado", { description: req.title });
      }
      setEditing(null);
      refresh();
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!pendingDeleteId) return;
    await eventsService.deleteEvent(productSlug, pendingDeleteId);
    toast.success("Evento removido");
    setPendingDeleteId(null);
    refresh();
  };

  const pendingDeleteEvent = events.find((e) => e.id === pendingDeleteId) ?? null;

  return (
    <Drawer open={open} onOpenChange={onOpenChange} direction="right">
      <DrawerContent className="w-full sm:max-w-md">
        <DrawerHeader>
          <DrawerTitle>Gerenciar eventos</DrawerTitle>
          <DrawerDescription>Eventos referenciados por este bloco de agenda — criar, editar e excluir sem saltar de tela.</DrawerDescription>
        </DrawerHeader>
        <div className="flex-1 overflow-auto px-4 pb-4">
          {pendingDeleteEvent && (
            <ConfirmDialog
              title={`Excluir evento "${pendingDeleteEvent.title}"?`}
              desc="Esta ação é irreversível."
              danger
              onCancel={() => setPendingDeleteId(null)}
              onConfirm={handleDelete}
            />
          )}
          {editing ? (
            <EventForm
              initial={editing === "new" ? EMPTY_EVENT : editing}
              onSave={handleSave}
              onCancel={() => setEditing(null)}
              saving={saving}
            />
          ) : (
            <div className="space-y-2">
              {events.map((ev) => (
                <div key={ev.id} className="rounded-lg border border-border p-3">
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <p className="font-medium">{ev.title}</p>
                      <p className="text-xs text-muted-foreground">{ev.date} · {ev.location}</p>
                      <div className="mt-1 flex gap-1">
                        <Badge tone={ev.type === "private" ? "amber" : "green"}>{ev.type}</Badge>
                        <Badge>{ev.visibility}</Badge>
                      </div>
                    </div>
                    <div className="flex gap-1">
                      <Button onClick={() => setEditing(ev)}>Editar</Button>
                      <button onClick={() => setPendingDeleteId(ev.id)} aria-label="Excluir evento" className="rounded-lg p-2 text-muted-foreground hover:bg-destructive/10 hover:text-destructive"><Trash2 size={14} /></button>
                    </div>
                  </div>
                </div>
              ))}
              {events.length === 0 && <p className="text-sm text-muted-foreground">Nenhum evento ainda.</p>}
            </div>
          )}
        </div>
        {!editing && (
          <DrawerFooter>
            <Button primary onClick={() => setEditing("new")}><Plus size={15} />Novo evento</Button>
          </DrawerFooter>
        )}
      </DrawerContent>
    </Drawer>
  );
}
