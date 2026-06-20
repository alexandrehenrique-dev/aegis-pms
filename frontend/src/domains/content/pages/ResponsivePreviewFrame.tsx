import { useState } from "react";
import { useNavigate, useParams } from "react-router";
import { Loader2 } from "lucide-react";
import { Button, Card, EmptyState, PageHeader, SelectLike, SkeletonLines } from "../../../shared/components/Primitives";
import { toast } from "../../../core/notifications/toast";
import { contentService } from "../services/contentService";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { slugify } from "../../../shared/utils/slugify";
import { ArticleBody } from "../components/ArticleBody";
import { knowledgeService } from "../../knowledge/services/knowledgeService";
import { KGBadge } from "../../knowledge/components/KGBadge";
import { pagesService } from "../../pages/services/pagesService";
import { globalsService } from "../../pages/services/globalsService";
import { BlockRenderer } from "../../pages/components/BlockRenderer";
import { GlobalFooter, GlobalNavbar } from "../../pages/components/GlobalChrome";

const LANGUAGES = ["PT-BR", "EN-US", "ES-ES"];

/** Artigo com referência kg-ref de exemplo, usado como prova de conceito da Tarefa C quando o produto atual é a WikiDev. */
function WikiDevArticlePreview({ article }: { article: { title: string; body: string } }) {
  const { data: related } = useAsyncData(() => knowledgeService.listRelated("node-spring-boot"), []);
  return (
    <div className="rounded-xl bg-muted p-8">
      <p className="text-xs text-muted-foreground">Preview de artigo publicado — WikiDev</p>
      <h2 className="mt-4 text-3xl font-semibold">{article.title}</h2>
      <div className="mt-4 max-w-xl"><ArticleBody body={article.body} /></div>
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
  const { id: pageSlug } = useParams<{ id: string }>();
  const [vp, setVp] = useState("desktop");
  const [lang, setLang] = useState(LANGUAGES[0]);
  const [submitting, setSubmitting] = useState(false);
  const { product } = useCurrentProduct();
  const productSlug = product ? slugify(product.name) : "maestro-beton";
  const { data: productContent } = useAsyncData(() => contentService.listContentByProduct(productSlug), [productSlug]);
  const wikidevArticle = productSlug === "wikidev" ? productContent?.find((c) => c.body) : undefined;
  const { data: page, loading: loadingPage } = useAsyncData(
    () => (pageSlug ? pagesService.getPageBySlug(productSlug, pageSlug) : Promise.resolve(undefined)),
    [productSlug, pageSlug],
  );
  const { data: globals } = useAsyncData(() => globalsService.getGlobals(productSlug), [productSlug]);

  const handleSubmitForReview = async () => {
    setSubmitting(true);
    try {
      await contentService.submitForReview();
      toast.success("Enviado para revisão!");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <PageHeader title={`${page?.title ?? "Preview"} — Preview`} module="Conteúdo" desc="Preview responsivo do conteúdo antes de revisão/publicação." badge="Preview">
        <Button onClick={() => navigate(-1)}>Voltar ao editor</Button>
        <Button primary onClick={handleSubmitForReview} disabled={submitting}>{submitting && <Loader2 size={15} className="animate-spin" />}{submitting ? "Enviando..." : "Enviar para revisão"}</Button>
      </PageHeader>
      <Card>
        <div className="mb-4 flex flex-wrap gap-2">
          <SelectLike label="Viewport" value={vp} options={["mobile", "tablet", "desktop"]} onChange={setVp} />
          {["mobile", "tablet", "desktop"].map((v) => <Button key={v} onClick={() => setVp(v)} primary={vp === v}>{v}</Button>)}
          <SelectLike label="Idioma" value={lang} options={LANGUAGES} onChange={setLang} />
        </div>
        <div className={`mx-auto rounded-2xl border border-border bg-white p-5 shadow-[0_8px_30px_rgba(28,28,28,.05)] ${vp === "mobile" ? "max-w-[375px]" : vp === "tablet" ? "max-w-[768px]" : "max-w-5xl"}`}>
          {wikidevArticle?.body ? (
            <WikiDevArticlePreview article={{ title: wikidevArticle.title, body: wikidevArticle.body }} />
          ) : loadingPage ? (
            <SkeletonLines />
          ) : page ? (
            <div className="divide-y divide-border">
              {globals && <GlobalNavbar globals={globals} />}
              {[...page.sections].sort((a, b) => a.order - b.order).map((section) => <BlockRenderer key={section.id} section={section} />)}
              {globals && <GlobalFooter globals={globals} />}
            </div>
          ) : (
            <EmptyState title="Preview indisponível" description={`Nenhuma página com slug "${pageSlug}" encontrada em ${productSlug}.`} />
          )}
        </div>
      </Card>
    </>
  );
}
