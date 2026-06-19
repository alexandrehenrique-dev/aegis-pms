import { useState } from "react";
import { Languages } from "lucide-react";
import { Button, Card, PageHeader, SelectLike } from "../../../shared/components/Primitives";

export function ResponsivePreviewFrame() {
  const [vp, setVp] = useState("desktop");
  return (
    <>
      <PageHeader title="Home — Preview" module="Conteúdo" desc="Preview responsivo do conteúdo antes de revisão/publicação." badge="Preview">
        <Button>Voltar ao editor</Button>
        <Button primary>Enviar para revisão</Button>
      </PageHeader>
      <Card>
        <div className="mb-4 flex flex-wrap gap-2">
          <SelectLike label="Viewport" value={vp} />
          {["mobile", "tablet", "desktop"].map((v) => <Button key={v} onClick={() => setVp(v)}>{v}</Button>)}
          <Button><Languages size={15} />PT-BR</Button>
        </div>
        <div className={`mx-auto rounded-2xl border border-border bg-white p-5 shadow-[0_8px_30px_rgba(28,28,28,.05)] ${vp === "mobile" ? "max-w-[375px]" : vp === "tablet" ? "max-w-[768px]" : "max-w-5xl"}`}>
          <div className="rounded-xl bg-muted p-8">
            <p className="text-xs text-muted-foreground">Preview institucional Maestro Beton</p>
            <h2 className="mt-4 text-3xl font-semibold">Experiências que conectam pessoas</h2>
            <p className="mt-2 max-w-xl text-muted-foreground">Mock simples da página Home renderizada no contexto do produto.</p>
            <Button primary>Solicitar orçamento</Button>
          </div>
        </div>
      </Card>
    </>
  );
}
