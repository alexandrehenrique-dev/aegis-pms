/** Mesma regra documentada no backend para `video`/`video-gallery`, aceitando query params do share do YouTube. */
const MIN_YOUTUBE_ID_LENGTH = 6;
const YOUTUBE_HOSTS = new Set(["youtube.com", "www.youtube.com"]);

export function isValidYoutubeUrl(url: string): boolean {
  return Boolean(extractYoutubeVideoId(url));
}

export function toYoutubeEmbedUrl(url: string): string | undefined {
  const id = extractYoutubeVideoId(url);
  if (!id) return undefined;
  const origin = typeof window === "undefined" ? "" : `&origin=${encodeURIComponent(window.location.origin)}`;
  return `https://www.youtube-nocookie.com/embed/${id}?rel=0&modestbranding=1${origin}`;
}

export function extractYoutubeVideoId(url: string): string | undefined {
  try {
    const parsed = new URL(url);
    if (parsed.hostname === "youtu.be") {
      const id = parsed.pathname.split("/").filter(Boolean)[0];
      return isYoutubeVideoId(id) ? id : undefined;
    }
    if (YOUTUBE_HOSTS.has(parsed.hostname)) {
      const id = parsed.pathname === "/embed"
        ? undefined
        : parsed.pathname.startsWith("/embed/")
          ? parsed.pathname.split("/").filter(Boolean)[1]
          : parsed.searchParams.get("v") ?? undefined;
      return isYoutubeVideoId(id) ? id : undefined;
    }
    return undefined;
  } catch {
    return undefined;
  }
}

function isYoutubeVideoId(value: string | undefined): value is string {
  if (!value || value.length < MIN_YOUTUBE_ID_LENGTH) return false;
  return [...value].every((character) => /[\w-]/.test(character));
}
