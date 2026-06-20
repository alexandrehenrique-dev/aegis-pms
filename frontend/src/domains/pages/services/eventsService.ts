import { eventsByProduct } from "../mocks/events.mocks";
import { logApiCall } from "../../../shared/services/devLog";
import type { CreateEventRequest, PageEvent, UpdateEventRequest } from "../contracts/events";

// Store em memória só para a sessão do navegador — ver nota equivalente em
// domains/products/services/productsService.ts.
const eventsStore: PageEvent[] = Object.values(eventsByProduct).flat();

function nextEventId(productSlug: string): string {
  return `${productSlug}-ev-${Date.now()}`;
}

export const eventsService = {
  async listEvents(productSlug: string): Promise<PageEvent[]> {
    return eventsStore.filter((e) => e.productSlug === productSlug);
  },

  async createEvent(productSlug: string, req: CreateEventRequest): Promise<PageEvent> {
    logApiCall("POST", `/api/v1/products/${productSlug}/events`, req);
    const created: PageEvent = { id: nextEventId(productSlug), productSlug, ...req };
    eventsStore.push(created);
    return created;
  },

  async updateEvent(productSlug: string, eventId: string, req: UpdateEventRequest): Promise<PageEvent> {
    const event = eventsStore.find((e) => e.productSlug === productSlug && e.id === eventId);
    if (!event) throw { status: 404, message: `Evento ${eventId} não encontrado.` };
    logApiCall("PUT", `/api/v1/products/${productSlug}/events/${eventId}`, req);
    Object.assign(event, req);
    return { ...event };
  },

  async deleteEvent(productSlug: string, eventId: string): Promise<void> {
    const index = eventsStore.findIndex((e) => e.productSlug === productSlug && e.id === eventId);
    if (index < 0) return;
    logApiCall("DELETE", `/api/v1/products/${productSlug}/events/${eventId}`);
    eventsStore.splice(index, 1);
  },
};
