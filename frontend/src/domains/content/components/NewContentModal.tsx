import { useState } from "react";
import { useNavigate } from "react-router";
import { ConfirmDialog } from "../../../shared/components/ConfirmDialog";
import { Field, SelectLike } from "../../../shared/components/Primitives";
import { toast } from "../../../core/notifications/toast";
import { contentService } from "../services/contentService";

const TYPES = ["Post", "Manifesto", "Reflexão", "Página de produto"];
const LANGS = ["PT-BR", "EN-US", "ES-ES"];

/**
 * Cria um artigo do domínio `content` (Sprint 12, Tarefa B) — substitui a
 * navegação antiga para `/content/new/editor`, que na prática abria o editor
 * de blocos de `pages` sempre na primeira página do produto.
 */
export function NewContentModal({ onClose }: { onClose: () => void }) {
  const navigate = useNavigate();
  const [title, setTitle] = useState("");
  const [type, setType] = useState(TYPES[0]);
  const [lang, setLang] = useState(LANGS[0]);
  const [author, setAuthor] = useState("");
  const [creating, setCreating] = useState(false);

  const handleCreate = async () => {
    setCreating(true);
    try {
      const created = await contentService.createContent({ title, type, lang, author: author || "Eu" });
      toast.success("Artigo criado", { description: `${created.title} entrou como rascunho.` });
      onClose();
      navigate("/content/list");
    } finally {
      setCreating(false);
    }
  };

  return (
    <ConfirmDialog
      title="Novo conteúdo"
      desc="Cria um artigo (post, manifesto, reflexão) do domínio editorial — não uma página institucional."
      onCancel={onClose}
      onConfirm={handleCreate}
      loading={creating}
      confirmDisabled={!title.trim()}
      confirmLabel="Criar artigo"
    >
      <div className="space-y-3">
        <Field label="Título" value={title} onChange={setTitle} />
        <SelectLike label="Tipo" value={type} options={TYPES} onChange={setType} />
        <SelectLike label="Idioma" value={lang} options={LANGS} onChange={setLang} />
        <Field label="Autor" value={author} onChange={setAuthor} />
      </div>
    </ConfirmDialog>
  );
}
