import { PageHeader } from "../../../shared/components/Primitives";
import { ProductTeamPanel } from "../../products/components/ProductTeamPanel";
import { useAuth } from "../../../core/auth/useAuth";

export function TeamSettings() {
  const { effectiveProduct, effectiveTenant } = useAuth();

  if (!effectiveProduct?.id || !effectiveTenant?.id) {
    return (
      <div className="rounded-xl border border-border bg-card p-8 text-center text-muted-foreground">
        Nenhum produto selecionado. Selecione um produto para gerenciar a equipe.
      </div>
    );
  }

  return (
    <>
      <PageHeader
        title="Equipe do produto"
        module="Configurações"
        desc={`Membros e papéis de "${effectiveProduct.name}". Product Managers recebem notificações de exportação e exclusão.`}
        badge="Equipe"
      />
      <ProductTeamPanel productId={effectiveProduct.id} tenantId={effectiveTenant.id} />
    </>
  );
}
