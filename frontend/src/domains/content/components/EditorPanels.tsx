import { useState } from "react";
import { AlertTriangle, Plus, Send } from "lucide-react";
import { Badge, Button, Card, Field, SelectLike } from "../../../shared/components/Primitives";
import { PermissionHint } from "../../../shared/components/Primitives";
import { ContentStatusBadge } from "./ContentStatusBadge";
import { VersionTimeline } from "./VersionTimeline";

export function ContentStructureTree() {
  return (
    <Card className="h-full">
      <h2 className="mb-3 text-lg font-semibold">Estrutura</h2>
      {["Hero", "Experiências", "Vídeo destaque", "Sobre", "Galeria", "Depoimentos", "CTA final", "SEO"].map((b, i) => (
        <div key={b} className={`mb-1 flex items-center justify-between rounded-lg p-2 text-sm ${i === 0 ? "bg-muted" : "hover:bg-muted"}`}>
          <span>{b}</span>
          <span className="flex gap-1"><Badge>{i === 7 ? "SEO" : "Bloco"}</Badge>{i === 4 && <AlertTriangle size={14} className="text-[#8A5A12]" />}</span>
        </div>
      ))}
      <Button><Plus size={15} />Adicionar bloco</Button>
    </Card>
  );
}

export function BlockEditorCanvas() {
  return (
    <Card>
      <h2 className="text-lg font-semibold">Editor / Canvas — Hero</h2>
      <p className="mb-4 text-sm text-muted-foreground">Cockpit de conteúdo por blocos, não textarea gigante.</p>
      <div className="grid gap-3 md:grid-cols-2">
        <Field label="Título" value="Maestro Beton: experiências que conectam pessoas" />
        <Field label="Subtítulo" value="Eventos, cultura e encontros em uma plataforma institucional governada." />
        <Field label="CTA principal" value="Solicitar orçamento" />
        <Field label="CTA secundário" value="Ver apresentações" />
        <SelectLike label="Imagem hero" value="hero-maestro-beton.jpg" />
        <SelectLike label="Alinhamento" value="Centro" />
        <SelectLike label="Visibilidade" value="Visível" />
      </div>
      <div className="mt-5 rounded-2xl border border-border bg-muted p-6">
        <p className="font-mono text-xs text-muted-foreground">Preview parcial</p>
        <h3 className="mt-3 max-w-xl text-2xl font-semibold">Maestro Beton: experiências que conectam pessoas</h3>
        <p className="mt-2 max-w-xl text-sm text-muted-foreground">Visual institucional simulado dentro do editor.</p>
        <div className="mt-4 flex gap-2"><Button primary>Solicitar orçamento</Button><Button>Ver apresentações</Button></div>
      </div>
    </Card>
  );
}

function SEOPanel() {
  return (
    <div className="space-y-3">
      <Field label="Title" value="Maestro Beton | Experiências" />
      <Field label="Description" value="Conheça experiências e apresentações do Maestro Beton." textarea />
      <Field label="Keywords" value="maestro, eventos, apresentações" />
      <div className="rounded-lg border border-border p-3 text-sm">
        <b>Preview Google</b>
        <p className="text-[#1F5FA8]">Maestro Beton | Experiências</p>
        <p className="text-muted-foreground">Conheça experiências e apresentações...</p>
      </div>
    </div>
  );
}

function WorkflowPanel() {
  return (
    <div className="space-y-2 text-sm">
      <ContentStatusBadge status="Draft" />
      <Button><Send size={15} />Enviar para revisão</Button>
      <Button primary>Publicar</Button>
      <Button>Arquivar</Button>
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
