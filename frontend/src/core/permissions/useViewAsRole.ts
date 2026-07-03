import { useContext } from "react";
import { ViewAsRoleContext, type ViewAsRoleContextValue } from "./viewAsRoleContextDefinition";

export function useViewAsRole(): ViewAsRoleContextValue {
  const ctx = useContext(ViewAsRoleContext);
  if (!ctx) throw new Error("useViewAsRole must be used within a ViewAsRoleProvider");
  return ctx;
}
