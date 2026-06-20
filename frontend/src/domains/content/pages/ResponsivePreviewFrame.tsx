import { useState } from "react";
import { useNavigate } from "react-router";
import { Loader2 } from "lucide-react";
import { Button, Card, PageHeader, SelectLike } from "../../../shared/components/Primitives";
import { toast } from "../../../core/notifications/toast";
import { contentService } from "../services/contentService";

const LANGUAGES = ["PT-BR", "EN-US", "ES-ES"];

export function ResponsivePreviewFrame() {
  const navigate = useNavigate();
  const [vp, setVp] = useState("desktop");
  const [lang, setLang] = useState(LANGUAGES[0]);
  const [submitting, setSubmitting] = useState(false);

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
      <PageHeader title="Home — Preview" module="Conteúdo" desc="Preview responsivo do conteúdo antes de revisão/publicação." badge="Preview">
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
          <div className="rounded-xl bg-muted p-8">
            <p className="text-xs text-muted-foreground">Preview institucional Maestro Beton</p>
            <h2 className="mt-4 text-3xl font-semibold">Experiências que conectam pessoas</h2>
            <p className="mt-2 max-w-xl text-muted-foreground">Mock simples da página Home renderizada no contexto do produto.</p>
            {/* preview do conteúdo sendo editado — não é controle da tela, não tratar como botão morto */}
            <Button primary>Solicitar orçamento</Button>
          </div>
        </div>
      </Card>
    </>
  );
}
