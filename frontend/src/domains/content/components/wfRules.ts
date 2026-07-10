import type { UserRole } from "../../../shared/types";
import { wfAllowed, type WFStatus } from "../mocks/content.mocks";

export function wfCanTransition(from: WFStatus, to: WFStatus, role: UserRole): { ok: boolean; reason?: string } {
  if (!wfAllowed[from].includes(to)) return { ok: false, reason: `${from} não pode ir para ${to} diretamente.` };
  if (to === "Published" && !["super_admin", "product_manager"].includes(role)) return { ok: false, reason: "Apenas Product Manager ou Super Admin pode publicar." };
  return { ok: true };
}
