import type { BlockType, Section, SectionSource } from "./responses";

export type CreateSectionRequest = {
  type: BlockType;
  label: string;
  content: Record<string, unknown>;
  settings?: Record<string, unknown>;
  source?: SectionSource;
};

export type UpdateSectionRequest = Partial<Pick<Section, "label" | "content" | "settings" | "source">>;

export type ReorderSectionsRequest = {
  sectionIds: string[];
};
