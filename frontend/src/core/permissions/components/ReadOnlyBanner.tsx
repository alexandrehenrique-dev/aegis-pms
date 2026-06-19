import { Eye } from "lucide-react";
import type { UserRole } from "../../../shared/types";
import { roleLabels } from "../roles";

export function ReadOnlyBanner({ role }: { role: UserRole }) {
  return (
    <div className="mb-4 flex items-center gap-3 rounded-xl border border-[#fef3c7] bg-[#fef3c7]/60 px-4 py-3 text-sm text-[#b45309]">
      <Eye size={15} className="shrink-0" />
      <span>Modo somente leitura — O perfil <b>{roleLabels[role]}</b> não pode criar, editar ou excluir conteúdo.</span>
    </div>
  );
}
