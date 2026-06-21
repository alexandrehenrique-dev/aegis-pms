import { AlertTriangle } from "lucide-react";
import { Card, PageHeader } from "../../../shared/components/Primitives";
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "../../../shared/components/ui/accordion";
import { useCurrentProduct } from "../../products/useCurrentProduct";
import { useFeedbackModal } from "../../notifications/FeedbackModalContext";
import { resolveEnabledModules } from "../../products/moduleDefaults";
import { MODULE_FAQ_TOPICS, PLATFORM_OVERVIEW_TOPICS } from "../helpTopics";

/**
 * Central de Ajuda (Sprint 13, Tarefa O) — antes, o botão "?" do `AppShell`
 * abria direto o `FeedbackModal` (reportar bug), sem nenhuma central de
 * ajuda/FAQ real. Duas seções: visão geral da plataforma (sempre visível,
 * cobre tudo que o sistema contém) e perguntas frequentes por módulo
 * (escopadas pelos módulos habilitados no produto atual). Cada `Accordion`
 * usa `type="single" collapsible` — um item aberto por vez.
 */
export function HelpCenter() {
  const { product } = useCurrentProduct();
  const { setOpen: setShowFeedback } = useFeedbackModal();
  const enabledModules = resolveEnabledModules(product);
  const moduleFaqEntries = Object.entries(MODULE_FAQ_TOPICS).filter(([moduleKey]) => enabledModules.includes(moduleKey));

  return (
    <>
      <PageHeader title="Central de Ajuda" desc="Como a plataforma funciona, de ponta a ponta — e perguntas frequentes dos módulos habilitados neste produto." badge={product?.name ?? "Produto"} />

      <Card>
        <h2 className="mb-1 text-lg font-semibold">Visão geral da plataforma</h2>
        <p className="mb-3 text-sm text-muted-foreground">O que cada área do Aegis PMS faz, independentemente dos módulos habilitados neste produto.</p>
        <Accordion type="single" collapsible>
          {PLATFORM_OVERVIEW_TOPICS.map((topic, i) => (
            <AccordionItem key={topic.title} value={`overview-${i}`}>
              <AccordionTrigger>{topic.title}</AccordionTrigger>
              <AccordionContent>{topic.description}</AccordionContent>
            </AccordionItem>
          ))}
        </Accordion>
      </Card>

      <Card className="mt-4">
        <h2 className="mb-1 text-lg font-semibold">Perguntas frequentes dos módulos deste produto</h2>
        <p className="mb-3 text-sm text-muted-foreground">Só aparecem aqui os módulos habilitados em {product?.name ?? "este produto"}.</p>
        {moduleFaqEntries.length === 0 ? (
          <p className="text-sm text-muted-foreground">Nenhum módulo com FAQ dedicada habilitado neste produto ainda.</p>
        ) : (
          <Accordion type="single" collapsible>
            {moduleFaqEntries.map(([moduleKey, items]) => (
              <AccordionItem key={moduleKey} value={moduleKey}>
                <AccordionTrigger>{moduleKey}</AccordionTrigger>
                <AccordionContent>
                  <div className="space-y-3">
                    {items.map((item) => (
                      <div key={item.question}>
                        <p className="font-medium text-foreground">{item.question}</p>
                        <p className="mt-0.5 text-muted-foreground">{item.answer}</p>
                      </div>
                    ))}
                  </div>
                </AccordionContent>
              </AccordionItem>
            ))}
          </Accordion>
        )}
      </Card>

      <div className="mt-4 flex items-center justify-between rounded-2xl border border-border bg-card p-4">
        <p className="text-sm text-muted-foreground">Não encontrou o que precisava?</p>
        <button onClick={() => setShowFeedback(true)} className="inline-flex items-center gap-2 rounded-lg border border-border bg-card px-3 py-2 text-sm transition hover:bg-muted">
          <AlertTriangle size={14} className="text-[#b45309]" />Reportar um problema
        </button>
      </div>
    </>
  );
}
