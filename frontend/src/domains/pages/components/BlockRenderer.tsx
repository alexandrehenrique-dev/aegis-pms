import type { MiniBlock } from "../contracts/responses";
import type { Section } from "../contracts/responses";
import { Markdown } from "../../../shared/components/Markdown";
import { useAuthenticatedImage } from "../../../shared/hooks/useAuthenticatedImage";
import { EventListPreview } from "./EventListPreview";

function asStr(v: unknown, fallback = ""): string {
  return typeof v === "string" ? v : fallback;
}

function asArray(v: unknown): Record<string, unknown>[] {
  return Array.isArray(v) ? (v as Record<string, unknown>[]) : [];
}

/** K.2 — resolve a imagem via fetch autenticado (blob URL) antes de renderizar; nunca inserir a URL do `/download` protegido direto em `src`. */
function AuthImg({ assetId, alt, className }: { assetId: string | undefined; alt: string; className: string }) {
  const src = useAuthenticatedImage(assetId);
  return src
    ? <img src={src} alt={alt} className={className} />
    : <div className={`flex items-center justify-center bg-muted p-6 text-center text-xs text-muted-foreground ${className}`}>[imagem: {alt}]</div>;
}

function AuthAudio({ assetId, className }: { assetId: string | undefined; className: string }) {
  const src = useAuthenticatedImage(assetId);
  return src
    ? <audio controls src={src} className={className} />
    : <div className="rounded-lg border border-border p-3 text-sm text-muted-foreground">[áudio não disponível: {assetId ?? "nenhum"}]</div>;
}

function AuthVideo({ assetId, className }: { assetId: string | undefined; className: string }) {
  const src = useAuthenticatedImage(assetId);
  return src
    ? <video controls src={src} className={className} />
    : <div className={`flex items-center justify-center rounded-lg border border-border text-sm text-muted-foreground ${className}`}>[vídeo não disponível]</div>;
}

function toYoutubeEmbed(url: string): string | undefined {
  const match = url.match(/(?:v=|youtu\.be\/)([a-zA-Z0-9_-]{11})/);
  return match ? `https://www.youtube.com/embed/${match[1]}` : undefined;
}

function MiniBlockPreview({ block }: { block: MiniBlock }) {
  const c = block.content;
  switch (block.type) {
    case "image":
      return <div className="rounded-lg bg-muted p-6 text-center text-xs text-muted-foreground">[imagem: {asStr(c.alt, "sem descrição")}]</div>;
    case "cta":
      return <button className="rounded-lg bg-primary px-3 py-1.5 text-sm text-primary-foreground">{asStr(c.label, "Call to action")}</button>;
    default:
      return (
        <div>
          {c.title ? <p className="font-medium">{asStr(c.title)}</p> : null}
          {c.body ? <p className="mt-1 text-sm text-muted-foreground">{asStr(c.body)}</p> : null}
        </div>
      );
  }
}

/**
 * Renderização visual aproximada de um `Section` (Sprint 12, Tarefa F) — não
 * é pixel-perfect, mas reflete o conteúdo real editado em vez de um mock
 * fixo. Pensado para ser reaproveitado futuramente pela renderização
 * pública real (fora de escopo desta sprint).
 */
