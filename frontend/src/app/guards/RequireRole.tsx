import { Outlet, useLocation } from "react-router";
import { useViewAsRole } from "../../core/permissions/useViewAsRole";
import { isRouteBlocked } from "../../core/permissions/roles";
import { NoPermScreen } from "../../core/permissions/components/NoPermScreen";

/**
 * Route-level permission gate. Renders the matched child route via <Outlet/>
 * unless the currently simulated role (`viewAsRole`) is blocked from the
 * current pathname, in which case it shows the same "Sem permissão de
 * acesso" screen the monolith rendered for `roleBlockedScreens`.
 */
export function RequireRole() {
  const { viewAsRole } = useViewAsRole();
  const location = useLocation();

  if (isRouteBlocked(viewAsRole, location.pathname)) {
    return <NoPermScreen role={viewAsRole} />;
  }

  return <Outlet />;
}
