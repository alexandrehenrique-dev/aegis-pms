import { eventsByProduct } from "../mocks/events.mocks";
import { products as productMocks } from "../../products/mocks/products.mocks";
import { logApiCall } from "../../../shared/services/devLog";
import { IS_API_MODE } from "../../../infra/apiMode";
import { apiClient } from "../../../shared/services/apiClient";
import { slugify } from "../../../shared/utils/slugify";
import type { CreateEventRequest, PageEvent, UpdateEventRequest } from "../contracts/events";

// Store em memória só para a sessão do navegador — ver nota equivalente em
// domains/products/services/productsService.ts.
const eventsStore: PageEvent[] = Object.values(eventsByProduct).flat();

function nextEventId(productSlug: string): string {
  return `${productSlug}-ev-${Date.now()}`;
}

function mockProductSlug(productId: string): string {
  const product = productMocks.find((p) => p.id === productId);
  return product ? slugify(product.name) : productId;
}

type EventDto = {
  id: string;
  title: string;
  datetime: string;
  location: string;
  type: PageEvent["type"];
  visibility: PageEvent["visibility"];
  description?: string | null;
  imageAssetId?: string | null;
};

function mapEvent(dto: EventDto, productId: string): PageEvent {
  return {
    id: dto.id,
    productSlug: productId,
    title: dto.title,
    date: dto.datetime,
    location: dto.location,
    type: dto.type,
    visibility: dto.visibility,
    description: dto.description ?? "",
    image: dto.imageAssetId ?? undefined,
  };
}

function toEventDto(req: CreateEventRequest | UpdateEventRequest) {
  return {
    title: req.title,
    datetime: req.date,
    location: req.location,
    type: req.type,
    visibility: req.visibility,
    description: req.description,
    imageAssetId: req.image || undefined,
  };
}

export const eventsService = {
  async listEvents(productId: string): Promise<PageEvent[]> {
    if (IS_API_MODE) {
      const dtos = await apiClient.get<EventDto[]>(`/products/${productId}/events`);
      return dtos.map((dto) => mapEvent(dto, productId));
    }
    const productSlug = mockProductSlug(productId);
    return eventsStore.filter((e) => e.productSlug === productSlug);
  },

  async createEvent(productId: string, req: CreateEventRequest): Promise<PageEvent> {
    if (IS_API_MODE) {
      const dto = await apiClient.post<EventDto>(`/products/${productId}/events`, toEventDto(req));
      return mapEvent(dto, productId);
    }
    const productSlug = mockProductSlug(productId);
    logApiCall("POST", `/api/v1/products/${productSlug}/events`, req);
    const created: PageEvent = { id: nextEventId(productSlug), productSlug, ...req };
    eventsStore.push(created);
    return created;
  },

  async updateEvent(productId: string, eventId: string, req: UpdateEventRequest): Promise<PageEvent> {
    if (IS_API_MODE) {
      const dto = await apiClient.put<EventDto>(`/products/${productId}/events/${eventId}`, toEventDto(req));
      return mapEvent(dto, productId);
    }
    const productSlug = mockProductSlug(productId);
    const event = eventsStore.find((e) => e.productSlug === productSlug && e.id === eventId);
    if (!event) throw { status: 404, message: `Evento ${eventId} não encontrado.` };
    logApiCall("PUT", `/api/v1/products/${productSlug}/events/${eventId}`, req);
    Object.assign(event, req);
    return { ...event };
  },

  async deleteEvent(productId: string, eventId: string): Promise<void> {
    if (IS_API_MODE) return apiClient.delete(`/products/${productId}/events/${eventId}`);
    const productSlug = mockProductSlug(productId);
    const index = eventsStore.findIndex((e) => e.productSlug === productSlug && e.id === eventId);
    if (index < 0) return;
    logApiCall("DELETE", `/api/v1/products/${productSlug}/events/${eventId}`);
    eventsStore.splice(index, 1);
  },
};
