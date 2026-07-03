import { useEffect, useMemo, useState } from "react";
import { CheckCircle2, Clock3, Eye, MoreVertical } from "lucide-react";
import { Badge, Button, EmptyState, PageHeader, PartialErrorWidget, SkeletonLines } from "../../../shared/components/Primitives";
import { ContextActionMenu, type ContextMenuTarget } from "../../../shared/components/ContextActionMenu";
import { FeedbackDetailDrawer } from "../components/FeedbackDetailDrawer";
import { feedbackService } from "../../../core/notifications/services/feedbackService";
import { tenantsService } from "../../../core/tenants/services/tenantsService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { formatRelativeTime } from "../../../shared/utils/relativeTime";
import { toast } from "../../../core/notifications/toast";
import type { FeedbackStatus, FeedbackSummary } from "../../../core/notifications/contracts/feedback";

const STATUS_TABS: { value: FeedbackStatus | "todos"; label: string }[] = [
  { value: "todos", label: "Todos" },
  { value: "aberto", label: "Abertos" },
  { value: "em_analise", label: "Em análise" },
  { value: "resolvido", label: "Resolvidos" },
];

const PRIORITY_FILTERS = ["alta", "crítica"] as const;

const PRIORITY_TONE: Record<string, "red" | "amber" | "neutral"> = {
  "crítica": "red",
  alta: "amber",
  média: "neutral",
  baixa: "neutral",
};

const PRIORITY_RANK: Record<string, number> = { "crítica": 3, alta: 2, média: 1, baixa: 0 };

/**
 * Inbox de feedbacks do Super Admin (Sprint 23). Rota `/admin/feedback` só é
 * visível/alcançável por `super_admin` (ver `core/permissions/roles.ts`),
 * então a listagem cross-tenant via `feedbackService.listAll()` é sempre o
 * caminho correto aqui (nunca `listByTenant`, que é para Tenant Admin).
 */
