import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router";
import { motion } from "motion/react";
import { ArrowLeft, CheckCircle2, Loader2 } from "lucide-react";
import { authActionErrorCode, authActivationService, type InviteTokenData } from "../services/authActivationService";
import { AuthEnvBadge, AuthLogo, PasswordStrengthBar } from "../components/AuthChrome";
import { getPasswordStrength } from "../passwordStrength";
import type { InviteStatus } from "../../../shared/types";
import { fade } from "../../../shared/components/motion";

export function InviteScreen() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token");

  const [status, setStatus] = useState<InviteStatus>("loading");
  const [invite, setInvite] = useState<InviteTokenData | null>(null);
  const [pwd, setPwd] = useState("");
  const [confirm, setConfirm] = useState("");
  const [terms, setTerms] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!token) { setStatus("revoked"); return; }
    authActivationService
      .validateInviteToken(token)
      .then((data) => { setInvite(data); setStatus("valid"); })
      .catch((err) => {
        const code = authActionErrorCode(err);
        if (code === "TOKEN_EXPIRED") setStatus("expired");
        else if (code === "TOKEN_ALREADY_USED") setStatus("used");
        else setStatus("revoked");
      });
  }, [token]);

  const handleActivate = () => {
    if (!token) return;
    setError("");
    if (pwd !== confirm) { setError("As senhas não coincidem."); return; }
    if (getPasswordStrength(pwd) === "fraca") { setError("Senha muito fraca. Use ao menos 8 caracteres com números."); return; }
    if (!terms) { setError("Você precisa aceitar os termos para continuar."); return; }
    setLoading(true);
    authActivationService
      .activateAccount(token, pwd)
      .then(() => {
        // Passa o slug do produto como `next` para que o LoginScreen redirecione
        // o usuário diretamente ao produto após o login, sem passar por /select-product.
        const next = invite?.productSlug ? `/products/${invite.productSlug}` : undefined;
        navigate("/login", { state: { toast: "Conta ativada! Faça login para continuar.", next } });
      })
      .catch((err) => {
        const code = authActionErrorCode(err);
        if (code === "WEAK_PASSWORD") setError("A senha deve ter ao menos 8 caracteres, incluindo letras e números.");
        else if (code === "TOKEN_EXPIRED") setStatus("expired");
        else setError("Ocorreu um erro. Tente novamente.");
      })
      .finally(() => setLoading(false));
  };

  const statusMsgs: Partial<Record<InviteStatus, string>> = {
    expired: `Este convite expirou${invite?.expiresAt ? ` em ${new Date(invite.expiresAt).toLocaleDateString("pt-BR")}` : ""}. Solicite um novo convite ao administrador.`,
    revoked: "Este convite é inválido ou foi revogado pelo administrador.",
    used: "Este convite já foi utilizado. Faça login com sua conta.",
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-4 py-12">
      <AuthEnvBadge />
      <div className="w-full max-w-[480px]">
        <motion.div {...fade} className="rounded-2xl border border-border bg-card p-8 shadow-[0_24px_80px_rgba(28,28,28,.08)]">
          <AuthLogo />
          {status === "loading" ? (
            <div className="flex flex-col items-center gap-3 py-6 text-sm text-muted-foreground">
              <Loader2 size={20} className="animate-spin" />
              Validando convite...
            </div>
          ) : status !== "valid" ? (
            <>
              <div className="rounded-lg border border-destructive/20 bg-[#FDEBE8] p-4 text-sm text-destructive">{statusMsgs[status]}</div>
              <button onClick={() => navigate("/login")} className="mt-4 flex w-full items-center justify-center rounded-lg bg-primary py-2.5 text-sm text-primary-foreground transition hover:bg-primary/90">Ir ao login</button>
            </>
          ) : invite ? (
            <>
              {/* Card de contexto do convite — igual para ambos os fluxos */}
              <div className="mb-6 rounded-xl bg-[#ede9fe] p-4">
                <p className="text-sm font-semibold text-[#7c3aed]">Convite para {invite.tenantName}</p>
                <p className="mt-1 text-sm text-muted-foreground">Olá, <b>{invite.userName}</b>. {invite.inviterName} convidou você para operar na plataforma Aegis PMS.</p>
                <div className="mt-3 grid gap-1.5 text-xs text-muted-foreground">
                  <div className="flex justify-between"><span>Papel</span><b>{invite.role}</b></div>
                  <div className="flex justify-between"><span>Produtos</span><b>{invite.productNames.join(", ")}</b></div>
                  <div className="flex justify-between"><span>E-mail</span><b>{invite.userEmail}</b></div>
                </div>
              </div>

              {!invite.requiresPasswordSetup ? (
                /* ── Usuário existente: já tem conta, não precisa de senha ── */
                <>
                  <div className="mb-4 flex items-start gap-3 rounded-xl border border-emerald-200 bg-emerald-50 p-4">
                    <CheckCircle2 size={18} className="mt-0.5 shrink-0 text-emerald-600" />
                    <div>
                      <p className="text-sm font-semibold text-emerald-800">Você já tem uma conta!</p>
                      <p className="mt-0.5 text-sm text-emerald-700">
                        Seu acesso ao produto <b>{invite.productNames[0]}</b> foi configurado. Faça login normalmente para começar.
                      </p>
                    </div>
                  </div>
                  <button
                    onClick={() => navigate("/login", {
                      state: { next: invite.productSlug ? `/products/${invite.productSlug}` : undefined },
                    })}
                    className="flex w-full items-center justify-center gap-2 rounded-lg bg-primary py-2.5 text-sm text-primary-foreground transition hover:bg-primary/90"
                  >
                    Fazer login e acessar produto
                  </button>
                </>
              ) : (
                /* ── Usuário novo: precisa criar senha ── */
                <>
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
                </>
              )}

              <button onClick={() => navigate("/login")} className="mt-4 flex w-full items-center justify-center gap-1.5 text-sm text-muted-foreground transition hover:text-foreground"><ArrowLeft size={14} />Voltar ao login</button>
            </>
          ) : null}
        </motion.div>
      </div>
    </div>
  );
}
