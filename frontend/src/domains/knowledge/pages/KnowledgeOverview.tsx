import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router";
import { Button, Card, EmptyState, KPIWidget, PageHeader, PermissionHint, SkeletonLines } from "../../../shared/components/Primitives";
import { PermGate } from "../../../app/guards/PermGate";
import { useViewAsRole } from "../../../core/permissions/useViewAsRole";
import { useAuth } from "../../../core/auth/useAuth";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { knowledgeService } from "../services/knowledgeService";
import { contentService } from "../../content/services/contentService";
import type { ListEdgesResponse, ListNodesResponse } from "../contracts/responses";

function KnowledgeTimeline({ relations }: { relations: { from: string; to: string; verb: string }[] }) {
  return (
    <div className="space-y-1">
      {relations.length === 0 ? (
        <EmptyState compact title="Sem relações recentes" description="As relações aparecem quando conteúdos referenciam outras entidades." />
      ) : relations.slice(0, 6).map((relation, i) => (
        <div key={`${relation.from}-${relation.to}-${relation.verb}`} className="flex gap-3 rounded-xl p-3 hover:bg-muted">
          <span className="mt-1 h-2.5 w-2.5 rounded-full bg-primary" />
          <div><p className="text-sm font-medium">{relation.from} {relation.verb} {relation.to}</p><p className="text-xs text-muted-foreground">relação {i + 1} · Knowledge Graph</p></div>
        </div>
      ))}
    </div>
  );
}

export function KnowledgeOverview() {
  const navigate = useNavigate();
  const { viewAsRole } = useViewAsRole();
  const { effectiveProduct } = useAuth();
  const canExplore = viewAsRole !== "viewer";
  const seededFor = useRef<string | null>(null);
  const productId = effectiveProduct?.id ?? "";
  const [seedRevision, setSeedRevision] = useState(0);
  const { data: nodes, loading: loadingNodes } = useAsyncData<ListNodesResponse>(() => (productId ? knowledgeService.listNodes(productId) : Promise.resolve([])), [productId, seedRevision]);
  const { data: edges, loading: loadingEdges } = useAsyncData<ListEdgesResponse>(() => (productId ? knowledgeService.listEdges(productId) : Promise.resolve([])), [productId, seedRevision]);
  const nodeById = useMemo(() => new Map((nodes ?? []).map((node) => [node.id, node])), [nodes]);
  const relations = (edges ?? []).flatMap((edge) => {
    const from = nodeById.get(edge.from);
    const to = nodeById.get(edge.to);
    if (!from || !to) return [];
    return [{ from: from.label, to: to.label, verb: edge.verb }];
  });
  const orphanNodes = (nodes ?? []).filter((node) => !(edges ?? []).some((edge) => edge.from === node.id || edge.to === node.id)).length;

  /** Conteúdo publicado gera candidato real no grafo, sem depender de seed demo. */
  useEffect(() => {
    const productId = effectiveProduct?.id;
    if (!productId || seededFor.current === productId) return;
    seededFor.current = productId;
    contentService.listContentByProduct(productId)
      .then((contents) => knowledgeService.seedNodesFromContent(productId, contents))
      .then(() => setSeedRevision((current) => current + 1));
  }, [effectiveProduct?.id]);

  if (loadingNodes || loadingEdges) return <SkeletonLines />;

  return (
    <>
      <PageHeader title="Knowledge Graph" desc="Mapa operacional de relações, dependências e impactos do produto." badge={effectiveProduct?.name}>
        <PermGate allowed={canExplore}><Button onClick={() => navigate("/knowledge/search")}>Buscar entidade</Button></PermGate>
        <PermGate allowed={canExplore}><Button primary onClick={() => navigate("/knowledge/graph")}>Explorar grafo</Button></PermGate>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
        <div className="space-y-4">
          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            <KPIWidget label="Entidades totais" value={String(nodes?.length ?? 0)} detail="Conteúdo e entidades vinculadas" />
            <KPIWidget label="Relações totais" value={String(edges?.length ?? 0)} detail="Arestas do produto atual" />
            <KPIWidget label="Entidades órfãs" value={String(orphanNodes)} detail="Sem relação direta" error={orphanNodes > 0} />
            <KPIWidget label="Conteúdos sem vínculo" value={String(orphanNodes)} detail="Revisar navegação" />
            <KPIWidget label="Assets sem uso" value="—" detail="Aguardando endpoint" />
            <KPIWidget label="Formulários desconectados" value="—" detail="Aguardando endpoint" />
            <KPIWidget label="Relações recentes" value={String(edges?.length ?? 0)} detail="Criadas na sessão" />
            <KPIWidget label="Mudanças recentes" value="—" detail="Auditáveis futuramente" />
          </div>
          <Card>
            <h2 className="mb-3 text-lg font-semibold">Estados do módulo</h2>
            <div className="grid gap-3 md:grid-cols-3">
              <EmptyState compact title="Grafo vazio" description="Assim que entidades forem vinculadas, o mapa aparece." />
              <SkeletonLines />
              <PermissionHint />
            </div>
          </Card>
        </div>
        <Card><h2 className="mb-3 text-lg font-semibold">Timeline de relações</h2><KnowledgeTimeline relations={relations} /></Card>
      </div>
    </>
  );
}
