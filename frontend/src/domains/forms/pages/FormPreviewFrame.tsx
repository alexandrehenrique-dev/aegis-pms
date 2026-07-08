import { useState } from "react";
import { useNavigate, useParams } from "react-router";
import { Button, Card, PageHeader, SkeletonLines, PartialErrorWidget } from "../../../shared/components/Primitives";
import { toast } from "../../../core/notifications/toast";
import { formsService } from "../services/formsService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";

/** H.3.3 (BUG-SPRINT-05) — antes navegava para `/forms/preview` sem `formId`, abrindo sempre o mesmo preview genérico ("Contato Comercial" hardcoded); agora lê o form real via rota `/forms/:formId/preview`. */
export function FormPreviewFrame() {
  const navigate = useNavigate();
  const { formId } = useParams<{ formId: string }>();
  const { product } = useCurrentProduct();
  const productId = product?.id ?? "";
  const [state, setState] = useState("desktop");

  const { data: form, loading: loadingForm } = useAsyncData(
    () => (productId && formId ? formsService.getForm(productId, formId) : Promise.resolve(undefined)),
    [productId, formId],
  );
  const { data: fields, loading: loadingFields, error } = useAsyncData(
    () => (productId && formId ? formsService.getFormFields(productId, formId) : Promise.resolve([])),
    [productId, formId],
  );

  const handleTestSubmit = async () => {
    await formsService.submitTest(productId, formId);
    toast.success("Envio de teste registrado");
  };

  if (!formId) return <Card><p className="text-sm text-muted-foreground">Nenhum formulário informado para preview.</p></Card>;
  if (loadingForm || loadingFields) return <SkeletonLines />;
  if (error) return <PartialErrorWidget />;

  return (
    <>
      <PageHeader title="Preview do Formulário" module="Forms" desc="Valide desktop, tablet, mobile e estados antes da publicação." badge="Preview">
        <Button onClick={() => navigate(-1)}>Voltar</Button>
        <Button primary onClick={handleTestSubmit}>Enviar teste</Button>
      </PageHeader>
      <Card>
        <div className="mb-4 flex flex-wrap gap-2">{["desktop", "tablet", "mobile", "vazio", "preenchido", "inválido", "enviado", "erro"].map((v) => <Button key={v} onClick={() => setState(v)}>{v}</Button>)}</div>
        <div className={`light isolate mx-auto rounded-2xl border border-border bg-white p-5 ${state === "mobile" ? "max-w-[375px]" : state === "tablet" ? "max-w-[768px]" : "max-w-4xl"}`}>
          <h2 className="text-2xl font-semibold">{form?.name ?? "Formulário"}</h2>
          <p className="mt-1 text-sm text-muted-foreground">{form?.type ? `Tipo: ${form.type}` : ""}</p>
          {(fields ?? []).length === 0 ? (
            <p className="mt-3 text-sm text-muted-foreground">Nenhum campo adicionado ainda.</p>
          ) : (fields ?? []).map((f, i) => (
            <div key={f.id} className="mt-3">
              <label className="text-sm font-medium">{f.label}{f.required ? " *" : ""}</label>
              <div className={`mt-1 h-11 rounded-lg border ${state === "inválido" && i === 1 ? "border-destructive" : "border-border"} bg-muted/40`} />
            </div>
          ))}
          {state === "enviado" && <div className="mt-3 rounded-xl bg-[#ede9fe] p-3 text-sm text-primary">Formulário enviado. Lead criado.</div>}
          <Button primary onClick={handleTestSubmit}>Enviar</Button>
        </div>
      </Card>
    </>
  );
}
