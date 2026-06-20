import type { MiniBlock } from "../contracts/responses";
import type { Section } from "../contracts/responses";
import { Markdown } from "../../../shared/components/Markdown";

function asStr(v: unknown, fallback = ""): string {
  return typeof v === "string" ? v : fallback;
}

function asArray(v: unknown): Record<string, unknown>[] {
  return Array.isArray(v) ? (v as Record<string, unknown>[]) : [];
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
export function BlockRenderer({ section }: { section: Section }) {
  const c = section.content;

  switch (section.type) {
    case "hero":
      return (
        <div className="rounded-xl bg-muted p-8 text-center">
          <h2 className="text-3xl font-semibold">{asStr(c.title, section.label)}</h2>
          {c.subtitle ? <p className="mt-2 text-muted-foreground">{asStr(c.subtitle)}</p> : null}
          {c.ctaPrimary ? <button className="mt-4 rounded-lg bg-primary px-4 py-2 text-sm text-primary-foreground">{asStr((c.ctaPrimary as Record<string, unknown>)?.label, "Saiba mais")}</button> : null}
        </div>
      );

    case "text":
    case "rich-text":
      return (
        <div className="p-6">
          {c.title ? <h3 className="text-xl font-semibold">{asStr(c.title)}</h3> : null}
          <div className="mt-2 max-w-2xl text-sm text-muted-foreground"><Markdown>{asStr(c.body)}</Markdown></div>
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
      const image = (c.image as Record<string, unknown>) ?? {};
      return <div className="rounded-xl bg-muted p-10 text-center text-xs text-muted-foreground">[imagem: {asStr(image.alt, "sem descrição")}]</div>;
    }

    case "image-text": {
      const image = (c.image as Record<string, unknown>) ?? {};
      const reverse = c.imagePosition === "right";
      return (
        <div className={`grid gap-4 p-6 md:grid-cols-2 ${reverse ? "md:[&>*:first-child]:order-2" : ""}`}>
          <div className="rounded-xl bg-muted p-10 text-center text-xs text-muted-foreground">[imagem: {asStr(image.alt, "sem descrição")}]</div>
          <div>
            <h3 className="text-xl font-semibold">{asStr(c.title)}</h3>
            <div className="mt-2 text-sm text-muted-foreground"><Markdown>{asStr(c.body)}</Markdown></div>
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
                <div className="mt-1 text-sm text-muted-foreground"><Markdown inline>{asStr(item.desc)}</Markdown></div>
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
            <div key={i} className="aspect-square rounded-lg bg-muted text-center text-[10px] text-muted-foreground flex items-center justify-center p-1">{asStr(item.alt, "imagem")}</div>
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
      return (
        <div className="p-6">
          <h3 className="text-xl font-semibold">{asStr(c.title, "Agenda")}</h3>
          <p className="mt-2 text-sm text-muted-foreground">Lista de eventos vinda da fonte referenciada (gerencie em "Gerenciar eventos" no editor).</p>
        </div>
      );

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
              <div className="mt-1 text-sm text-muted-foreground"><Markdown inline>{asStr(item.a ?? item.answer)}</Markdown></div>
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

    case "footer":
      return (
        <div className="border-t border-border p-6 text-sm text-muted-foreground">
          {asStr(c.address)}
        </div>
      );

    case "navbar": {
      const items = asArray(c.items);
      return (
        <div className="flex gap-4 border-b border-border p-4 text-sm">
          {items.map((item, i) => <span key={i}>{asStr(item.label)}</span>)}
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
