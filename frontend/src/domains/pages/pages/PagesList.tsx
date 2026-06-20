import { useEffect, useState } from "react";
import { useNavigate } from "react-router";
import { AnimatePresence } from "motion/react";
import { Plus, Trash2 } from "lucide-react";
import { Badge, Button, Card, EmptyState, Field, PageHeader, SkeletonLines, PartialErrorWidget } from "../../../shared/components/Primitives";
import { ConfirmDialog } from "../../../shared/components/ConfirmDialog";
import { toast } from "../../../core/notifications/toast";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { slugify } from "../../../shared/utils/slugify";
import { pagesService } from "../services/pagesService";
import type { Page } from "../contracts/responses";

const LOCALES = ["pt-BR", "en-US", "es-ES"];

const STATUS_TONE: Record<Page["status"], "neutral" | "blue" | "green" | "amber"> = {
  draft: "neutral",
  review: "amber",
  published: "green",
  archived: "neutral",
};

function NewPageModal({ onClose, onCreate }: { onClose: () => void; onCreate: (req: { title: string; slug: string; locale: string }) => Promise<void> }) {
  const [title, setTitle] = useState("");
  const [slug, setSlug] = useState("");
  const [locale, setLocale] = useState(LOCALES[0]);
  const [creating, setCreating] = useState(false);
  const [touchedSlug, setTouchedSlug] = useState(false);

  const handleTitleChange = (v: string) => {
    setTitle(v);
    if (!touchedSlug) setSlug(slugify(v));
  };

  const handleCreate = async () => {
    setCreating(true);
    try {
      await onCreate({ title, slug, locale });
    } finally {
      setCreating(false);
    }
  };

  return (
    <ConfirmDialog
      title="Nova página"
      desc="Cria uma página institucional vazia, pronta para receber blocos no editor."
      onCancel={onClose}
      onConfirm={handleCreate}
      loading={creating}
      confirmDisabled={!title.trim() || !slug.trim()}
      confirmLabel="Criar página"
    >
      <div className="space-y-3">
        <Field label="Título" value={title} onChange={handleTitleChange} />
        <Field label="Slug" value={slug} onChange={(v) => { setTouchedSlug(true); setSlug(slugify(v)); }} />
        <label className="block">
          <span className="mb-1 block text-sm font-medium">Idioma</span>
          <select value={locale} onChange={(e) => setLocale(e.target.value)} className="w-full rounded-lg border border-border bg-card p-3 text-sm outline-primary">
            {LOCALES.map((l) => <option key={l} value={l}>{l}</option>)}
          </select>
        </label>
      </div>
    </ConfirmDialog>
  );
}

export function PagesList() {
  const navigate = useNavigate();
  const { product } = useCurrentProduct();
  const productSlug = product ? slugify(product.name) : "maestro-beton";

  const { data: loadedPages, loading, error } = useAsyncData(() => pagesService.listPages(productSlug), [productSlug]);
  const [pages, setPages] = useState<Page[]>([]);
  const [showNewPage, setShowNewPage] = useState(false);
  const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => { setPages(loadedPages ?? []); }, [loadedPages]);

  const pendingDeletePage = pages.find((p) => p.id === pendingDeleteId) ?? null;

  const handleCreate = async (req: { title: string; slug: string; locale: string }) => {
    const created = await pagesService.createPage(productSlug, req);
    toast.success("Página criada", { description: created.title });
    setShowNewPage(false);
    navigate(`/content/${created.slug}/editor`);
  };

  const handleDelete = async () => {
    if (!pendingDeletePage) return;
    setDeleting(true);
    try {
      await pagesService.deletePage(productSlug, pendingDeletePage.id);
      setPages((prev) => prev.filter((p) => p.id !== pendingDeletePage.id));
      toast.success("Página removida", { description: pendingDeletePage.title });
      setPendingDeleteId(null);
    } finally {
      setDeleting(false);
    }
  };

  if (loading) return <SkeletonLines />;
  if (error) return <PartialErrorWidget />;

  return (
    <>
      <AnimatePresence>{showNewPage && <NewPageModal onClose={() => setShowNewPage(false)} onCreate={handleCreate} />}</AnimatePresence>
      {pendingDeletePage && (
        <ConfirmDialog
          title={`Excluir página "${pendingDeletePage.title}"?`}
          desc="Esta ação é irreversível: a página e todas as suas seções/blocos são removidos."
          danger
          loading={deleting}
          onCancel={() => setPendingDeleteId(null)}
          onConfirm={handleDelete}
        />
      )}
      <PageHeader title="Páginas" desc="Páginas institucionais deste produto — cada uma é composta por seções e blocos." badge={product?.name ?? "Produto"}>
        <Button onClick={() => navigate("/products/globals")}>Navbar, footer e redes sociais</Button>
        <Button primary onClick={() => setShowNewPage(true)}><Plus size={15} />Nova página</Button>
      </PageHeader>
      {pages.length === 0 ? (
        <EmptyState title="Nenhuma página criada" description="Crie a primeira página institucional deste produto." />
      ) : (
        <div className="grid gap-3">
          {pages.map((p) => (
            <Card key={p.id}>
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="font-semibold">{p.title}</h3>
                    <Badge tone={STATUS_TONE[p.status]}>{p.status}</Badge>
                  </div>
                  <p className="mt-0.5 text-xs text-muted-foreground">/{p.slug} · {p.locale} · v{p.version} · {p.sections.length} seções</p>
                </div>
                <div className="flex gap-2">
                  <Button onClick={() => navigate(`/content/${p.slug}/editor`)}>Editar</Button>
                  <Button onClick={() => setPendingDeleteId(p.id)}><Trash2 size={14} />Excluir</Button>
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}
    </>
  );
}
