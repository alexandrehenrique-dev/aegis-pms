import type { PageEvent } from "../contracts/events";

export const eventsByProduct: Record<string, PageEvent[]> = {
  "maestro-beton": [
    {
      id: "mb-ev-1",
      productSlug: "maestro-beton",
      title: "Casamento Ana & Pedro",
      date: "2026-07-12",
      location: "Espaço Jardins, São Paulo",
      type: "private",
      visibility: "private",
      description: "Cerimônia e festa de casamento, repertório personalizado.",
    },
    {
      id: "mb-ev-2",
      productSlug: "maestro-beton",
      title: "Show acústico de verão",
      date: "2026-08-03",
      location: "Praça Roosevelt, São Paulo",
      type: "public",
      visibility: "public",
      description: "Apresentação aberta ao público com repertório autoral.",
    },
  ],
  cmss: [
    {
      id: "cmss-ev-1",
      productSlug: "cmss",
      title: "Apresentação cívica de aniversário da cidade",
      date: "2026-09-07",
      location: "Praça Central, São Sebastião",
      type: "public",
      visibility: "public-summary",
      description: "Desfile e apresentação musical da banda em comemoração ao aniversário da cidade.",
    },
  ],
};
