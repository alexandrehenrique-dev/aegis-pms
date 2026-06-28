import { eventsByProduct } from "../mocks/events.mocks";
import { logApiCall } from "../../../shared/services/devLog";
import { IS_API_MODE } from "../../../infra/apiMode";
import { apiClient } from "../../../shared/services/apiClient";
import type { CreateEventRequest, PageEvent, UpdateEventRequest } from "../contracts/events";

// Store em memória só para a sessão do navegador — ver nota equivalente em
// domains/products/services/productsService.ts.
const eventsStore: PageEvent[] = Object.values(eventsByProduct).flat();

function nextEventId(productSlug: string): string {
  return `${productSlug}-ev-${Date.now()}`;
}

export const eventsService = {
  async listEvents(productSlug: string): Promise<PageEvent[]> {
    if (IS_API_MODE) return apiClient.get<PageEvent[]>(`/products/${productSlug}/events`);
    return eventsStore.filter((e) => e.productSlug === productSlug);
  },

  async createEvent(productSlug: string, req: CreateEventRequest): Promise<PageEvent> {
    if (IS_API_MODE) return apiClient.post<PageEvent>(`/products/${productSlug}/events`, req);
    logApiCall("POST", `/api/v1/products/${productSlug}/events`, req);
    const created: PageEvent = { id: nextEventId(productSlug), productSlug, ...req };
    eventsStore.push(created);
    return created;
  },

  async updateEvent(productSlug: string, eventId: string, req: UpdateEventRequest): Promise<PageEvent> {
    if (IS_API_MODE) return apiClient.put<PageEvent>(`/products/${productSlug}/events/${eventId}`, req);
    const event = eventsStore.find((e) => e.productSlug === productSlug && e.id === eventId);
    if (!event) throw { status: 404, message: `Evento ${eventId} não encontrado.` };
    logApiCall("PUT", `/api/v1/products/${productSlug}/events/${eventId}`, req);
    Object.assign(event, req);
    return { ...event };
  },

  async deleteEvent(productSlug: string, eventId: string): Promise<void> {
    if (IS_API_MODE) return apiClient.delete(`/products/${productSlug}/events/${eventId}`);
    const index = eventsStore.findIndex((e) => e.productSlug === productSlug && e.id === eventId);
    if (index < 0) return;
    logApiCall("DELETE", `/api/v1/products/${productSlug}/events/${eventId}`);
    eventsStore.splice(index, 1);
  },
};