export function BlockRenderer({ section, forceLightProse = false, productSlug }: { section: Section; forceLightProse?: boolean; productSlug?: string }) {
  const c = section.content;

  switch (section.type) {
    case "hero": {
      const image = (c.image as Record<string, unknown>) ?? {};
      const hasImage = typeof image.src === "string" && image.src.length > 0;
      // Retrocompatibilidade (E.10.2): conteúdo criado antes da canonicalização
      // para `ctas[]` guarda os CTAs em `ctaPrimary`/`ctaSecondary` — sem este
      // fallback, um hero já publicado perderia os botões no preview público
      // assim que este renderer passasse a ler só `ctas[]`.
      const declaredCtas = asArray(c.ctas);
      const legacyCtas = [c.ctaPrimary, c.ctaSecondary]
        .filter((cta): cta is Record<string, unknown> => !!cta && typeof cta === "object" && asStr((cta as Record<string, unknown>).label).trim() !== "");
      const ctas = declaredCtas.length > 0 ? declaredCtas : legacyCtas;
      // E.10.3 — "light" é a variante compacta (sem imagem, menos respiro);
      // "dark" (padrão) mantém o hero completo com imagem em destaque.
      const isLightTheme = c.theme === "light";
      return (
        <div className={isLightTheme ? "rounded-xl border border-border p-4 text-center" : "rounded-xl bg-muted p-8 text-center"}>
          {!isLightTheme && hasImage && (
            <AuthImg assetId={image.src as string} alt={asStr(image.alt, "sem descrição")} className="mx-auto mb-4 max-h-64 w-full max-w-md rounded-lg object-cover" />
          )}
          <h2 className={isLightTheme ? "text-xl font-semibold" : "text-3xl font-semibold"}>{asStr(c.title, section.label)}</h2>
          {c.subtitle ? <p className="mt-2 text-muted-foreground">{asStr(c.subtitle)}</p> : null}
          <div className="mt-4 flex flex-wrap justify-center gap-2">
            {ctas.map((cta, i) => (
              asStr(cta.label) ? (
                <button key={i} className={i === 0 ? "rounded-lg bg-primary px-4 py-2 text-sm text-primary-foreground" : "rounded-lg border border-border px-4 py-2 text-sm"}>{asStr(cta.label)}</button>
              ) : null
            ))}
          </div>
        </div>
      );
    }

    case "text":
    case "rich-text":
      return (
        <div className="p-6">
          {c.title ? <h3 className="text-xl font-semibold">{asStr(c.title)}</h3> : null}
          <div className="mt-2 max-w-2xl text-sm text-muted-foreground"><Markdown forceLightProse={forceLightProse}>{asStr(c.body)}</Markdown></div>
        </div>
      );

    case "two-column": {
      const left = asArray(c.left) as unknown as MiniBlock[];
      const right = asArray(c.right) as unknown as MiniBlock[];
      return (
        <div className="grid gap-4 p-6 md:grid-cols-2">
          <div className="space-y-3">{left.map((b, i) => <MiniBlockPreview key={i} block={b} />)}</div>
          <div className="space-y-3">{right.map((b, i) => <MiniBlockPreview key={i} block={b} />)}</div>
        </div>
      );
    }

    case "image": {
      const assetId = typeof c.src === "string" ? c.src : undefined;
      return <AuthImg assetId={assetId} alt={asStr(c.alt, "sem descrição")} className="w-full rounded-xl object-cover" />;
    }

    case "image-text": {
      const image = (c.image as Record<string, unknown>) ?? {};
      const assetId = typeof image.src === "string" ? image.src : undefined;
      const reverse = c.imagePosition === "right";
      return (
        <div className={`grid gap-4 p-6 md:grid-cols-2 ${reverse ? "md:[&>*:first-child]:order-2" : ""}`}>
          <AuthImg assetId={assetId} alt={asStr(image.alt, "sem descrição")} className="rounded-xl object-cover" />
          <div>
            <h3 className="text-xl font-semibold">{asStr(c.title)}</h3>
            <div className="mt-2 text-sm text-muted-foreground"><Markdown forceLightProse={forceLightProse}>{asStr(c.body)}</Markdown></div>
          </div>
        </div>
      );
    }

    case "feature-grid":
    case "card-list": {
      const items = asArray(c.items);
      return (
        <div className="p-6">
          {c.title ? <h3 className="mb-3 text-xl font-semibold">{asStr(c.title)}</h3> : null}
          <div className="grid gap-3 sm:grid-cols-2 md:grid-cols-3">
            {items.map((item, i) => (
              <div key={i} className="rounded-lg border border-border p-3">
                <p className="font-medium">{asStr(item.title)}</p>
                <div className="mt-1 text-sm text-muted-foreground"><Markdown inline forceLightProse={forceLightProse}>{asStr(item.desc)}</Markdown></div>
              </div>
            ))}
          </div>
        </div>
      );
    }

    case "gallery": {
      const items = asArray(c.items);
      return (
        <div className="grid grid-cols-3 gap-2 p-6">
          {items.map((item, i) => (
            <AuthImg key={i} assetId={typeof item.src === "string" ? item.src : undefined} alt={asStr(item.alt, "imagem")} className="aspect-square rounded-lg object-cover" />
          ))}
        </div>
      );
    }

    case "timeline": {
      const items = asArray(c.items);
      return (
        <div className="space-y-3 p-6">
          {items.map((item, i) => (
            <div key={i} className="flex gap-3">
              <span className="shrink-0 rounded-md bg-muted px-2 py-0.5 text-xs font-medium">{asStr(item.date)}</span>
              <div><p className="font-medium">{asStr(item.title)}</p><p className="text-sm text-muted-foreground">{asStr(item.desc)}</p></div>
            </div>
          ))}
        </div>
      );
    }

    case "event-list":
      return <EventListPreview content={c} productSlug={productSlug} />;

    case "cta-section":
      return (
        <div className="rounded-xl bg-primary/10 p-8 text-center">
          <h3 className="text-xl font-semibold">{asStr(c.title)}</h3>
          {c.ctaPrimary ? <button className="mt-3 rounded-lg bg-primary px-4 py-2 text-sm text-primary-foreground">{asStr((c.ctaPrimary as Record<string, unknown>)?.label, "Call to action")}</button> : null}
        </div>
      );

    case "faq": {
      const items = asArray(c.items);
      return (
        <div className="space-y-2 p-6">
          {items.map((item, i) => (
            <div key={i} className="rounded-lg border border-border p-3">
              <p className="font-medium">{asStr(item.q ?? item.question)}</p>
              <div className="mt-1 text-sm text-muted-foreground"><Markdown inline forceLightProse={forceLightProse}>{asStr(item.a ?? item.answer)}</Markdown></div>
            </div>
          ))}
        </div>
      );
    }

    case "contact":
      return (
        <div className="p-6">
          <h3 className="text-xl font-semibold">{asStr(c.title, "Fale com a gente")}</h3>
          <p className="mt-2 text-sm text-muted-foreground">{c.formId ? `Formulário referenciado: ${asStr(c.formId)}` : "Nenhum formulário selecionado."}</p>
        </div>
      );

    case "form":
      return (
        <div className="p-6">
          <p className="text-sm text-muted-foreground">{c.formId ? `Formulário embutido: ${asStr(c.formId)}` : "Nenhum formulário selecionado."}</p>
        </div>
      );

    case "audio": {
      const source = asStr(c.source, "upload");
      return (
        <div className="p-6">
          {c.title ? <h3 className="mb-2 text-xl font-semibold">{asStr(c.title)}</h3> : null}
          {source === "upload" ? (
            <AuthAudio assetId={asStr(c.fileAssetId) || undefined} className="w-full" />
          ) : (
            <a href={asStr(c.spotifyUrl)} target="_blank" rel="noreferrer" className="inline-block rounded-lg border border-border p-3 text-sm text-primary underline">
              Ouvir no Spotify {source === "spotify-track" ? "(faixa)" : "(playlist)"}
            </a>
          )}
          {c.autoplay ? <p className="mt-1 text-xs text-muted-foreground">Autoplay ativado.</p> : null}
        </div>
      );
    }

    case "video": {
      const source = asStr(c.source, "upload");
      const youtubeEmbedUrl = source === "youtube" ? toYoutubeEmbed(asStr(c.youtubeUrl)) : undefined;
      return (
        <div className="p-6">
          {c.title ? <h3 className="mb-2 text-xl font-semibold">{asStr(c.title)}</h3> : null}
          {source === "upload" ? (
            <AuthVideo assetId={asStr(c.fileAssetId) || undefined} className="aspect-video w-full rounded-lg" />
          ) : youtubeEmbedUrl ? (
            <iframe src={youtubeEmbedUrl} className="aspect-video w-full rounded-lg" allowFullScreen />
          ) : (
            <div className="flex aspect-video items-center justify-center rounded-lg border border-border text-sm text-muted-foreground">[URL do YouTube inválida]</div>
          )}
          {c.autoplay ? <p className="mt-1 text-xs text-muted-foreground">Autoplay ativado.</p> : null}
        </div>
      );
    }

    case "video-gallery": {
      const items = asArray(c.items);
      return (
        <div className="grid grid-cols-2 gap-3 p-6 md:grid-cols-3">
          {items.map((item, i) => (
            <div key={i} className="flex aspect-video flex-col items-center justify-center rounded-lg bg-muted p-2 text-center text-xs text-muted-foreground">
              {asStr(item.source, "upload") === "upload"
                ? `[vídeo: ${asStr(item.fileAssetId, "nenhum arquivo")}]`
                : `[YouTube: ${asStr(item.youtubeUrl, "nenhuma URL")}]`}
              <span className="mt-1 font-medium text-foreground">{asStr(item.title, "Sem título")}</span>
            </div>
          ))}
        </div>
      );
    }

    case "social-links": {
      const items = asArray(c.items);
      return (
        <div className="flex gap-3 p-6">
          {items.map((item, i) => <span key={i} className="rounded-full border border-border px-3 py-1 text-sm">{asStr(item.platform, "rede social")}</span>)}
        </div>
      );
    }

    case "download": {
      const items = asArray(c.items);
      return (
        <div className="space-y-2 p-6">
          {c.title ? <h3 className="text-xl font-semibold">{asStr(c.title)}</h3> : null}
          {items.map((item, i) => <div key={i} className="rounded-lg border border-border p-3 text-sm">{asStr(item.title, "Download")}</div>)}
        </div>
      );
    }

    default:
      return <div className="p-6 text-sm text-muted-foreground">Bloco "{section.type}" sem renderização de preview específica.</div>;
  }
}
