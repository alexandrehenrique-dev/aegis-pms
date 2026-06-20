/** Tempo relativo simples em português, a partir de um ISO timestamp (ex.: "há 3 dias"). */
export function formatRelativeTime(iso: string): string {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return "";

  const diffMin = Math.round((Date.now() - date.getTime()) / 60000);
  if (diffMin < 1) return "agora";
  if (diffMin < 60) return `há ${diffMin} min`;

  const diffH = Math.round(diffMin / 60);
  if (diffH < 24) return `há ${diffH}h`;

  const diffD = Math.round(diffH / 24);
  if (diffD < 30) return `há ${diffD} dia${diffD !== 1 ? "s" : ""}`;

  const diffMonths = Math.round(diffD / 30);
  if (diffMonths < 12) return `há ${diffMonths} mês${diffMonths !== 1 ? "es" : ""}`;

  const diffYears = Math.round(diffMonths / 12);
  return `há ${diffYears} ano${diffYears !== 1 ? "s" : ""}`;
}
