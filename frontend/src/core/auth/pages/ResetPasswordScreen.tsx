import { useState } from "react";
import { useNavigate } from "react-router";
import { motion } from "motion/react";
import { CheckCircle2, Loader2 } from "lucide-react";
import { AuthCard, AuthEnvBadge, AuthLogo, PasswordStrengthBar, getPasswordStrength } from "../components/AuthChrome";

export function ResetPasswordScreen() {
  const navigate = useNavigate();
  const [tokenState, setTokenState] = useState<"valid" | "invalid" | "expired">("valid");
  const [pwd, setPwd] = useState("");
  const [confirm, setConfirm] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);

  const handleReset = () => {
    setError("");
    if (pwd !== confirm) { setError("As senhas não coincidem. Verifique e tente novamente."); return; }
    if (getPasswordStrength(pwd) === "fraca") { setError("Senha muito fraca. Use ao menos 8 caracteres com números."); return; }
    setLoading(true);
    setTimeout(() => {
      setLoading(false);
      setSuccess(true);
      setTimeout(() => navigate("/login"), 1500);
    }, 800);
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-4 py-12">
      <AuthEnvBadge />
      <AuthCard>
        <AuthLogo />
        {tokenState !== "valid" ? (
          <>
            <div className="rounded-lg border border-destructive/20 bg-[#FDEBE8] p-4 text-sm text-destructive">{tokenState === "expired" ? "Link expirado. Solicite um novo link de recuperação." : "Link inválido. Verifique o e-mail recebido."}</div>
            <button onClick={() => navigate("/forgot-password")} className="mt-4 flex w-full items-center justify-center rounded-lg bg-primary py-2.5 text-sm text-primary-foreground transition hover:bg-primary/90">Solicitar novo link</button>
          </>
        ) : success ? (
          <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} className="text-center">
            <div className="mx-auto mb-4 grid h-14 w-14 place-items-center rounded-full bg-[#ede9fe]"><CheckCircle2 size={24} className="text-primary" /></div>
            <h2 className="font-semibold">Senha redefinida com sucesso!</h2>
            <p className="mt-1 text-sm text-muted-foreground">Redirecionando para o login...</p>
          </motion.div>
        ) : (
          <>
            <h1 className="mb-1 text-xl font-semibold tracking-[-.02em]">Definir nova senha</h1>
            <p className="mb-6 text-sm text-muted-foreground">Escolha uma senha segura para sua conta.</p>
            <div className="space-y-4">
              <label className="block">
                <span className="mb-1 block text-sm font-medium">Nova senha</span>
                <input type="password" autoFocus value={pwd} onChange={(e) => setPwd(e.target.value)} placeholder="••••••••" className="w-full rounded-lg border border-border bg-card px-3 py-2.5 text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/10" />
                {pwd && <PasswordStrengthBar password={pwd} />}
              </label>
              <label className="block">
                <span className="mb-1 block text-sm font-medium">Confirmar senha</span>
                <input type="password" value={confirm} onChange={(e) => setConfirm(e.target.value)} placeholder="••••••••" className="w-full rounded-lg border border-border bg-card px-3 py-2.5 text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/10" />
              </label>
              {error && <div className="rounded-lg border border-destructive/20 bg-[#FDEBE8] p-3 text-sm text-destructive">{error}</div>}
              <button onClick={handleReset} disabled={loading || !pwd || !confirm} className="flex w-full items-center justify-center gap-2 rounded-lg bg-primary py-2.5 text-sm text-primary-foreground transition hover:bg-primary/90 disabled:opacity-50">
                {loading ? <><Loader2 size={16} className="animate-spin" />Salvando...</> : "Salvar nova senha"}
              </button>
            </div>
          </>
        )}
        <div className="mt-4 flex gap-1.5 text-xs">
          <button onClick={() => setTokenState("valid")} className="flex-1 rounded-lg border border-border bg-muted p-1 text-muted-foreground">válido</button>
          <button onClick={() => setTokenState("expired")} className="flex-1 rounded-lg border border-border bg-muted p-1 text-muted-foreground">expirado</button>
          <button onClick={() => setTokenState("invalid")} className="flex-1 rounded-lg border border-border bg-muted p-1 text-muted-foreground">inválido</button>
        </div>
      </AuthCard>
    </div>
  );
}
