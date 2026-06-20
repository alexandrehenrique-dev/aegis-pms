import { useEffect, useState } from "react";
import { Search } from "lucide-react";
import { knowledgeService } from "../services/knowledgeService";
import type { KGNode } from "../mocks/knowledge.mocks";

/**
 * Busca leve de entidade para linkar uma referência inline (`kg-ref`)
 * durante a autoria (Sprint 11, Tarefa C.3) — versão embutível do que
 * `EntitySearch.tsx` faz como tela cheia.
 */
export function EntityPicker({ onSelect }: { onSelect: (node: KGNode) => void }) {
  const [q, setQ] = useState("");
  const [results, setResults] = useState<KGNode[]>([]);

  useEffect(() => {
    knowledgeService.searchNodes(q).then(setResults);
  }, [q]);

  return (
    <div className="w-72">
      <div className="mb-2 flex items-center gap-2 rounded-lg border border-border px-2 py-1.5">
        <Search size={14} className="text-muted-foreground" />
        <input autoFocus value={q} onChange={(e) => setQ(e.target.value)} placeholder="Buscar entidade..." className="w-full bg-transparent text-sm outline-none" />
      </div>
      <div className="max-h-56 space-y-1 overflow-auto">
        {results.length === 0 && <p className="p-2 text-xs text-muted-foreground">Nenhuma entidade encontrada.</p>}
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
