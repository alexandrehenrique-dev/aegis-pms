import { Outlet, useLocation } from "react-router";
import { useViewAsRole } from "../../core/permissions/useViewAsRole";
import { isRouteBlockedForEffectiveAccess } from "../../core/permissions/roles";
import { NoPermScreen } from "../../core/permissions/components/NoPermScreen";
import { useAuth } from "../../core/auth/useAuth";

/**
 * Route-level permission gate. Renders the matched child route via <Outlet/>
 * unless the currently simulated role (`viewAsRole`) is blocked from the
 * current pathname, in which case it shows the same "Sem permissão de
 * acesso" screen the monolith rendered for `roleBlockedScreens`.
 *
 * Leva em conta também o papel do caller no PRODUTO efetivo (`ProductAssignment`
 * própria, independente do papel de plataforma) — ver `isRouteBlockedForEffectiveAccess`.
 * Sem isso, um Super Admin que também é Editor de um produto específico seria
 * bloqueado ao navegar direto para `/content` etc. mesmo tendo acesso real
 * (o backend já libera via `ProductAccessResolver`).
 */
export function RequireRole() {
  const { viewAsRole } = useViewAsRole();
  const { effectiveProduct } = useAuth();
  const location = useLocation();

  if (isRouteBlockedForEffectiveAccess(viewAsRole, effectiveProduct?.callerAssignedRole, location.pathname)) {
    return <NoPermScreen role={viewAsRole} />;
  }

  return <Outlet />;
}
