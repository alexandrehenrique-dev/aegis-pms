import { useState } from "react";
import { Button, Card, PageHeader } from "../../../shared/components/Primitives";

export function FormPreviewFrame() {
  const [state, setState] = useState("desktop");
  return (
    <>
      <PageHeader title="Preview do Formulário" module="Forms" desc="Valide desktop, tablet, mobile e estados antes da publicação." badge="Preview">
        <Button>Voltar</Button>
        <Button primary>Enviar teste</Button>
      </PageHeader>
      <Card>
        <div className="mb-4 flex flex-wrap gap-2">{["desktop", "tablet", "mobile", "vazio", "preenchido", "inválido", "enviado", "erro"].map((v) => <Button key={v} onClick={() => setState(v)}>{v}</Button>)}</div>
        <div className={`mx-auto rounded-2xl border border-border bg-white p-5 ${state === "mobile" ? "max-w-[375px]" : state === "tablet" ? "max-w-[768px]" : "max-w-4xl"}`}>
          <h2 className="text-2xl font-semibold">Contato Comercial</h2>
          <p className="mt-1 text-sm text-muted-foreground">Solicite uma proposta para o Maestro Beton.</p>
          {["Nome", "Email", "Telefone", "Mensagem"].map((f, i) => (
            <div key={f} className="mt-3">
              <label className="text-sm font-medium">{f}</label>
              <div className={`mt-1 h-11 rounded-lg border ${state === "inválido" && i === 1 ? "border-destructive" : "border-border"} bg-muted/40`} />
            </div>
          ))}
          {state === "enviado" && <div className="mt-3 rounded-xl bg-[#ede9fe] p-3 text-sm text-primary">Formulário enviado. Lead criado.</div>}
          <Button primary>Enviar</Button>
        </div>
      </Card>
    </>
  );
}
