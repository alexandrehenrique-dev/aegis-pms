import { useState } from "react";
import { useNavigate } from "react-router";
import { motion } from "motion/react";
import { ArrowLeft, CheckCircle2, Loader2 } from "lucide-react";
import { useAuth } from "../AuthContext";
import { AuthEnvBadge, AuthLogo, PasswordStrengthBar, getPasswordStrength } from "../components/AuthChrome";
import { roleLabels } from "../../permissions/roles";
import type { AuthUser, InviteStatus, ProductOption, TenantOption, UserRole } from "../../../shared/types";
import { fade } from "../../../shared/components/Primitives";

export function InviteScreen() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [status, setStatus] = useState<InviteStatus>("valid");
  const [pwd, setPwd] = useState("");
  const [confirm, setConfirm] = useState("");
  const [terms, setTerms] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [done, setDone] = useState(false);

  const invite = { name: "João Alves", email: "joao@byop.com", tenant: "BYOP", role: "editor" as UserRole, products: ["Maestro Beton"], inviter: "Ana Martins", validUntil: "25 jun 2026" };

  const handleActivate = () => {
    setError("");
    if (pwd !== confirm) { setError("As senhas não coincidem."); return; }
    if (getPasswordStrength(pwd) === "fraca") { setError("Senha muito fraca. Use ao menos 8 caracteres com números."); return; }
    if (!terms) { setError("Você precisa aceitar os termos para continuar."); return; }
    setLoading(true);
    setTimeout(() => {
      setLoading(false);
      setDone(true);
      const u: AuthUser = { id: "u_inv", name: invite.name, email: invite.email, role: invite.role, initials: "JA" };
      const t: TenantOption[] = [{ id: "t1", name: "BYOP", plan: "Pro", productCount: 6, lastAccess: "agora", status: "ativo" }];
      const p: Record<string, ProductOption[]> = { t1: [{ id: "p1", name: "Maestro Beton", type: "Site Institucional", status: "Ativo", modules: 6 }] };
      setTimeout(() => { login(u, t, p); navigate("/select-tenant"); }, 1500);
    }, 800);
  };

  const statusMsgs: Partial<Record<InviteStatus, string>> = {
    expired: `Este convite expirou em ${invite.validUntil}. Solicite um novo convite ao administrador.`,
    revoked: "Este convite foi revogado pelo administrador.",
    used: "Este convite já foi utilizado. Faça login com sua conta.",
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-4 py-12">
      <AuthEnvBadge />
      <div className="w-full max-w-[480px]">
        <motion.div {...fade} className="rounded-2xl border border-border bg-card p-8 shadow-[0_24px_80px_rgba(28,28,28,.08)]">
          <AuthLogo />
          {done ? (
            <div className="text-center">
              <div className="mx-auto mb-4 grid h-14 w-14 place-items-center rounded-full bg-[#ede9fe]"><CheckCircle2 size={24} className="text-primary" /></div>
              <h2 className="font-semibold">Conta ativada com sucesso!</h2>
              <p className="mt-1 text-sm text-muted-foreground">Redirecionando para a plataforma...</p>
            </div>
          ) : status !== "valid" ? (
            <>
              <div className="rounded-lg border border-destructive/20 bg-[#FDEBE8] p-4 text-sm text-destructive">{statusMsgs[status]}</div>
              <button onClick={() => navigate("/login")} className="mt-4 flex w-full items-center justify-center rounded-lg bg-primary py-2.5 text-sm text-primary-foreground transition hover:bg-primary/90">Ir ao login</button>
            </>
          ) : (
            <>
              <div className="mb-6 rounded-xl bg-[#ede9fe] p-4">
                <p className="text-sm font-semibold text-[#7c3aed]">Convite para {invite.tenant}</p>
                <p className="mt-1 text-sm text-muted-foreground">Olá, <b>{invite.name}</b>. {invite.inviter} convidou você para operar na plataforma Aegis PMS.</p>
                <div className="mt-3 grid gap-1.5 text-xs text-muted-foreground">
                  <div className="flex justify-between"><span>Papel</span><b>{roleLabels[invite.role]}</b></div>
                  <div className="flex justify-between"><span>Produtos</span><b>{invite.products.join(", ")}</b></div>
                  <div className="flex justify-between"><span>Válido até</span><b>{invite.validUntil}</b></div>
                </div>
              </div>
              <h2 className="mb-1 font-semibold">Definir senha e ativar conta</h2>
              <p className="mb-4 text-sm text-muted-foreground">Escolha uma senha para acessar a plataforma.</p>
              <div className="space-y-3">
                <label className="block">
                  <span className="mb-1 block text-sm font-medium">Nova senha</span>
                  <input type="password" autoFocus value={pwd} onChange={(e) => setPwd(e.target.value)} placeholder="••••••••" className="w-full rounded-lg border border-border bg-card px-3 py-2.5 text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/10" />
                  {pwd && <PasswordStrengthBar password={pwd} />}
                </label>
                <label className="block">
                  <span className="mb-1 block text-sm font-medium">Confirmar senha</span>
                  <input type="password" value={confirm} onChange={(e) => setConfirm(e.target.value)} placeholder="••••••••" className="w-full rounded-lg border border-border bg-card px-3 py-2.5 text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/10" />
                </label>
                <label className="flex cursor-pointer items-start gap-3">
                  <input type="checkbox" checked={terms} onChange={(e) => setTerms(e.target.checked)} className="mt-0.5 h-4 w-4 rounded accent-primary" />
                  <span className="text-sm text-muted-foreground">Aceito os <span className="text-primary">Termos de Uso</span> e a <span className="text-primary">Política de Privacidade</span> da plataforma Aegis PMS.</span>
                </label>
                {error && <div className="rounded-lg border border-destructive/20 bg-[#FDEBE8] p-3 text-sm text-destructive">{error}</div>}
                <button onClick={handleActivate} disabled={loading || !pwd || !confirm} className="flex w-full items-center justify-center gap-2 rounded-lg bg-primary py-2.5 text-sm text-primary-foreground transition hover:bg-primary/90 disabled:opacity-50">
                  {loading ? <><Loader2 size={16} className="animate-spin" />Ativando conta...</> : "Ativar conta"}
                </button>
              </div>
              <button onClick={() => navigate("/login")} className="mt-4 flex w-full items-center justify-center gap-1.5 text-sm text-muted-foreground transition hover:text-foreground"><ArrowLeft size={14} />Voltar ao login</button>
            </>
          )}
          <div className="mt-4 flex gap-1 text-xs">
            {(["valid", "expired", "revoked", "used"] as InviteStatus[]).map((s) => (
              <button key={s} onClick={() => { setDone(false); setStatus(s); }} className="flex-1 rounded-lg border border-border bg-muted p-1 text-muted-foreground">{s}</button>
            ))}
          </div>
        </motion.div>
      </div>
    </div>
  );
}
