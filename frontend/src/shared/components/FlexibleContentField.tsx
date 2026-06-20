import { Field, SelectLike } from "./Primitives";
import { MarkdownField } from "./MarkdownField";
import { MediaField } from "./MediaField";
import type { AssetTypeFilter } from "../../domains/assets/components/AssetPickerModal";

export type FlexibleContentKind = "text" | "image" | "pdf" | "audio" | "link";

export type FlexibleContentValue = {
  kind: FlexibleContentKind;
  text?: string;
  assetName?: string;
  url?: string;
};

const KIND_LABELS: Record<FlexibleContentKind, string> = { text: "Texto", image: "Imagem", pdf: "PDF", audio: "Áudio", link: "Link" };
const KIND_OPTIONS = Object.keys(KIND_LABELS) as FlexibleContentKind[];
const KIND_TYPE_FILTER: Record<FlexibleContentKind, AssetTypeFilter> = { text: "qualquer", image: "imagem", pdf: "PDF", audio: "áudio", link: "qualquer" };

/**
 * Campo de conteúdo flexível (Sprint 13, Tarefa B.4) — base reutilizável para
 * qualquer bloco que precise deixar o editor escolher, por campo, se o
 * conteúdo é Texto/Imagem/PDF/Áudio/Link. Não está ligado a nenhum bloco
 * específico hoje; existe pronta para blocos novos que precisem dela.
 */
export function FlexibleContentField({ label, value, onChange }: { label: string; value: FlexibleContentValue; onChange: (next: FlexibleContentValue) => void }) {
  const setKind = (kind: FlexibleContentKind) => onChange({ ...value, kind });

  return (
    <div className="space-y-2 md:col-span-2">
      <SelectLike
        label={`${label} — este campo aceita`}
        value={KIND_LABELS[value.kind]}
        options={KIND_OPTIONS.map((k) => KIND_LABELS[k])}
        onChange={(v) => setKind(KIND_OPTIONS.find((k) => KIND_LABELS[k] === v) ?? "text")}
      />
      {value.kind === "text" && (
        <MarkdownField label={label} value={value.text ?? ""} onChange={(text) => onChange({ ...value, text })} />
      )}
      {(value.kind === "image" || value.kind === "pdf" || value.kind === "audio") && (
        <MediaField label={label} value={value.assetName ?? ""} typeFilter={KIND_TYPE_FILTER[value.kind]} onChange={(assetName) => onChange({ ...value, assetName })} />
      )}
      {value.kind === "link" && (
        <Field label={`${label} — URL`} value={value.url ?? ""} onChange={(url) => onChange({ ...value, url })} />
      )}
    </div>
  );
}
