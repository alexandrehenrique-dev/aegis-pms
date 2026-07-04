import { type ReactNode } from "react";
import { motion } from "motion/react";
import { fade } from "../../../shared/components/motion";
import { AegisLogo } from "../../../shared/components/AegisLogo";
import { getPasswordStrength } from "../passwordStrength";

export function AuthCard({ children }: { children: ReactNode }) {
  return (
    <motion.div {...fade} className="w-full max-w-[400px] rounded-2xl border border-border bg-card p-8 shadow-[0_2px_8px_rgba(0,0,0,0.06),0_16px_48px_rgba(0,0,0,0.08)] dark:shadow-[0_2px_8px_rgba(0,0,0,0.3),0_16px_48px_rgba(0,0,0,0.4)]">
      {children}
    </motion.div>
  );
}

export function AuthLogo() {
  return (
    <div className="mb-8 flex flex-col items-center gap-3">
      <AegisLogo size="lg" />
      <div className="text-center">
        <p className="font-semibold tracking-[-.02em]">Aegis PMS</p>
        <p className="text-xs text-muted-foreground">Product Operating System · BYOP</p>
      </div>
    </div>
  );
}

export function AuthEnvBadge() {
  if (import.meta.env.PROD) return null;
  const label = import.meta.env.VITE_API_MODE === "api" ? "api" : "mock";
  return (
    <div className="fixed left-4 top-4 z-50 rounded-full border border-[var(--byop-violet-soft)] bg-[var(--byop-violet-soft)] px-3 py-1 text-xs text-[var(--byop-violet-dark)]">
      {label}
    </div>
  );
}

export function PasswordStrengthBar({ password }: { password: string }) {
  const s = getPasswordStrength(password);
  const lvl = { fraca: 1, média: 2, forte: 3 }[s];
  const col = { fraca: "bg-destructive", média: "bg-[#D97706]", forte: "bg-primary" }[s];
  return (
    <div className="mt-1.5">
      <div className="flex gap-1">
        {[1, 2, 3].map((i) => <div key={i} className={`h-1.5 flex-1 rounded-full transition-colors duration-200 ${i <= lvl ? col : "bg-muted"}`} />)}
      </div>
      <p className="mt-1 text-xs text-muted-foreground">Senha {s}</p>
    </div>
  );
}
