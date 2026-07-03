import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router";
import { motion } from "motion/react";
import { Eye, EyeOff, Loader2 } from "lucide-react";
import { useAuth } from "../useAuth";
import { mockUsers, mockTenantsByUser, mockProductsByUser } from "../mocks/users";
import { AuthCard, AuthEnvBadge, AuthLogo } from "../components/AuthChrome";
import { getApiMode } from "../../config/keycloakConfig";
import { toast } from "../../notifications/toast";
import type { LoginError } from "../../../shared/types";

export function LoginScreen() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPwd, setShowPwd] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<LoginError>("");

  useEffect(() => {
    const toastMessage = (location.state as { toast?: string } | null)?.toast;
    if (toastMessage) {
      toast.success(toastMessage);
      window.history.replaceState({}, "", location.pathname);
    }
  }, [location.state, location.pathname]);

  const handleLogin = () => {
    setError("");
    setLoading(true);
    setTimeout(() => {
      setLoading(false);
      const e = mockUsers[email.toLowerCase().trim()];
      if (!e) { setError("invalid"); return; }
      if (e.error) { setError(e.error); return; }
      if (e.password !== password) { setError("invalid"); return; }
      login(e.user, mockTenantsByUser[e.user.id] || [], mockProductsByUser[e.user.id] || {});
      navigate("/select-tenant");
    }, 700);
  };

  const msgs: Record<LoginError, string> = {
    "": "",
    invalid: "E-mail ou senha incorretos. Verifique suas credenciais e tente novamente.",
    blocked: "Esta conta está bloqueada. Entre em contato com o administrador.",
    expired: "Sua sessão expirou. Faça login novamente para continuar.",
    server: "Erro no servidor. Tente novamente em instantes.",
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-4 py-12">
      <AuthEnvBadge />
      <AuthCard>
        <AuthLogo />
        <h1 className="mb-1 text-xl font-semibold tracking-[-.02em]">Entrar na plataforma</h1>
        <p className="mb-6 text-sm text-muted-foreground">Acesse sua conta para continuar.</p>
        <div className="space-y-4">
          <label className="block">
            <span className="mb-1 block text-sm font-medium">E-mail</span>
            <input type="email" autoFocus value={email} onChange={(e) => setEmail(e.target.value)} placeholder="seu@email.com" className="w-full rounded-lg border border-border bg-card px-3 py-2.5 text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/10" onKeyDown={(e) => e.key === "Enter" && handleLogin()} />
          </label>
          <label className="block">
            <div className="mb-1 flex items-center justify-between">
              <span className="text-sm font-medium">Senha</span>
              <button type="button" onClick={() => navigate("/forgot-password")} className="text-xs text-muted-foreground transition hover:text-foreground">Esqueci minha senha</button>
            </div>
            <div className="relative">
              <input type={showPwd ? "text" : "password"} value={password} onChange={(e) => setPassword(e.target.value)} placeholder="••••••••" className="w-full rounded-lg border border-border bg-card px-3 py-2.5 pr-10 text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/10" onKeyDown={(e) => e.key === "Enter" && handleLogin()} />
              <button type="button" onClick={() => setShowPwd(!showPwd)} className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground transition hover:text-foreground">{showPwd ? <EyeOff size={16} /> : <Eye size={16} />}</button>
            </div>
          </label>
          {error && <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} className="rounded-lg border border-destructive/20 bg-[#FDEBE8] p-3 text-sm text-destructive">{msgs[error]}</motion.div>}
          <button onClick={handleLogin} disabled={loading || !email || !password} className="flex w-full items-center justify-center gap-2 rounded-lg bg-primary py-2.5 text-sm text-primary-foreground transition hover:bg-primary/90 disabled:opacity-50">
            {loading ? <><Loader2 size={16} className="animate-spin" />Entrando...</> : "Entrar"}
          </button>
        </div>
        <div className="mt-6 border-t border-border pt-4 text-center text-sm text-muted-foreground">
          Primeiro acesso? <button onClick={() => navigate("/invite")} className="text-primary transition hover:underline">Ativar convite</button>
        </div>
        {getApiMode() === "mock" && (
          <div className="mt-4 rounded-lg bg-muted/60 p-3 text-xs text-muted-foreground">
            <p className="mb-1 font-medium">Contas de demonstração</p>
            <p>super-admin@byop.io · Super Admin</p>
            <p>admin@byop.io · Tenant Admin</p>
            <p>pm@byop.io · Product Manager</p>
            <p>editor@byop.io · Editor</p>
            <p>viewer@byop.io · Viewer</p>
            <p className="mt-1">Senha: <span className="font-mono">senha123</span></p>
          </div>
        )}
      </AuthCard>
    </div>
  );
}
