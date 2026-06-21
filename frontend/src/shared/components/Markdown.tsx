import ReactMarkdown, { type Components } from "react-markdown";
import rehypeRaw from "rehype-raw";
import rehypeSanitize, { type Options as SanitizeSchema } from "rehype-sanitize";
import type { PluggableList } from "unified";

const BLOCK_COMPONENTS: Components = {
  a: ({ children, ...props }) => <a {...props} className="text-primary underline" target="_blank" rel="noreferrer">{children}</a>,
};

const INLINE_COMPONENTS: Components = {
  ...BLOCK_COMPONENTS,
  p: ({ children }) => <>{children}</>,
};

/**
 * Allowlist de sanitização do cliente (Sprint 18, Tarefa B.2) — replica
 * fielmente a mesma allowlist que o backend já impõe ao salvar (etapas
 * 10/21): `p, strong, em, ul, ol, li, blockquote, h2, h3, a, br` mais `span`
 * com só o atributo `class`, restrito aos 6 valores fixos da paleta de cor
 * (Tarefa B.1) — nunca `style` nem qualquer outro atributo/tag, mesmo que o
 * `rehype-raw` abaixo os interprete antes da sanitização rodar.
 */
const MARKDOWN_SANITIZE_SCHEMA: SanitizeSchema = {
  tagNames: ["p", "strong", "em", "ul", "ol", "li", "blockquote", "h2", "h3", "a", "br", "span"],
  attributes: {
    a: ["href"],
    span: [["className", "text-aegis-red", "text-aegis-blue", "text-aegis-green", "text-aegis-amber", "text-aegis-violet"]],
  },
  protocols: { href: ["http", "https", "mailto"] },
};

const REHYPE_PLUGINS: PluggableList = [rehypeRaw, [rehypeSanitize, MARKDOWN_SANITIZE_SCHEMA]];

/**
 * Renderizador de markdown único do app — toda conversão de `**bold**`/listas/links
 * para HTML passa por aqui (via `react-markdown`, sem `dangerouslySetInnerHTML`).
 * `inline` evita que o parser envolva o texto num bloco `<p>` próprio, para uso
 * dentro de fluxo de texto já existente (ex.: `ArticleBody`). `forceLightProse`
 * (Sprint 18, Tarefa C.2) renderiza sem `dark:prose-invert` — só para os
 * previews "papel branco" (`ResponsivePreviewFrame`/`FormPreviewFrame`), que
 * simulam a página publicada de verdade e nunca devem reagir ao tema escuro
 * do editor; qualquer outro uso de `Markdown` no app continua reagindo ao tema.
 */
export function Markdown({ children, inline = false, className = "", forceLightProse = false }: { children: string; inline?: boolean; className?: string; forceLightProse?: boolean }) {
  return (
    <span className={`prose prose-sm max-w-none ${forceLightProse ? "" : "dark:prose-invert"} prose-p:my-1 prose-ul:my-1 prose-ol:my-1 ${className}`}>
      <ReactMarkdown components={inline ? INLINE_COMPONENTS : BLOCK_COMPONENTS} rehypePlugins={REHYPE_PLUGINS}>{children}</ReactMarkdown>
    </span>
  );
}
