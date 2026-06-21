import { useState } from "react";
import { Image as ImageIcon, X } from "lucide-react";
import { Button, Field } from "./Primitives";
import { AssetPickerModal, type AssetTypeFilter } from "../../domains/assets/components/AssetPickerModal";
import type { AssetSummary } from "../../domains/assets/contracts/responses";

const FILTER_LABEL: Record<AssetTypeFilter, string> = { imagem: "imagem", PDF: "PDF", áudio: "áudio", "vídeo": "vídeo", qualquer: "arquivo" };

/** Sugere um `alt` legível a partir do nome do arquivo (Sprint 13, Tarefa C.2): remove extensão e separadores, capitaliza. */
export function suggestAltFromFilename(filename: string): string {
  const withoutExt = filename.replace(/\.[a-z0-9]+$/i, "");
  const words = withoutExt.replace(/[-_]+/g, " ").trim();
  return words.charAt(0).toUpperCase() + words.slice(1);
}

/**
 * Campo de mídia genérico (Sprint 13, Tarefa C) — botão "Selecionar" abre o
 * `AssetPickerModal` real em vez de um campo de texto puro; "Trocar" reabre
 * o picker, "Remover" limpa a seleção.
 */
export function MediaField({ label, value, onChange, typeFilter = "qualquer", onSelectAsset }: {
  label: string;
  value: string;
  onChange: (assetName: string) => void;
  typeFilter?: AssetTypeFilter;
  onSelectAsset?: (asset: AssetSummary) => void;
}) {
  const [pickerOpen, setPickerOpen] = useState(false);

  return (
    <div>
      <span className="mb-1 block text-sm font-medium">{label}</span>
      {value ? (
        <div className="flex items-center justify-between gap-2 rounded-lg border border-border p-2">
          <span className="flex items-center gap-2 truncate text-sm"><ImageIcon size={14} className="shrink-0 text-muted-foreground" />{value}</span>
          <div className="flex shrink-0 gap-1">
            <Button onClick={() => setPickerOpen(true)}>Trocar</Button>
            <button onClick={() => onChange("")} aria-label="Remover seleção" className="rounded-lg p-1.5 text-muted-foreground transition hover:bg-destructive/10 hover:text-destructive"><X size={14} /></button>
          </div>
        </div>
      ) : (
        <Button onClick={() => setPickerOpen(true)}>Selecionar {FILTER_LABEL[typeFilter]}</Button>
      )}
      <AssetPickerModal
        open={pickerOpen}
        typeFilter={typeFilter}
        lockFilter={typeFilter !== "qualquer"}
        onClose={() => setPickerOpen(false)}
        onSelect={(asset) => {
          onChange(asset.name);
          onSelectAsset?.(asset);
          setPickerOpen(false);
        }}
      />
    </div>
  );
}

/** Par `src`/`alt` (Sprint 13, Tarefa C.2) — ao selecionar a imagem, sugere `alt` a partir do nome do arquivo somente se ainda estiver vazio. */
export function ImageFieldEditor({ src, alt, onChangeSrc, onChangeAlt }: { src: string; alt: string; onChangeSrc: (v: string) => void; onChangeAlt: (v: string) => void }) {
  return (
    <div className="grid gap-3 md:grid-cols-2">
      <MediaField
        label="Imagem"
        value={src}
        typeFilter="imagem"
        onChange={onChangeSrc}
        onSelectAsset={(asset) => {
          if (!alt) onChangeAlt(suggestAltFromFilename(asset.name));
        }}
      />
      <Field label="Texto alternativo (alt)" value={alt} onChange={onChangeAlt} />
    </div>
  );
}
