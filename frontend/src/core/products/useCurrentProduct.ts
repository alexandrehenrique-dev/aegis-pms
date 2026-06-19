import { useAuth } from "../auth/AuthContext";

/** Current product selection state, derived from the auth/session store. */
export function useCurrentProduct() {
  const { effectiveProduct, tenantProducts, switchProduct } = useAuth();
  return { product: effectiveProduct, products: tenantProducts, switchProduct };
}