export function FeedbackInboxPage() {
  const { data, loading, error } = useAsyncData(() => feedbackService.listAll(), []);
  const { data: tenants } = useAsyncData(() => tenantsService.listTenants(), []);
  const [items, setItems] = useState<FeedbackSummary[]>([]);
  const [status, setStatus] = useState<FeedbackStatus | "todos">("todos");
  const [priorities, setPriorities] = useState<Set<string>>(new Set());
  const [tenantId, setTenantId] = useState<string>("");
  const [selected, setSelected] = useState<FeedbackSummary | null>(null);
  const [menu, setMenu] = useState<{ feedback: FeedbackSummary; position: ContextMenuTarget } | null>(null);

  useEffect(() => { if (data) setItems(data); }, [data]);

  const tenantNameById = useMemo(() => new Map((tenants ?? []).map((t) => [t.id, t.name])), [tenants]);

  const counts = useMemo(() => ({
    aberto: items.filter((f) => f.status === "aberto").length,
    em_analise: items.filter((f) => f.status === "em_analise").length,
    resolvido: items.filter((f) => f.status === "resolvido").length,
  }), [items]);

  const filtered = useMemo(() => items
    .filter((f) => status === "todos" || f.status === status)
    .filter((f) => priorities.size === 0 || priorities.has(f.priority))
    .filter((f) => !tenantId || f.tenantId === tenantId)
    .sort((a, b) => (PRIORITY_RANK[b.priority] ?? 0) - (PRIORITY_RANK[a.priority] ?? 0) || new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()),
  [items, status, priorities, tenantId]);

  const togglePriority = (p: string) => setPriorities((prev) => {
    const next = new Set(prev);
    if (next.has(p)) next.delete(p); else next.add(p);
    return next;
  });

  const handleStatusChange = async (feedbackId: string, next: FeedbackStatus) => {
    await feedbackService.updateStatus(feedbackId, next);
    setItems((prev) => prev.map((f) => (f.id === feedbackId ? { ...f, status: next } : f)));
    setSelected((prev) => (prev && prev.id === feedbackId ? { ...prev, status: next } : prev));
    toast.success("Status atualizado.");
  };

  const clearFilters = () => { setStatus("todos"); setPriorities(new Set()); setTenantId(""); };

  if (loading) return <SkeletonLines />;
  if (error) return <PartialErrorWidget />;

  return (
    <>
      <PageHeader title="Feedbacks recebidos" module="Administração" desc="Inbox de bugs e sugestões enviados pelos usuários da plataforma." badge="Super Admin">
        <select
          value={tenantId}
          onChange={(e) => setTenantId(e.target.value)}
          className="rounded-xl border border-border bg-card px-3 py-2 text-sm outline-primary"
        >
          <option value="">Filtrar por tenant</option>
          {(tenants ?? []).map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
        </select>
      </PageHeader>

      <div className="mb-4 flex flex-wrap items-center gap-3">
        <div className="flex flex-wrap gap-1.5">
          {STATUS_TABS.map((tab) => (
            <Button key={tab.value} primary={status === tab.value} onClick={() => setStatus(tab.value)}>
              {tab.label}{tab.value !== "todos" && ` ${counts[tab.value]}`}
            </Button>
          ))}
        </div>
        <div className="flex gap-1.5">
          {PRIORITY_FILTERS.map((p) => (
            <Button key={p} primary={priorities.has(p)} onClick={() => togglePriority(p)} className="capitalize">{p}</Button>
          ))}
        </div>
      </div>

      {filtered.length === 0 ? (
        <EmptyState
          title="🎉 Nenhum feedback por aqui"
          description="Parece que tudo está funcionando bem."
          secondaryAction={{ label: "Ver todos os feedbacks", onClick: clearFilters }}
        />
      ) : (
        <div className="space-y-2">
          {filtered.map((f) => (
            <div key={f.id} className="flex items-center gap-3 rounded-xl border border-border bg-card p-3">
              <span className="w-20 shrink-0 font-mono text-sm">{f.id}</span>
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2">
                  <span className="text-sm font-medium">{f.category}</span>
                  <Badge tone={PRIORITY_TONE[f.priority] ?? "neutral"}>{f.priority}</Badge>
                  {f.screenName && <span className="truncate text-xs text-muted-foreground">{f.screenName}</span>}
                </div>
                <p className="truncate text-xs text-muted-foreground">
                  {tenantNameById.get(f.tenantId) ?? f.tenantId} · {f.createdBySubject} · {formatRelativeTime(f.createdAt)}
                </p>
              </div>
              <Badge tone={f.status === "resolvido" ? "green" : f.status === "em_analise" ? "blue" : "neutral"}>{f.status.replace("_", " ")}</Badge>
              <button
                onClick={(e) => setMenu({ feedback: f, position: { x: e.clientX, y: e.clientY } })}
                aria-label="Mais ações"
                className="rounded-lg p-2 text-muted-foreground transition hover:bg-muted"
              >
                <MoreVertical size={16} />
              </button>
            </div>
          ))}
        </div>
      )}

      {menu && (
        <ContextActionMenu
          position={menu.position}
          onClose={() => setMenu(null)}
          items={[
            { label: "Ver detalhes", icon: <Eye size={14} />, onClick: () => { setSelected(menu.feedback); setMenu(null); } },
            ...(menu.feedback.status === "aberto" ? [{ label: "Marcar como em análise", icon: <Clock3 size={14} />, onClick: () => { handleStatusChange(menu.feedback.id, "em_analise"); setMenu(null); } }] : []),
            ...(menu.feedback.status !== "resolvido" ? [{ label: "Marcar como resolvido", icon: <CheckCircle2 size={14} />, onClick: () => { handleStatusChange(menu.feedback.id, "resolvido"); setMenu(null); } }] : []),
          ]}
        />
      )}

      <FeedbackDetailDrawer
        feedback={selected}
        tenantName={selected ? tenantNameById.get(selected.tenantId) ?? "" : ""}
        onOpenChange={(open) => !open && setSelected(null)}
        onStatusChange={handleStatusChange}
      />
    </>
  );
}
