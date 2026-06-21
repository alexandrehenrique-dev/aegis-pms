import { useState } from "react";
import { useNavigate } from "react-router";
import { ConfirmDialog } from "../../../shared/components/ConfirmDialog";
import { Field, SelectLike } from "../../../shared/components/Primitives";
import { toast } from "../../../core/notifications/toast";
import { contentService } from "../services/contentService";
import { pagesService } from "../../pages/services/pagesService";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";
import { slugify } from "../../../shared/utils/slugify";

const PAGE_TYPE = "Página de produto";
const TYPES = ["Post", "Manifesto", "Reflexão", PAGE_TYPE];
const LANGS = ["PT-BR", "EN-US", "ES-ES"];
const LANG_TO_LOCALE: Record<string, string> = { "PT-BR": "pt-BR", "EN-US": "en-US", "ES-ES": "es-ES" };

/**
 * Cria um artigo do domínio `content` (Sprint 12, Tarefa B) — substitui a
 * navegação antiga para `/content/new/editor`, que na prática abria o editor
 * de blocos de `pages` sempre na primeira página do produto.
 *
 * "Página de produto" (Sprint 15, Tarefa A.2) não é um `Content` — é um
 * desvio de fluxo explícito para o domínio `pages`: ao escolher esse tipo,
 * `handleCreate` cria uma `Page` em branco via `pagesService.createPage` e
 * navega para o editor de blocos (`PageEditor`), nunca para o editor de
 * artigo do domínio `content`.
 */
export function NewContentModal({ onClose }: { onClose: () => void }) {
  const navigate = useNavigate();
  const { product } = useCurrentProduct();
  const productSlug = product ? slugify(product.name) : "maestro-beton";
  const [title, setTitle] = useState("");
  const [type, setType] = useState(TYPES[0]);
  const [lang, setLang] = useState(LANGS[0]);
  const [author, setAuthor] = useState("");
  const [creating, setCreating] = useState(false);

  const handleCreate = async () => {
    setCreating(true);
    try {
      if (type === PAGE_TYPE) {
        const created = await pagesService.createPage(productSlug, { title, slug: slugify(title), locale: LANG_TO_LOCALE[lang] });
        toast.success("Página criada", { description: created.title });
        onClose();
        navigate(`/pages/${created.slug}/editor`);
        return;
      }
      const created = await contentService.createContent({ title, type, lang, author: author || "Eu" }, product);
      toast.success("Artigo criado", { description: `${created.title} entrou como rascunho.` });
      onClose();
      navigate(`/content/${created.id}/editor`);
    } finally {
      setCreating(false);
    }
  };

  return (
    <ConfirmDialog
      title="Novo conteúdo"
      desc="Artigo (post, manifesto, reflexão) do domínio editorial, ou uma página institucional em branco do domínio de páginas."
      onCancel={onClose}
      onConfirm={handleCreate}
      loading={creating}
      confirmDisabled={!title.trim()}
      confirmLabel={type === PAGE_TYPE ? "Criar página" : "Criar artigo"}
    >
      <div className="space-y-3">
        <Field label="Título" value={title} onChange={setTitle} />
        <SelectLike label="Tipo" value={type} options={TYPES} onChange={setType} />
        <SelectLike label="Idioma" value={lang} options={LANGS} onChange={setLang} />
        {type !== PAGE_TYPE && <Field label="Autor" value={author} onChange={setAuthor} />}
      </div>
    </ConfirmDialog>
  );
}
