import { useAuth } from "../auth/AuthContext";

/** Current tenant selection state, derived from the auth/session store. */
export function useCurrentTenant() {
  const { effectiveTenant, userTenants, switchTenant } = useAuth();
  return { tenant: effectiveTenant, tenants: userTenants, switchTenant };
}
