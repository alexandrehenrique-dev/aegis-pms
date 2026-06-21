import { Field, SelectLike } from "../../../shared/components/Primitives";
import { MediaField } from "../../../shared/components/MediaField";
import { isValidYoutubeUrl } from "../videoUrl";

const VIDEO_SOURCES = ["upload", "youtube"] as const;
type VideoSource = (typeof VIDEO_SOURCES)[number];

/**
 * Editor do `BlockType: "video"` (Sprint 18, Tarefa A.3) — cópia estrutural
 * do `AudioBlockEditor`, trocando o embed do Spotify por YouTube. Upload
 * filtra o picker de assets por `typeFilter="vídeo"`; a URL do YouTube é
 * validada contra a mesma regex documentada no backend (etapa 21) antes de
 * salvar, nunca só no servidor.
 */
export function VideoBlockEditor({ content, onChange }: { content: Record<string, unknown>; onChange: (patch: Record<string, unknown>) => void }) {
  const source = (typeof content.source === "string" ? content.source : "upload") as VideoSource;
  const autoplay = Boolean(content.autoplay);
  const youtubeUrl = typeof content.youtubeUrl === "string" ? content.youtubeUrl : "";
  const youtubeUrlInvalid = youtubeUrl.trim() !== "" && !isValidYoutubeUrl(youtubeUrl);

  return (
    <div className="mt-3 space-y-3">
      <SelectLike label="Fonte do vídeo" value={source} options={[...VIDEO_SOURCES]} onChange={(v) => onChange({ source: v })} />
      {source === "upload" ? (
        <MediaField label="Arquivo de vídeo" value={typeof content.fileAssetId === "string" ? content.fileAssetId : ""} typeFilter="vídeo" onChange={(name) => onChange({ fileAssetId: name })} />
      ) : (
        <div>
          <Field label="URL do YouTube" value={youtubeUrl} onChange={(v) => onChange({ youtubeUrl: v })} />
          <p className="mt-1 text-xs text-muted-foreground">Ex.: https://www.youtube.com/watch?v=... ou https://youtu.be/...</p>
          {youtubeUrlInvalid && <p className="mt-1 text-xs text-destructive">URL do YouTube inválida — confira o formato acima.</p>}
        </div>
      )}
      <label className="flex items-center gap-2 text-sm">
        <input type="checkbox" checked={autoplay} onChange={(e) => onChange({ autoplay: e.target.checked })} className="accent-primary" />
        Reproduzir automaticamente ao abrir a página (recomendamos manter desligado)
      </label>
    </div>
  );
}
