import { motion } from "motion/react";
import { Lock } from "lucide-react";
import { fade, Badge } from "../../../shared/components/Primitives";
import type { UserRole } from "../../../shared/types";
import { roleLabels } from "../roles";

export function NoPermScreen({ role }: { role: UserRole }) {
  return (
    <motion.div {...fade} className="flex flex-col items-center justify-center py-24 text-center">
      <div className="mb-5 grid h-16 w-16 place-items-center rounded-2xl bg-[#fee2e2]"><Lock size={26} className="text-[#dc2626]" /></div>
      <h2 className="text-xl font-semibold">Sem permissão de acesso</h2>
      <p className="mt-2 max-w-sm text-sm leading-6 text-muted-foreground">O perfil <b>{roleLabels[role]}</b> não tem acesso a esta área. Solicite permissão ao administrador do tenant.</p>
      <div className="mt-4 flex gap-2"><Badge tone="red">{roleLabels[role]}</Badge><Badge tone="neutral">Acesso restrito</Badge></div>
    </motion.div>
  );
}
