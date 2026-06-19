import { useState, type ReactNode } from "react";
import { useNavigate } from "react-router";
import { motion } from "motion/react";
import { ArrowLeft, Building2, ChevronRight, Clock3, LogOut, Star } from "lucide-react";
import { useAuth } from "../AuthContext";
import { getPostLoginLandingPath } from "../../permissions/roles";
import { AegisLogo } from "../../../shared/components/AegisLogo";
import { ProductStatusBadge } from "../../../shared/components/ProductStatusBadge";
import { EmptyState, fade } from "../../../shared/components/Primitives";
import type { ProductOption } from "../../../shared/types";

export function ProductSelectScreen() {
  const navigate = useNavigate();
  const { authUser, effectiveTenant, tenantProducts, selectProduct, selectTenant, logout } = useAuth();
  const [q, setQ] = useState("");
  const [sf, setSf] = useState("todos");

  if (!authUser || !effectiveTenant) return null;

  const filtered = tenantProducts.filter((p) => (p.name + p.type).toLowerCase().includes(q.toLowerCase()) && (sf === "todos" || p.status === sf));
  const favs = filtered.filter((p) => p.isFavorite);
  const recents = filtered.filter((p) => p.isRecent && !p.isFavorite);
  const others = filtered.filter((p) => !p.isFavorite && !p.isRecent);

  const handleSelect = (p: ProductOption) => { selectProduct(p); navigate(getPostLoginLandingPath(authUser.role, p)); };
  const handleBack = () => { selectTenant(null); navigate("/select-tenant"); };
  const handleLogout = () => { logout(); navigate("/login"); };

  const PCard = ({ p }: { p: ProductOption }) => {
    const disabled = p.status === "Arquivado" || p.modules === 0;
    return (
      <button onClick={() => !disabled && handleSelect(p)} disabled={disabled} className="group rounded-2xl border border-border bg-card p-5 text-left transition hover:border-primary hover:shadow-[0_8px_30px_rgba(15,61,46,.08)] disabled:cursor-not-allowed disabled:opacity-40">
        <div className="flex items-start justify-between gap-3">
          <div>
            <div className="flex items-center gap-1.5"><p className="font-semibold">{p.name}</p>{p.isFavorite && <Star size={12} className="fill-[#D97706] text-[#D97706]" />}</div>
            <p className="text-xs text-muted-foreground">{p.type}</p>
          </div>
          <ProductStatusBadge status={p.status} />
        </div>
        <div className="mt-4 flex items-center justify-between text-xs text-muted-foreground">
          <span>{p.modules} módulos{p.modules === 0 ? " · sem módulos" : ""}</span>
          {!disabled && <span className="text-primary opacity-0 transition group-hover:opacity-100">Abrir →</span>}
        </div>
      </button>
    );
  };

  const Sec = ({ title, icon, items }: { title: string; icon?: ReactNode; items: ProductOption[] }) => items.length === 0 ? null : (
    <div>
      <div className="mb-2 flex items-center gap-1.5 text-xs font-medium text-muted-foreground">{icon}{title}</div>
      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">{items.map((p) => <PCard key={p.id} p={p} />)}</div>
    </div>
  );

  return (
    <div className="min-h-screen bg-background">
      <header className="flex items-center justify-between border-b border-border bg-card px-6 py-4 shadow-[0_1px_0_rgba(0,0,0,0.06)] dark:shadow-none">
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2.5"><AegisLogo size="sm" /><span className="font-semibold tracking-[-.02em]">Aegis</span></div>
          <ChevronRight size={14} className="text-muted-foreground" />
          <div className="flex items-center gap-1.5"><Building2 size={14} className="text-muted-foreground" /><span className="text-sm font-medium">{effectiveTenant.name}</span></div>
        </div>
        <div className="flex items-center gap-2">
          <button onClick={handleBack} className="flex items-center gap-1.5 rounded-xl border border-border bg-card px-3 py-1.5 text-sm transition hover:bg-muted"><ArrowLeft size={14} />Trocar tenant</button>
          <button onClick={handleLogout} className="flex items-center gap-1.5 rounded-xl border border-border bg-card px-3 py-1.5 text-sm transition hover:bg-muted"><LogOut size={14} />Sair</button>
        </div>
      </header>
      <main className="mx-auto max-w-5xl px-4 py-10">
        <motion.div {...fade}>
          <div className="mb-6">
            <h1 className="text-2xl font-semibold tracking-[-.02em]">Selecione um produto</h1>
            <p className="mt-1 text-sm text-muted-foreground">{effectiveTenant.name} · {tenantProducts.length} produto{tenantProducts.length !== 1 ? "s" : ""}</p>
          </div>
          <div className="mb-4 flex flex-wrap gap-2">
            <div className="flex min-w-[200px] flex-1 items-center gap-2 rounded-xl border border-border bg-card px-3 py-2.5">
              <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Buscar produto..." className="w-full bg-transparent text-sm outline-none" />
            </div>
            {["todos", "Ativo", "Pendente", "Arquivado"].map((s) => (
              <button key={s} onClick={() => setSf(s)} className={`rounded-xl border px-3 py-2 text-sm transition ${sf === s ? "border-primary bg-primary/5 text-primary" : "border-border bg-card text-muted-foreground hover:bg-muted"}`}>{s}</button>
            ))}
          </div>
          {filtered.length === 0 ? <EmptyState title="Nenhum produto encontrado" description="Ajuste os filtros ou limpe a busca." /> : (
            <div className="space-y-6">
              <Sec title="Favoritos" icon={<Star size={12} className="text-[#D97706]" />} items={favs} />
              <Sec title="Acessados recentemente" icon={<Clock3 size={12} />} items={recents} />
              <Sec title={favs.length > 0 || recents.length > 0 ? "Todos os produtos" : "Produtos"} items={others} />
            </div>
          )}
        </motion.div>
      </main>
    </div>
  );
}
