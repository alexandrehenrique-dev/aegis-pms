import { useState } from "react";
import { Pencil } from "lucide-react";
import { Markdown } from "./Markdown";
import { MarkdownEditModal } from "./MarkdownEditModal";

/**
 * Campo de texto longo com suporte a markdown (Sprint 13, Tarefa B.2-3).
 * Em vez do `<textarea>` simples, mostra uma pré-visualização e um botão
 * "Editar texto" que abre a `MarkdownEditModal`.
 */
export function MarkdownField({ label, value, onChange }: { label: string; value: string; onChange: (next: string) => void }) {
  const [editing, setEditing] = useState(false);

  return (
    <div className="md:col-span-2">
      <div className="mb-1 flex items-center justify-between">
        <span className="text-sm font-medium">{label}</span>
        <button onClick={() => setEditing(true)} className="inline-flex items-center gap-1 text-xs text-primary hover:underline"><Pencil size={12} />Editar texto</button>
      </div>
      <div className="min-h-16 rounded-lg border border-border bg-card p-3">
        {value ? <Markdown>{value}</Markdown> : <p className="text-sm text-muted-foreground">Vazio — clique em "Editar texto" para escrever.</p>}
      </div>
      {editing && (
        <MarkdownEditModal
          title={label}
          value={value}
          onClose={() => setEditing(false)}
          onSave={(next) => {
            onChange(next);
            setEditing(false);
          }}
        />
      )}
    </div>
  );
}
