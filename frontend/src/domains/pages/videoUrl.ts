/** Mesma regex documentada no backend (etapa 21, Seção C) para `video`/`video-gallery` — replicada no cliente para rejeitar a URL antes mesmo de salvar. */
const YOUTUBE_URL_PATTERN = /^https:\/\/(www\.)?youtube\.com\/watch\?v=[\w-]{6,}$|^https:\/\/youtu\.be\/[\w-]{6,}$/;

export function isValidYoutubeUrl(url: string): boolean {
  return YOUTUBE_URL_PATTERN.test(url);
}
