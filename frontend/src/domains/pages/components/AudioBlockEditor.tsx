import { Field, SelectLike } from "../../../shared/components/Primitives";
import { MediaField } from "../../../shared/components/MediaField";

const AUDIO_SOURCES = ["upload", "spotify-track", "spotify-playlist"] as const;
type AudioSource = (typeof AUDIO_SOURCES)[number];

/**
 * Editor do `BlockType: "audio"` (Sprint 13, Tarefa D.1) — upload de arquivo
 * avulso (via picker de assets filtrado por áudio) ou embed do Spotify
 * (faixa ou playlist). `autoplay` é `false` por padrão e fica visível por
 * bloco, não global — autoplay agressivo é ruim para UX e alguns navegadores
 * bloqueiam mesmo se ligado.
 */
export function AudioBlockEditor({ content, onChange }: { content: Record<string, unknown>; onChange: (patch: Record<string, unknown>) => void }) {
  const source = (typeof content.source === "string" ? content.source : "upload") as AudioSource;
  const autoplay = Boolean(content.autoplay);

  return (
    <div className="mt-3 space-y-3">
      <SelectLike label="Fonte do áudio" value={source} options={[...AUDIO_SOURCES]} onChange={(v) => onChange({ source: v })} />
      {source === "upload" ? (
        <MediaField label="Arquivo de áudio" value={typeof content.fileAssetId === "string" ? content.fileAssetId : ""} typeFilter="áudio" onChange={(name) => onChange({ fileAssetId: name })} />
      ) : (
        <Field
          label={source === "spotify-track" ? "URL da faixa no Spotify" : "URL da playlist no Spotify"}
          value={typeof content.spotifyUrl === "string" ? content.spotifyUrl : ""}
          onChange={(v) => onChange({ spotifyUrl: v })}
        />
      )}
      <label className="flex items-center gap-2 text-sm">
        <input type="checkbox" checked={autoplay} onChange={(e) => onChange({ autoplay: e.target.checked })} className="accent-primary" />
        Reproduzir automaticamente ao abrir a página (recomendamos manter desligado)
      </label>
    </div>
  );
}
