import { useState } from "react";
import { AlertTriangle, Loader2, Plus, Send } from "lucide-react";
import { Badge, Button, Card, Field, SelectLike } from "../../../shared/components/Primitives";
import { PermissionHint } from "../../../shared/components/Primitives";
import { toast } from "../../../core/notifications/toast";
import { contentService } from "../services/contentService";
import { ContentStatusBadge } from "./ContentStatusBadge";
import { VersionTimeline } from "./VersionTimeline";

const INITIAL_BLOCKS = ["Hero", "Experiências", "Vídeo destaque", "Sobre", "Galeria", "Depoimentos", "CTA final", "SEO"];

export function ContentStructureTree() {
  const [blocks, setBlocks] = useState(INITIAL_BLOCKS);
  const [selected, setSelected] = useState(blocks[0]);

  const handleAddBlock = () => {
    const name = `Novo bloco ${blocks.length + 1}`;
    setBlocks((prev) => [...prev, name]);
    setSelected(name);
    toast.success("Bloco adicionado", { description: name });
  };

  return (
    <Card className="h-full">
      <h2 className="mb-3 text-lg font-semibold">Estrutura</h2>
      {blocks.map((b, i) => (
        <button key={b} onClick={() => setSelected(b)} className={`mb-1 flex w-full items-center justify-between rounded-lg p-2 text-left text-sm ${selected === b ? "bg-muted" : "hover:bg-muted"}`}>
          <span>{b}</span>
          <span className="flex gap-1"><Badge>{b === "SEO" ? "SEO" : "Bloco"}</Badge>{i === 4 && <AlertTriangle size={14} className="text-[#8A5A12]" />}</span>
        </button>
      ))}
      <Button onClick={handleAddBlock}><Plus size={15} />Adicionar bloco</Button>
    </Card>
  );
}

export function BlockEditorCanvas() {
  const [title, setTitle] = useState("Maestro Beton: experiências que conectam pessoas");
  const [subtitle, setSubtitle] = useState("Eventos, cultura e encontros em uma plataforma institucional governada.");
  const [ctaPrimary, setCtaPrimary] = useState("Solicitar orçamento");
  const [ctaSecondary, setCtaSecondary] = useState("Ver apresentações");
  const [heroImage, setHeroImage] = useState("hero-maestro-beton.jpg");
  const [alignment, setAlignment] = useState("Centro");
  const [visibility, setVisibility] = useState("Visível");

  return (
    <Card>
      <h2 className="text-lg font-semibold">Editor / Canvas — Hero</h2>
      <p className="mb-4 text-sm text-muted-foreground">Cockpit de conteúdo por blocos, não textarea gigante.</p>
      <div className="grid gap-3 md:grid-cols-2">
        <Field label="Título" value={title} onChange={setTitle} />
        <Field label="Subtítulo" value={subtitle} onChange={setSubtitle} />
        <Field label="CTA principal" value={ctaPrimary} onChange={setCtaPrimary} />
        <Field label="CTA secundário" value={ctaSecondary} onChange={setCtaSecondary} />
        <SelectLike label="Imagem hero" value={heroImage} options={["hero-maestro-beton.jpg", "hero-evento-2024.jpg", "hero-equipe.jpg"]} onChange={setHeroImage} />
        <SelectLike label="Alinhamento" value={alignment} options={["Esquerda", "Centro", "Direita"]} onChange={setAlignment} />
        <SelectLike label="Visibilidade" value={visibility} options={["Visível", "Oculto"]} onChange={setVisibility} />
      </div>
      <div className="mt-5 rounded-2xl border border-border bg-muted p-6">
        <p className="font-mono text-xs text-muted-foreground">Preview parcial</p>
        <h3 className="mt-3 max-w-xl text-2xl font-semibold">{title}</h3>
        <p className="mt-2 max-w-xl text-sm text-muted-foreground">{subtitle}</p>
        {/* preview do conteúdo sendo editado — não é controle da tela, não tratar como botão morto */}
        <div className="mt-4 flex gap-2"><Button primary>{ctaPrimary}</Button><Button>{ctaSecondary}</Button></div>
      </div>
    </Card>
  );
}

function SEOPanel() {
  const [title, setTitle] = useState("Maestro Beton | Experiências");
  const [description, setDescription] = useState("Conheça experiências e apresentações do Maestro Beton.");
  const [keywords, setKeywords] = useState("maestro, eventos, apresentações");
  return (
    <div className="space-y-3">
      <Field label="Title" value={title} onChange={setTitle} />
      <Field label="Description" value={description} onChange={setDescription} textarea />
      <Field label="Keywords" value={keywords} onChange={setKeywords} />
      <div className="rounded-lg border border-border p-3 text-sm">
        <b>Preview Google</b>
        <p className="text-[#1F5FA8]">{title}</p>
        <p className="text-muted-foreground">{description}</p>
      </div>
    </div>
  );
}

function WorkflowPanel() {
  const [status, setStatus] = useState<"Draft" | "In Review" | "Published" | "Archived">("Draft");
  const [busy, setBusy] = useState<string | null>(null);

  const run = async (label: string, action: () => Promise<void>, nextStatus: typeof status) => {
    setBusy(label);
    try {
      await action();
      setStatus(nextStatus);
      toast.success(`${label}!`);
    } finally {
      setBusy(null);
    }
  };

  return (
    <div className="space-y-2 text-sm">
      <ContentStatusBadge status={status} />
      <Button onClick={() => run("Enviado para revisão", contentService.submitForReview, "In Review")} disabled={busy !== null}>
        {busy === "Enviado para revisão" ? <Loader2 size={15} className="animate-spin" /> : <Send size={15} />}Enviar para revisão
      </Button>
      <Button primary onClick={() => run("Publicado", contentService.publish, "Published")} disabled={busy !== null}>
        {busy === "Publicado" && <Loader2 size={15} className="animate-spin" />}Publicar
      </Button>
      <Button onClick={() => run("Arquivado", contentService.archive, "Archived")} disabled={busy !== null}>
        {busy === "Arquivado" && <Loader2 size={15} className="animate-spin" />}Arquivar
      </Button>
      <PermissionHint />
    </div>
  );
}

export function PropertiesPanel() {
  const [tab, setTab] = useState("Propriedades");
  return (
    <Card className="h-full">
      <div className="mb-3 flex gap-1 overflow-auto">
        {["Propriedades", "SEO", "Workflow", "Histórico"].map((t) => (
          <button key={t} onClick={() => setTab(t)} className={`rounded-lg px-2 py-1 text-xs ${tab === t ? "bg-primary text-white" : "bg-muted"}`}>{t}</button>
        ))}
      </div>
      {tab === "SEO" ? <SEOPanel /> : tab === "Workflow" ? <WorkflowPanel /> : tab === "Histórico" ? <VersionTimeline compact /> : (
        <div className="space-y-2 text-sm">
          {[["slug", "home"], ["tipo", "Página"], ["idioma", "PT-BR"], ["autor", "Marina Costa"], ["status", "Draft"], ["versão", "v18"], ["última atualização", "há 2 min"]].map((x) => (
            <div key={x[0]} className="flex justify-between rounded-lg bg-muted p-2"><span>{x[0]}</span><b>{x[1]}</b></div>
          ))}
        </div>
      )}
    </Card>
  );
}
