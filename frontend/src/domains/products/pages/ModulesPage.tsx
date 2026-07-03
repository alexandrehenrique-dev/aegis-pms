import { ModuleCatalog } from "../components/ModuleCatalog";
import { useAuth } from "../../../core/auth/useAuth";

export function ModulesPage() {
  const { effectiveProduct } = useAuth();
  return <ModuleCatalog productId={effectiveProduct?.id} />;
}
