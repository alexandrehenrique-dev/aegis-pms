import { useEffect, useState } from "react";
import { Search } from "lucide-react";
import { knowledgeService } from "../services/knowledgeService";
import type { KGNode } from "../mocks/knowledge.mocks";

/**
 * Busca leve de entidade para linkar uma referência inline (`kg-ref`)
 * durante a autoria (Sprint 11, Tarefa C.3) — versão embutível do que
 * `EntitySearch.tsx` faz como tela cheia.
 *
 * `productId` restringe a busca aos nós do mesmo produto do conteúdo em
 * edição (ADR-0016: uma edge nunca conecta nós de produtos diferentes) —
 * sem isto, o autor poderia escolher (e o `createEdge` rejeitaria depois,
 * silenciosamente) uma entidade de outro produto.
 */
export function EntityPicker({ productId, onSelect }: { productId: string; onSelect: (node: KGNode) => void }) {
  const [q, setQ] = useState("");
  const [results, setResults] = useState<KGNode[]>([]);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    setLoaded(false);
    knowledgeService.searchNodes(q, productId).then((r) => { setResults(r); setLoaded(true); });
  }, [q, productId]);

  return (
    <div className="w-72">
      <div className="mb-2 flex items-center gap-2 rounded-lg border border-border px-2 py-1.5">
        <Search size={14} className="text-muted-foreground" />
        <input autoFocus value={q} onChange={(e) => setQ(e.target.value)} placeholder="Buscar entidade..." className="w-full bg-transparent text-sm outline-none" />
      </div>
      <div className="max-h-56 space-y-1 overflow-auto">
        {/* J.3.3 (BUG-SPRINT-05) — distingue "grafo vazio" (nenhum resultado mesmo sem busca) de "busca sem resultado", já que o primeiro caso costuma ser conteúdo antigo que ainda não passou por `ensureNodeForContent`. */}
        {loaded && results.length === 0 && (
          q.trim() === "" ? (
            <p className="p-2 text-xs text-muted-foreground">Nenhuma entidade no grafo ainda. Conteúdos publicados aparecem automaticamente após a primeira edição.</p>
          ) : (
            <p className="p-2 text-xs text-muted-foreground">Nenhuma entidade encontrada para "{q}".</p>
          )
        )}
        {results.map((n) => (
          <button key={n.id} onClick={() => onSelect(n)} className="flex w-full items-center justify-between rounded-lg p-2 text-left text-sm hover:bg-muted">
            <span>{n.label}</span>
            <span className="text-xs text-muted-foreground">{n.type}</span>
          </button>
        ))}
      </div>
    </div>
  );
}
