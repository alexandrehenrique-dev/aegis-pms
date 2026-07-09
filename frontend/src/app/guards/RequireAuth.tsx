import { Navigate, Outlet, useLocation } from "react-router";
import { useAuth } from "../../core/auth/useAuth";
import { requiresProductContext } from "../../core/permissions/roles";

const PRODUCT_WORKSPACE_ROLES = new Set(["product_manager", "editor", "viewer"]);

/**
 * Guards the authenticated section of the app. Redirects to /login if there's
 * no logged-in user, to /select-tenant if a tenant hasn't been chosen yet, and
 * to /select-product if a product hasn't been chosen yet — mirroring the
 * original App()'s `if (!authUser) ... if (!effectiveTenant) ... if (!effectiveProduct)`
 * cascade, but expressed as router redirects instead of conditional renders.
 */
export function RequireAuth() {
  const { authUser, effectiveTenant, effectiveProduct } = useAuth();
  const location = useLocation();

  if (!authUser) return <Navigate to="/login" replace state={{ from: location }} />;
  if (!effectiveTenant) {
    return <Navigate to={PRODUCT_WORKSPACE_ROLES.has(authUser.role) ? "/select-product" : "/select-tenant"} replace />;
  }
  if (!effectiveProduct && requiresProductContext(location.pathname)) return <Navigate to="/select-product" replace />;

  return <Outlet />;
}
