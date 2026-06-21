import { KgRefMark } from "../../knowledge/components/KgRefMark";
import { Markdown } from "../../../shared/components/Markdown";

const KG_REF_PATTERN = /\{\{kg-ref:([\w-]+):([^}]+)\}\}/g;

/**
 * Renderiza o corpo de um artigo publicado, transformando marcas inline
 * `{{kg-ref:nodeId:Label}}` em referências vivas com popover ao passar o
 * mouse (Sprint 11, Tarefa C.4) — a experiência de leitura que a WikiDev
 * define como motivo de existir da plataforma ("Conhecimento Conectado").
 * Os trechos de texto entre marcas `kg-ref` aceitam markdown (Sprint 13,
 * Tarefa B.2) — por isso são processados em segmentos, não como bloco único.
 */
export function ArticleBody({ body, forceLightProse = false }: { body: string; forceLightProse?: boolean }) {
  const parts: Array<string | { nodeId: string; label: string }> = [];
  let lastIndex = 0;
  for (const match of body.matchAll(KG_REF_PATTERN)) {
    const [full, nodeId, label] = match;
    const index = match.index ?? 0;
    if (index > lastIndex) parts.push(body.slice(lastIndex, index));
    parts.push({ nodeId, label });
    lastIndex = index + full.length;
  }
  if (lastIndex < body.length) parts.push(body.slice(lastIndex));

  return (
    <p className="leading-relaxed text-foreground">
      {parts.map((part, i) =>
        typeof part === "string" ? <Markdown key={i} inline forceLightProse={forceLightProse}>{part}</Markdown> : <KgRefMark key={i} nodeId={part.nodeId} label={part.label} />,
      )}
    </p>
  );
}
