import { useNavigate } from "react-router";
import { Plus } from "lucide-react";
import { Button } from "../../../shared/components/Primitives";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { formsService } from "../../forms/services/formsService";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";

/**
 * Seletor de `formId` para os blocos `contact`/`form` (Sprint 12, Tarefa K.4)
 * — substitui o texto livre antigo, populado pelos formulários já criados
 * no módulo Forms do produto. "Criar novo" é um atalho para o `FormBuilder`
 * quando nenhum formulário existente serve.
 */
export function FormIdSelector({ productSlug, value, onChange }: { productSlug: string; value: string; onChange: (formId: string) => void }) {
  const navigate = useNavigate();
  const { product } = useCurrentProduct();
  const productId = product?.id ?? productSlug;
  const { data: forms } = useAsyncData(() => formsService.listForms(productId), [productId]);

  return (
    <div className="md:col-span-2">
      <label className="block">
        <span className="mb-1 block text-sm font-medium">Formulário (formId)</span>
        <select value={value} onChange={(e) => onChange(e.target.value)} className="w-full rounded-lg border border-border bg-card p-3 text-sm outline-primary">
          <option value="">Selecionar formulário...</option>
          {(forms ?? []).map((f) => <option key={f.id} value={f.id}>{f.name}</option>)}
        </select>
      </label>
      <Button onClick={() => navigate("/forms/new")} className="mt-2"><Plus size={14} />Criar novo formulário</Button>
    </div>
  );
}
