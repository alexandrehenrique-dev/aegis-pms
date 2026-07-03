import { Navigate, Outlet, useLocation } from "react-router";
import { useAuth } from "../../core/auth/useAuth";

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
  if (!effectiveTenant) return <Navigate to="/select-tenant" replace />;
  if (!effectiveProduct) return <Navigate to="/select-product" replace />;

  return <Outlet />;
}
