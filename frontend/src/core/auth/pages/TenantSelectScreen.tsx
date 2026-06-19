import { useState } from "react";
import { useNavigate } from "react-router";
import { motion } from "motion/react";
import { Boxes, Clock3, LogOut } from "lucide-react";
import { useAuth } from "../AuthContext";
import { roleLabels } from "../../permissions/roles";
import { AegisLogo } from "../../../shared/components/AegisLogo";
import { Badge, EmptyState, fade } from "../../../shared/components/Primitives";

export function TenantSelectScreen() {
  const navigate = useNavigate();
  const { authUser, userTenants, selectTenant, logout } = useAuth();
  const [q, setQ] = useState("");
  if (!authUser) return null;
  const filtered = userTenants.filter((t) => t.name.toLowerCase().includes(q.toLowerCase()));

  const handleSelect = (t: typeof userTenants[number]) => {
    if (t.status === "suspenso") return;
    selectTenant(t);
    navigate("/select-product");
  };

  const handleLogout = () => { logout(); navigate("/login"); };

  return (
    <div className="min-h-screen bg-background">
      <header className="flex items-center justify-between border-b border-border bg-card px-6 py-4 shadow-[0_1px_0_rgba(0,0,0,0.06)] dark:shadow-none">
        <div className="flex items-center gap-2.5"><AegisLogo size="sm" /><span className="font-semibold tracking-[-.02em]">Aegis</span><Badge tone="violet">Product OS</Badge></div>
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2 rounded-xl border border-border bg-muted/40 px-3 py-1.5 text-sm">
            <div className="grid h-6 w-6 place-items-center rounded-full bg-primary text-primary-foreground text-xs font-semibold">{authUser.initials}</div>
            <span className="text-muted-foreground hidden sm:block">{authUser.name}</span>
            <Badge tone="violet">{roleLabels[authUser.role]}</Badge>
          </div>
          <button onClick={handleLogout} className="flex items-center gap-1.5 rounded-xl border border-border bg-card px-3 py-1.5 text-sm transition hover:bg-muted"><LogOut size={14} />Sair</button>
        </div>
      </header>
      <main className="mx-auto max-w-3xl px-4 py-12">
        <motion.div {...fade}>
          <h1 className="text-2xl font-semibold tracking-[-.02em]">Selecione seu espaço de trabalho</h1>
          <p className="mt-1 text-sm text-muted-foreground">Você tem acesso a {userTenants.length} tenant{userTenants.length !== 1 ? "s" : ""}.</p>
          <div className="mt-6 flex items-center gap-2 rounded-xl border border-border bg-card px-3 py-2.5">
            <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Buscar tenant..." className="w-full bg-transparent text-sm outline-none" />
          </div>
          <div className="mt-4 grid gap-3 md:grid-cols-2">
            {filtered.length === 0 ? <EmptyState compact title="Nenhum tenant encontrado" description="Ajuste a busca." /> : filtered.map((t) => (
              <button key={t.id} onClick={() => handleSelect(t)} disabled={t.status === "suspenso"} className="group rounded-2xl border border-border bg-card p-5 text-left transition hover:border-primary hover:shadow-[0_8px_30px_rgba(15,61,46,.08)] disabled:opacity-50">
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-3">
                    <div className="grid h-10 w-10 place-items-center rounded-xl bg-primary/10 font-semibold text-primary">{t.name.charAt(0)}</div>
                    <div><p className="font-semibold">{t.name}</p><p className="text-xs text-muted-foreground">{t.plan}</p></div>
                  </div>
                  <Badge tone={t.status === "ativo" ? "green" : "red"}>{t.status}</Badge>
                </div>
                <div className="mt-4 grid grid-cols-2 gap-2 text-xs text-muted-foreground">
                  <span className="flex items-center gap-1"><Boxes size={12} />{t.productCount} produtos</span>
                  <span className="flex items-center gap-1"><Clock3 size={12} />{t.lastAccess}</span>
                </div>
                {t.status === "suspenso" ? <p className="mt-3 text-xs text-destructive">Tenant suspenso. Contate o suporte.</p> : <div className="mt-4 flex justify-end"><span className="text-sm text-primary opacity-0 transition group-hover:opacity-100">Entrar →</span></div>}
              </button>
            ))}
          </div>
        </motion.div>
      </main>
    </div>
  );
}
