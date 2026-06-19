import { useLocation, useNavigate } from "react-router";
import { MailCheck } from "lucide-react";
import { AuthCard, AuthEnvBadge } from "../components/AuthChrome";

export function ForgotPasswordSentScreen() {
  const navigate = useNavigate();
  const location = useLocation();
  const email = (location.state as { email?: string } | null)?.email ?? "seu e-mail";

  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-4 py-12">
      <AuthEnvBadge />
      <AuthCard>
        <div className="text-center">
          <div className="mx-auto mb-4 grid h-14 w-14 place-items-center rounded-full bg-[#ede9fe]"><MailCheck size={24} className="text-primary" /></div>
          <h1 className="text-xl font-semibold tracking-[-.02em]">Verifique seu e-mail</h1>
          <p className="mt-2 text-sm text-muted-foreground">Enviamos um link de recuperação para <b>{email}</b>. Se não encontrar, confira sua caixa de spam.</p>
        </div>
        <button onClick={() => navigate("/login")} className="mt-6 flex w-full items-center justify-center gap-2 rounded-lg bg-primary py-2.5 text-sm text-primary-foreground transition hover:bg-primary/90">Voltar ao login</button>
        <button onClick={() => navigate("/forgot-password")} className="mt-3 w-full text-center text-sm text-muted-foreground transition hover:text-foreground">Reenviar e-mail</button>
        <button onClick={() => navigate("/reset-password")} className="mt-2 w-full text-center text-xs text-muted-foreground/60 transition hover:text-muted-foreground">Demo: Abrir tela de redefinição →</button>
      </AuthCard>
    </div>
  );
}
