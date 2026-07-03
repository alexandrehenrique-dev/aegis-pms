import { useAuth } from "../auth/useAuth";

/** Current tenant selection state, derived from the auth/session store. */
export function useCurrentTenant() {
  const { effectiveTenant, userTenants, switchTenant } = useAuth();
  return { tenant: effectiveTenant, tenants: userTenants, switchTenant };
}
