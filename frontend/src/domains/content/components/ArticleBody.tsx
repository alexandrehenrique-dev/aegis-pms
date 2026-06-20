import { Fragment } from "react";
import { KgRefMark } from "../../knowledge/components/KgRefMark";

const KG_REF_PATTERN = /\{\{kg-ref:([\w-]+):([^}]+)\}\}/g;

/**
 * Renderiza o corpo de um artigo publicado, transformando marcas inline
 * `{{kg-ref:nodeId:Label}}` em referências vivas com popover ao passar o
 * mouse (Sprint 11, Tarefa C.4) — a experiência de leitura que a WikiDev
 * define como motivo de existir da plataforma ("Conhecimento Conectado").
 */
export function ArticleBody({ body }: { body: string }) {
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
        typeof part === "string" ? <Fragment key={i}>{part}</Fragment> : <KgRefMark key={i} nodeId={part.nodeId} label={part.label} />,
      )}
    </p>
  );
}
