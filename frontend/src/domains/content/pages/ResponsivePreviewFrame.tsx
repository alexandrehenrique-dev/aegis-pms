import { useState } from "react";
import { useNavigate, useParams } from "react-router";
import { Loader2 } from "lucide-react";
import { Badge, Button, Card, EmptyState, PageHeader, SelectLike, SkeletonLines } from "../../../shared/components/Primitives";
import { toast } from "../../../core/notifications/toast";
import { contentService } from "../services/contentService";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { ArticleBody } from "../components/ArticleBody";
import { knowledgeService } from "../../knowledge/services/knowledgeService";
import { KGBadge } from "../../knowledge/components/KGBadge";
import { pagesService } from "../../pages/services/pagesService";
import { globalsService } from "../../pages/services/globalsService";
import { BlockRenderer } from "../../pages/components/BlockRenderer";
import { GlobalFooter, GlobalNavbar } from "../../pages/components/GlobalChrome";

const LANGUAGES = ["PT-BR", "EN-US", "ES-ES"];

/** Artigo com referência kg-ref de exemplo, usado como prova de conceito da Tarefa C quando o produto atual é a WikiDev. */
function WikiDevArticlePreview({ article, productId }: { article: { title: string; body: string }; productId: string }) {
  const { data: related } = useAsyncData(() => knowledgeService.listRelated(productId, "node-spring-boot"), [productId]);
  return (
    <div className="rounded-xl bg-muted p-8">
      <p className="text-xs text-muted-foreground">Preview de artigo publicado — WikiDev</p>
      <h2 className="mt-4 text-3xl font-semibold">{article.title}</h2>
      <div className="mt-4 max-w-xl"><ArticleBody body={article.body} forceLightProse /></div>
      {related && related.length > 0 && (
        <div className="mt-6 max-w-xl rounded-lg border border-border bg-card p-3">
          <p className="mb-2 text-xs font-semibold uppercase tracking-wider text-muted-foreground">Relacionados</p>
          <div className="space-y-1">
            {related.map((r) => (
              <div key={r.node.id} className="flex items-center justify-between text-sm">
                <span>{r.node.label}</span>
                <span className="flex items-center gap-2"><KGBadge type={r.node.type} /><span className="text-xs text-muted-foreground">peso {r.weight}</span></span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

export function ResponsivePreviewFrame() {
  const navigate = useNavigate();
  const { id: previewId } = useParams<{ id: string }>();
  const [vp, setVp] = useState("desktop");
  const [lang, setLang] = useState(LANGUAGES[0]);
  const [submitting, setSubmitting] = useState(false);
  const { product } = useCurrentProduct();
  const productId = product ? product.id : "p1";
  const { data: productContent } = useAsyncData(() => contentService.listContentByProduct(productId), [productId]);
  const contentPreview = productContent?.find((c) => c.id === previewId);
  const wikidevArticle = product?.name === "WikiDev" ? productContent?.find((c) => c.body) : undefined;
  const { data: page, loading: loadingPage } = useAsyncData(
    () => (previewId ? pagesService.getPageBySlug(productId, previewId) : Promise.resolve(undefined)),
    [productId, previewId],
  );
  const { data: globals } = useAsyncData(() => globalsService.getGlobals(productId), [productId]);

  const handleSubmitForReview = async () => {
    setSubmitting(true);
    try {
      if (page) await pagesService.updatePage(productId, page.id, { status: "review" });
      toast.success("Enviado para revisão!");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <PageHeader title={`${contentPreview?.title ?? page?.title ?? "Preview"} — Preview`} module="Conteúdo" desc="Preview responsivo do conteúdo antes de revisão/publicação." badge={contentPreview?.status ?? "Preview"}>
        <Button onClick={() => navigate(-1)}>Voltar ao editor</Button>
        <Button primary onClick={handleSubmitForReview} disabled={submitting}>{submitting && <Loader2 size={15} className="animate-spin" />}{submitting ? "Enviando..." : "Enviar para revisão"}</Button>
      </PageHeader>
      <Card>
        <div className="mb-4 flex flex-wrap gap-2">
          <SelectLike label="Viewport" value={vp} options={["mobile", "tablet", "desktop"]} onChange={setVp} />
          {["mobile", "tablet", "desktop"].map((v) => <Button key={v} onClick={() => setVp(v)} primary={vp === v}>{v}</Button>)}
          <SelectLike label="Idioma" value={lang} options={LANGUAGES} onChange={setLang} />
        </div>
        <div className={`light isolate mx-auto rounded-2xl border border-border bg-white p-5 shadow-[0_8px_30px_rgba(28,28,28,.05)] ${vp === "mobile" ? "max-w-[375px]" : vp === "tablet" ? "max-w-[768px]" : "max-w-5xl"}`}>
          <div className="mb-6 flex flex-wrap items-center justify-between gap-3 border-b border-border pb-4">
            <div>
              <p className="text-xs font-semibold uppercase tracking-wider text-primary">Aegis · {product?.name ?? "Produto"}</p>
              <h1 className="mt-1 text-2xl font-semibold">{contentPreview?.title ?? page?.title ?? "Preview"}</h1>
            </div>
            <Badge>{contentPreview?.status ?? page?.status ?? "preview"}</Badge>
          </div>
          {contentPreview?.body ? (
            <article className="mx-auto max-w-3xl py-6">
              <ArticleBody body={contentPreview.body} forceLightProse />
            </article>
          ) : wikidevArticle?.body ? (
            <WikiDevArticlePreview article={{ title: wikidevArticle.title, body: wikidevArticle.body }} productId={productId} />
          ) : loadingPage ? (
            <SkeletonLines />
          ) : page ? (
            <div className="divide-y divide-border">
              {globals && <GlobalNavbar globals={globals} />}
              {[...page.sections].sort((a, b) => a.order - b.order).map((section) => <BlockRenderer key={section.id} section={section} forceLightProse />)}
              {globals && <GlobalFooter globals={globals} />}
            </div>
          ) : (
            <EmptyState title="Preview indisponível" description={`Nenhuma página ou conteúdo com referência "${previewId}" foi encontrado neste produto.`} />
          )}
        </div>
      </Card>
    </>
  );
}
