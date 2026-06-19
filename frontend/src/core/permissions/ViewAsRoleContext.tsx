import { createContext, useContext, useState, type ReactNode } from "react";
import type { UserRole } from "../../shared/types";
import { useAuth } from "../auth/AuthContext";

type ViewAsRoleContextValue = { viewAsRole: UserRole; setViewAsRole: (r: UserRole) => void; restore: () => void };

const ViewAsRoleContext = createContext<ViewAsRoleContextValue | undefined>(undefined);

export function useViewAsRole(): ViewAsRoleContextValue {
  const ctx = useContext(ViewAsRoleContext);
  if (!ctx) throw new Error("useViewAsRole must be used within a ViewAsRoleProvider");
  return ctx;
}

/** Lets Tenant/Super admins simulate viewing the app as another role (permission preview). */
export function ViewAsRoleProvider({ children }: { children: ReactNode }) {
  const { authUser } = useAuth();
  const [viewAsRole, setViewAsRole] = useState<UserRole>(authUser?.role ?? "viewer");
  const restore = () => { if (authUser) setViewAsRole(authUser.role); };
  return <ViewAsRoleContext.Provider value={{ viewAsRole, setViewAsRole, restore }}>{children}</ViewAsRoleContext.Provider>;
}
