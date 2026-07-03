import { createContext } from "react";
import type { UserRole } from "../../shared/types";

export type ViewAsRoleContextValue = { viewAsRole: UserRole; setViewAsRole: (r: UserRole) => void; restore: () => void };

export const ViewAsRoleContext = createContext<ViewAsRoleContextValue | undefined>(undefined);
