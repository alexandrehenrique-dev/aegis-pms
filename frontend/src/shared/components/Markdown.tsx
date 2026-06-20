import ReactMarkdown, { type Components } from "react-markdown";

const BLOCK_COMPONENTS: Components = {
  a: ({ children, ...props }) => <a {...props} className="text-primary underline" target="_blank" rel="noreferrer">{children}</a>,
};

const INLINE_COMPONENTS: Components = {
  ...BLOCK_COMPONENTS,
  p: ({ children }) => <>{children}</>,
};

/**
 * Renderizador de markdown único do app — toda conversão de `**bold**`/listas/links
 * para HTML passa por aqui (via `react-markdown`, sem `dangerouslySetInnerHTML`).
 * `inline` evita que o parser envolva o texto num bloco `<p>` próprio, para uso
 * dentro de fluxo de texto já existente (ex.: `ArticleBody`).
 */
export function Markdown({ children, inline = false, className = "" }: { children: string; inline?: boolean; className?: string }) {
  return (
    <span className={`prose prose-sm max-w-none dark:prose-invert prose-p:my-1 prose-ul:my-1 prose-ol:my-1 ${className}`}>
      <ReactMarkdown components={inline ? INLINE_COMPONENTS : BLOCK_COMPONENTS}>{children}</ReactMarkdown>
    </span>
  );
}
