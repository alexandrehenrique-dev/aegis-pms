import { useState } from "react";
import { useNavigate, useSearchParams } from "react-router";
import { Loader2 } from "lucide-react";
import { authActionErrorCode, authActivationService } from "../services/authActivationService";
import { AuthCard, AuthEnvBadge, AuthLogo, PasswordStrengthBar } from "../components/AuthChrome";
import { getPasswordStrength } from "../passwordStrength";

export function ResetPasswordScreen() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token");

  const [tokenState, setTokenState] = useState<"valid" | "invalid" | "expired">(token ? "valid" : "invalid");
  const [pwd, setPwd] = useState("");
  const [confirm, setConfirm] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleReset = () => {
    if (!token) return;
    setError("");
    if (pwd !== confirm) { setError("As senhas não coincidem. Verifique e tente novamente."); return; }
    if (getPasswordStrength(pwd) === "fraca") { setError("Senha muito fraca. Use ao menos 8 caracteres com números."); return; }
    setLoading(true);
    authActivationService
      .confirmPasswordReset(token, pwd)
      .then(() => navigate("/login", { state: { toast: "Senha redefinida! Faça login para continuar." } }))
      .catch((err) => {
        const code = authActionErrorCode(err);
        if (code === "TOKEN_EXPIRED") setTokenState("expired");
        else if (code === "WEAK_PASSWORD") setError("Senha fraca. Use ao menos 8 caracteres, letras e números.");
        else setTokenState("invalid");
      })
      .finally(() => setLoading(false));
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
      </AuthCard>
    </div>
  );
}
