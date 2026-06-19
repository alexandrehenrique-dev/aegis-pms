import { useState } from "react";
import { useNavigate } from "react-router";
import { ArrowLeft, Loader2 } from "lucide-react";
import { AuthCard, AuthEnvBadge, AuthLogo } from "../components/AuthChrome";

export function ForgotPasswordScreen() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [attempts, setAttempts] = useState(0);

  const handleSend = () => {
    if (attempts >= 3) { setError("rate_limit"); return; }
    setError("");
    if (!email.includes("@")) { setError("E-mail inválido. Verifique e tente novamente."); return; }
    setLoading(true);
    setTimeout(() => {
      setLoading(false);
      setAttempts((a) => a + 1);
      navigate("/forgot-password/sent", { state: { email } });
    }, 800);
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-4 py-12">
      <AuthEnvBadge />
      <AuthCard>
        <AuthLogo />
        <button onClick={() => navigate("/login")} className="mb-6 flex items-center gap-1.5 text-sm text-muted-foreground transition hover:text-foreground"><ArrowLeft size={15} />Voltar ao login</button>
        <h1 className="mb-1 text-xl font-semibold tracking-[-.02em]">Recuperar acesso</h1>
        <p className="mb-6 text-sm text-muted-foreground">Informe seu e-mail e enviaremos um link de recuperação de senha.</p>
        <div className="space-y-4">
          <label className="block">
            <span className="mb-1 block text-sm font-medium">E-mail</span>
            <input type="email" autoFocus value={email} onChange={(e) => setEmail(e.target.value)} placeholder="seu@email.com" className="w-full rounded-lg border border-border bg-card px-3 py-2.5 text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/10" onKeyDown={(e) => e.key === "Enter" && handleSend()} />
          </label>
          {error === "rate_limit" ? (
            <div className="rounded-lg border border-[#D97706]/30 bg-[#FBF1DF] p-3 text-sm text-[#8A5A12]">Limite de tentativas atingido. Aguarde alguns minutos antes de tentar novamente.</div>
          ) : error ? (
            <div className="rounded-lg border border-destructive/20 bg-[#FDEBE8] p-3 text-sm text-destructive">{error}</div>
          ) : null}
          <button onClick={handleSend} disabled={loading || !email} className="flex w-full items-center justify-center gap-2 rounded-lg bg-primary py-2.5 text-sm text-primary-foreground transition hover:bg-primary/90 disabled:opacity-50">
            {loading ? <><Loader2 size={16} className="animate-spin" />Enviando...</> : "Enviar instruções"}
          </button>
        </div>
      </AuthCard>
    </div>
  );
}
