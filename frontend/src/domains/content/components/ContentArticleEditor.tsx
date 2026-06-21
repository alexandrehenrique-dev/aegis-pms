import { Card, Field } from "../../../shared/components/Primitives";
import { MarkdownField } from "../../../shared/components/MarkdownField";
import { MediaField } from "../../../shared/components/MediaField";
import { EntityPicker } from "../../knowledge/components/EntityPicker";
import { Popover, PopoverContent, PopoverTrigger } from "../../../shared/components/ui/popover";
import { Button } from "../../../shared/components/Primitives";
import { Link2 } from "lucide-react";
import type { ContentRow } from "../contracts/responses";
import type { KGNode } from "../../knowledge/mocks/knowledge.mocks";

const MUSIC_LINKED_TYPES = new Set(["Post", "Manifesto", "Reflexão", "Poema"]);

/**
 * Painel de metadados condicional por `type` (Sprint 15, Tarefa A.2) — antes
 * disto, o editor de conteúdo era 100% genérico (motor de blocos de `pages`
 * reaproveitado por engano, ver `ContentEditor.tsx`) e nenhum dos campos
 * específicos do ADR-0012 (isbn, pdfUrl, musicReferenceId...) tinha onde
 * aparecer.
 */
function MetadataPanel({ content, knowledgeGraphEnabled, onPatchMetadata }: {
  content: ContentRow;
  knowledgeGraphEnabled: boolean;
  onPatchMetadata: (patch: Record<string, unknown>) => void;
}) {
  const metadata = content.metadata ?? {};

  if (MUSIC_LINKED_TYPES.has(content.type)) {
    const musicReferenceId = typeof metadata.musicReferenceId === "string" ? metadata.musicReferenceId : "";
    const coverImage = typeof metadata.coverImage === "string" ? metadata.coverImage : "";
    const handleLinkMusic = (node: KGNode) => onPatchMetadata({ musicReferenceId: node.id, musicReferenceLabel: node.label });
    return (
      <div className="grid gap-3 md:grid-cols-2">
        <MediaField label="Imagem de capa" value={coverImage} typeFilter="imagem" onChange={(v) => onPatchMetadata({ coverImage: v })} />
        {knowledgeGraphEnabled ? (
          <div>
            <span className="mb-1 block text-sm font-medium">Vincular música</span>
            {musicReferenceId ? (
              <div className="flex items-center justify-between gap-2 rounded-lg border border-border p-2 text-sm">
                <span className="truncate">{typeof metadata.musicReferenceLabel === "string" ? metadata.musicReferenceLabel : musicReferenceId}</span>
                <Button onClick={() => onPatchMetadata({ musicReferenceId: undefined, musicReferenceLabel: undefined })}>Remover</Button>
              </div>
            ) : (
              <Popover>
                <PopoverTrigger asChild><Button><Link2 size={14} />Buscar entidade musical</Button></PopoverTrigger>
                <PopoverContent><EntityPicker onSelect={handleLinkMusic} /></PopoverContent>
              </Popover>
            )}
          </div>
        ) : (
          <p className="self-end text-xs text-muted-foreground">Vincular música exige o módulo Knowledge Graph habilitado neste produto.</p>
        )}
      </div>
    );
  }

  if (content.type === "Livro") {
    const isbn = typeof metadata.isbn === "string" ? metadata.isbn : "";
    const pdfUrl = typeof metadata.pdfUrl === "string" ? metadata.pdfUrl : "";
    const epubUrl = typeof metadata.epubUrl === "string" ? metadata.epubUrl : "";
    const physicalAvailable = Boolean(metadata.physicalAvailable);
    return (
      <div className="grid gap-3 md:grid-cols-2">
        <Field label="ISBN" value={isbn} onChange={(v) => onPatchMetadata({ isbn: v })} />
        <label className="flex items-center gap-2 self-end text-sm">
          <input type="checkbox" checked={physicalAvailable} onChange={(e) => onPatchMetadata({ physicalAvailable: e.target.checked })} className="accent-primary" />
          Disponível em versão física
        </label>
        <MediaField label="Arquivo PDF" value={pdfUrl} typeFilter="PDF" onChange={(v) => onPatchMetadata({ pdfUrl: v })} />
        <MediaField label="Arquivo EPUB" value={epubUrl} typeFilter="qualquer" onChange={(v) => onPatchMetadata({ epubUrl: v })} />
      </div>
    );
  }

  if (content.type === "Playlist") {
    const url = typeof metadata.url === "string" ? metadata.url : "";
    return <Field label="URL da playlist (Spotify)" value={url} onChange={(v) => onPatchMetadata({ url: v })} />;
  }

  return <p className="text-sm text-muted-foreground">Este tipo de conteúdo não tem metadados adicionais.</p>;
}

export function ContentArticleEditor({ content, knowledgeGraphEnabled, onChange }: {
  content: ContentRow;
  knowledgeGraphEnabled: boolean;
  onChange: (patch: Partial<ContentRow>) => void;
}) {
  const handlePatchMetadata = (patch: Record<string, unknown>) => onChange({ metadata: { ...content.metadata, ...patch } });

  return (
    <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
      <Card>
        <Field label="Título" value={content.title} onChange={(v) => onChange({ title: v })} />
        <div className="mt-4">
          <MarkdownField label="Corpo (markdown)" value={content.body ?? ""} onChange={(v) => onChange({ body: v })} />
        </div>
        <p className="mt-2 text-xs text-muted-foreground">
          Use <code>{"{{kg-ref:nodeId:Label}}"}</code> dentro do corpo para criar uma referência ao Knowledge Graph — a conexão é criada automaticamente ao salvar.
        </p>
      </Card>
      <Card>
        <h2 className="mb-3 text-lg font-semibold">Metadados — {content.type}</h2>
        <MetadataPanel content={content} knowledgeGraphEnabled={knowledgeGraphEnabled} onPatchMetadata={handlePatchMetadata} />
      </Card>
    </div>
  );
}
