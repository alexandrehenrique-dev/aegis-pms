import { ModuleCatalog } from "../components/ModuleCatalog";
import { useAuth } from "../../../core/auth/AuthContext";

export function ModulesPage() {
  const { effectiveProduct } = useAuth();
  return <ModuleCatalog productId={effectiveProduct?.id} />;
}
