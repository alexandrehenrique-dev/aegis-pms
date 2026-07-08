import { useEffect, useRef, useState, type ReactNode } from "react";
import type { UserRole } from "../../shared/types";
import { useAuth } from "../auth/useAuth";
import { toProductUserRole } from "./roles";
import { ViewAsRoleContext } from "./viewAsRoleContextDefinition";

/** Lets Tenant/Super admins simulate viewing the app as another role (permission preview). */
export function ViewAsRoleProvider({ children }: { children: ReactNode }) {
  const { authUser, effectiveProduct } = useAuth();
  const actualRole = toProductUserRole(effectiveProduct?.callerAssignedRole) ?? authUser?.role ?? "viewer";
  const [viewAsRole, setViewAsRole] = useState<UserRole>(actualRole);
  const prevUserId = useRef<string | undefined>(authUser?.id);
  const prevActualRole = useRef<UserRole>(actualRole);

  // AuthProvider's authUser is still null on the very first render (login
  // happens after mount), então o useState acima captura "viewer" como
  // fallback. Sincroniza sempre que o usuário logado ou o papel efetivo no
  // produto mudar para refletir a autorização real assim que ela existir.
  useEffect(() => {
    if (authUser && (authUser.id !== prevUserId.current || actualRole !== prevActualRole.current)) {
      setViewAsRole(actualRole);
    }
    prevUserId.current = authUser?.id;
    prevActualRole.current = actualRole;
  }, [actualRole, authUser]);

  const restore = () => { if (authUser) setViewAsRole(actualRole); };
  return <ViewAsRoleContext.Provider value={{ actualRole, viewAsRole, setViewAsRole, restore }}>{children}</ViewAsRoleContext.Provider>;
}
