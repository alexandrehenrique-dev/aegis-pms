import { useState } from "react";
import { Tooltip, TooltipContent, TooltipTrigger } from "../../../shared/components/ui/tooltip";
import { Badge } from "../../../shared/components/Primitives";
import { knowledgeService } from "../services/knowledgeService";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";
import type { GraphNodePreview } from "../contracts/responses";

/**
 * Marca inline `{{kg-ref:nodeId:Label}}` (Sprint 11, Tarefa C.3/C.4) — ao
 * passar o mouse sobre a referência dentro do corpo de um artigo publicado,
 * busca `GraphNodePreview` (leve, cacheável) e mostra um popover, em vez de
 * navegar para a tela de detalhe completa do nó.
 */
export function KgRefMark({ nodeId, label }: { nodeId: string; label: string }) {
  const { product } = useCurrentProduct();
  const productId = product?.id ?? "p1";
  const [preview, setPreview] = useState<GraphNodePreview | null | undefined>(undefined);

  const loadPreview = () => {
    if (preview !== undefined) return;
    knowledgeService.getNodePreview(productId, nodeId).then((p) => setPreview(p ?? null));
  };

  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <button
          type="button"
          onMouseEnter={loadPreview}
          onFocus={loadPreview}
          className="underline decoration-dotted decoration-primary/60 underline-offset-2 text-primary hover:decoration-solid"
        >
          {label}
        </button>
      </TooltipTrigger>
      <TooltipContent className="w-64 bg-card text-card-foreground border border-border p-3">
        {preview === undefined && <p className="text-xs text-muted-foreground">Carregando...</p>}
        {preview === null && <p className="text-xs text-muted-foreground">Referência não encontrada.</p>}
        {preview && (
          <div className="space-y-1">
            <p className="text-xs font-bold uppercase tracking-wider text-primary">{preview.type}</p>
            <p className="text-sm font-semibold text-foreground">{preview.label}</p>
            <p className="text-xs text-muted-foreground">{preview.summary}</p>
            {preview.difficulty && <Badge tone="blue">{preview.difficulty}</Badge>}
          </div>
        )}
      </TooltipContent>
    </Tooltip>
  );
}
