export type EventVisibility = "public" | "public-summary" | "private";

/** Espelha `agenda.json` do contrato Maestro Beton (Sprint 12, Tarefa E.1). */
export type PageEvent = {
  id: string;
  productSlug: string;
  title: string;
  date: string;
  location: string;
  type: "public" | "private";
  visibility: EventVisibility;
  description: string;
  image?: string;
};

export type CreateEventRequest = Omit<PageEvent, "id" | "productSlug">;
export type UpdateEventRequest = Partial<CreateEventRequest>;
