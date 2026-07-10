import { useState } from "react";
import { Loader2 } from "lucide-react";
import { Button, Card, EmptyState, PageHeader, PartialErrorWidget, SkeletonLines } from "../../../shared/components/Primitives";
import { KGBadge } from "../components/KGBadge";
import { toast } from "../../../core/notifications/toast";
import { knowledgeService, type GraphOrphan } from "../services/knowledgeService";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";

export function OrphanEntityTable() {
  const { product } = useCurrentProduct();
  const productId = product?.id ?? "p1";
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [reloadKey, setReloadKey] = useState(0);
  const [resolvingAll, setResolvingAll] = useState(false);
  const [resolvingRow, setResolvingRow] = useState<string | null>(null);
  const { data, loading, error } = useAsyncData(() => knowledgeService.listOrphans(productId), [productId, reloadKey]);

  const rows = data ?? [];
  const allSelected = rows.length > 0 && rows.every((r) => selected.has(r.id));

  const toggleSelectAll = () => {
    setSelected(allSelected ? new Set() : new Set(rows.map((r) => r.id)));
  };

  const handleResolveRow = async (row: GraphOrphan) => {
    setResolvingRow(row.id);
    try {
      await knowledgeService.resolveOrphan(productId, row.id, row.action);
      setReloadKey((current) => current + 1);
      setSelected((prev) => {
        const next = new Set(prev);
        next.delete(row.id);
        return next;
      });
      toast.success(`${row.action} aplicado!`, { description: row.label });
    } finally {
      setResolvingRow(null);
    }
  };

  const handleResolveSelected = async () => {
    if (selected.size === 0) {
      toast.error("Selecione ao menos uma entidade.");
      return;
    }
    setResolvingAll(true);
    try {
      await knowledgeService.resolveOrphans(productId, Array.from(selected));
      setReloadKey((current) => current + 1);
      toast.success(`${selected.size} entidade(s) resolvida(s)!`);
      setSelected(new Set());
    } finally {
      setResolvingAll(false);
    }
  };

  return (
    <>
      <PageHeader title="Orphan Entities" desc="Entidades sem relações ativas. Limpeza recomendada para manter o grafo consistente." badge="Órfãos">
        <Button onClick={toggleSelectAll}>{allSelected ? "Limpar seleção" : "Selecionar todos"}</Button>
        <Button primary onClick={handleResolveSelected} disabled={resolvingAll}>{resolvingAll && <Loader2 size={15} className="animate-spin" />}{resolvingAll ? "Resolvendo..." : "Resolver órfãos"}</Button>
      </PageHeader>
      <Card>
        {loading && <SkeletonLines />}
        {error && <PartialErrorWidget />}
        {!loading && !error && rows.length === 0 && <EmptyState compact title="Nenhum órfão encontrado" description="As entidades do produto estão conectadas ou ainda não existem." />}
        {!loading && !error && rows.map((r) => (
          <div key={r.id} className="mb-2 grid gap-2 rounded-xl border border-border p-3 text-sm md:grid-cols-5">
            <input type="checkbox" checked={selected.has(r.id)} onChange={() => setSelected((prev) => { const next = new Set(prev); if (next.has(r.id)) next.delete(r.id); else next.add(r.id); return next; })} aria-label={`Selecionar ${r.label}`} />
            <div><b>{r.label}</b><p className="text-xs text-muted-foreground">{r.status}</p></div>
            <KGBadge type={r.type} />
            <span className="text-muted-foreground">{r.action} ou vincular</span>
            <Button onClick={() => handleResolveRow(r)} disabled={resolvingRow === r.id}>{resolvingRow === r.id && <Loader2 size={14} className="animate-spin" />}{r.action}</Button>
          </div>
        ))}
      </Card>
    </>
  );
}
