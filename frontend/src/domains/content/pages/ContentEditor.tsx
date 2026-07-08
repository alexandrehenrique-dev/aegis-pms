import { useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router";
import { AnimatePresence } from "motion/react";
import { Button, EmptyState, PageHeader, SkeletonLines } from "../../../shared/components/Primitives";
import { FloatingSaveStatus, type SaveStatus } from "../../../shared/components/FloatingSaveStatus";
import { toast } from "../../../core/notifications/toast";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";
import { resolveEnabledModules } from "../../../core/products/moduleDefaults";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { contentService } from "../services/contentService";
import { ContentArticleEditor } from "../components/ContentArticleEditor";
import type { ContentRow } from "../contracts/responses";

const CONTENT_SAVE_DEBOUNCE_MS = 500;

/**
 * Editor dedicado do domínio `content` (Sprint 15, Tarefa A) — substitui a
 * versão antiga deste componente, que carregava e editava uma `Page`
 * institucional via `pagesService.getPageBySlug` por engano (esse motor de
 * blocos virou `domains/pages/pages/PageEditor.tsx`). Resolve por `id` real
 * do `Content` via `contentService.getContent`, nunca por slug improvisado
 * de título.
 */
export function ContentEditor() {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const { product } = useCurrentProduct();
  const knowledgeGraphEnabled = resolveEnabledModules(product).includes("Knowledge Graph");
  const productId = product ? product.id : "p1";

  const { data: foundContent, loading } = useAsyncData(() => (id ? contentService.getContent(productId, id) : Promise.resolve(undefined)), [productId, id]);
  const [content, setContent] = useState<ContentRow | null>(null);
  const [saveStatus, setSaveStatus] = useState<SaveStatus>("idle");
  const pendingPatch = useRef<Partial<ContentRow> | null>(null);
  const debounceTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => setContent(foundContent ?? null), [foundContent]);
  useEffect(() => () => { if (debounceTimer.current) clearTimeout(debounceTimer.current); }, []);

  /** Debounçado (Tarefa F) — persiste só `CONTENT_SAVE_DEBOUNCE_MS` depois da última tecla, em vez de uma chamada por caractere digitado. */
  const handleChange = (patch: Partial<ContentRow>) => {
    if (!id) return;
    setContent((prev) => (prev ? { ...prev, ...patch } : prev));
    setSaveStatus("dirty");
    pendingPatch.current = { ...pendingPatch.current, ...patch };
    if (debounceTimer.current) clearTimeout(debounceTimer.current);
    debounceTimer.current = setTimeout(async () => {
      const patchToSave = pendingPatch.current;
      pendingPatch.current = null;
      if (!patchToSave) return;
      setSaveStatus("saving");
      await contentService.updateContent(productId, id, patchToSave, product);
      setSaveStatus("saved");
      setTimeout(() => setSaveStatus("idle"), 3000);
    }, CONTENT_SAVE_DEBOUNCE_MS);
  };

  /** J.4.1 (BUG-SPRINT-05) — antes sem try/catch: uma transição inválida (ex.: conteúdo já `Published`) lançava e deixava o usuário preso na tela sem nenhum feedback (nem toast, nem navegação). */
  const handleSubmitForReview = async () => {
    if (!id || !content) return;
    try {
      await contentService.submitForReview(id, productId, content.status);
      setContent((prev) => (prev ? { ...prev, status: "In Review" } : prev));
      toast.success("Enviado para revisão.", { description: "A equipe editorial será notificada." });
      // Tarefa C.1 — antes ficava preso na tela de edição de um conteúdo que já
      // não está mais em edição; agora volta para o Kanban onde o item aparece
      // na coluna "Em revisão".
      navigate("/content/workflow");
    } catch (err: unknown) {
      const code = (err as { code?: string }).code;
      const message = code === "INVALID_CONTENT_TRANSITION"
        ? "Esta transição não é permitida para o status atual do conteúdo."
        : ((err as { message?: string }).message ?? "Tente novamente.");
      toast.error("Não foi possível enviar para revisão.", { description: message });
    }
  };

  if (!loading && !content) {
    return (
      <EmptyState
        title="Conteúdo não encontrado"
        description={`Nenhum conteúdo com id "${id}" foi encontrado. Volte para a lista e escolha um item existente.`}
      />
    );
  }

  if (loading || !content) return <SkeletonLines />;

  return (
    <>
      <AnimatePresence>{saveStatus !== "idle" && <FloatingSaveStatus key={saveStatus} status={saveStatus} />}</AnimatePresence>
      <PageHeader title={`${content.title} — Editar`} module="Conteúdo" desc="Edite título, corpo em markdown e metadados deste item editorial." badge={content.status}>
        <Button onClick={() => navigate(`/content/${content.id}/preview`)}>Preview</Button>
        {/* J.4.2 — "Enviar para revisão" só faz sentido a partir de Draft; para os demais status, a transição pertence ao Workflow Board (In Review) ou já não é mais permitida (Published/Archived). */}
        {content.status === "Draft" && (
          <Button primary onClick={handleSubmitForReview}>Enviar para revisão</Button>
        )}
      </PageHeader>
      <ContentArticleEditor content={content} knowledgeGraphEnabled={knowledgeGraphEnabled} productSlug={productId} onChange={handleChange} />
    </>
  );
}
