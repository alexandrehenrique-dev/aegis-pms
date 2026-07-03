import { useEffect, useRef, useState, type ReactNode } from "react";
import type { UserRole } from "../../shared/types";
import { useAuth } from "../auth/useAuth";
import { ViewAsRoleContext } from "./viewAsRoleContextDefinition";

/** Lets Tenant/Super admins simulate viewing the app as another role (permission preview). */
export function ViewAsRoleProvider({ children }: { children: ReactNode }) {
  const { authUser } = useAuth();
  const [viewAsRole, setViewAsRole] = useState<UserRole>(authUser?.role ?? "viewer");
  const prevUserId = useRef<string | undefined>(authUser?.id);

  // AuthProvider's authUser is still null on the very first render (login
  // happens after mount), então o useState acima captura "viewer" como
  // fallback. Sincroniza sempre que o usuário logado mudar (login/logout/
  // troca de conta) para refletir o papel real assim que ele existir.
  useEffect(() => {
    if (authUser && authUser.id !== prevUserId.current) {
      setViewAsRole(authUser.role);
    }
    prevUserId.current = authUser?.id;
  }, [authUser]);

  const restore = () => { if (authUser) setViewAsRole(authUser.role); };
  return <ViewAsRoleContext.Provider value={{ viewAsRole, setViewAsRole, restore }}>{children}</ViewAsRoleContext.Provider>;
}
