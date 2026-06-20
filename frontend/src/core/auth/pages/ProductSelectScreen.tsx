import { useState, type ReactNode } from "react";
import { useNavigate } from "react-router";
import { motion, AnimatePresence } from "motion/react";
import { ArrowLeft, Building2, ChevronRight, Clock3, LogOut, MoreVertical, Plus, Star } from "lucide-react";
import { useAuth } from "../AuthContext";
import { getPostLoginLandingPath } from "../../permissions/roles";
import { AegisLogo } from "../../../shared/components/AegisLogo";
import { ProductStatusBadge } from "../../../shared/components/ProductStatusBadge";
import { Button, EmptyState, fade } from "../../../shared/components/Primitives";
import { MobileDrawerMenu } from "../../../shared/components/MobileDrawerMenu";
// domains/products é consumido aqui mesmo vivendo em core/auth — mesmo
// padrão de domains/tenants em TenantSelectScreen. Botão direito em um
// produto bloqueado/sem módulos (que não pode ser aberto) é o único jeito
// de editá-lo ou excluí-lo, espelhando a jornada de tenant suspenso.
import { useProductActions, type ProductPersistence } from "../../../domains/products/hooks/useProductActions";
import { CreateProductModal } from "../../../domains/products/components/CreateProductModal";
import type { ProductOption } from "../../../shared/types";

/**
 * Componente de nível superior (não definido dentro de `ProductSelectScreen`)
 * de propósito: `useProductActions` guarda estado de menu/modal por card —
 * se `PCard` fosse recriado a cada render do pai (ex.: a cada tecla digitada
 * na busca), perderia identidade e React remontaria o card, fechando
 * qualquer modal aberto no meio da edição.
 */
function PCard({ p, onSelect }: { p: ProductOption; onSelect: (p: ProductOption) => void }) {
  const { updateProduct, removeProduct } = useAuth();
  const disabled = p.status === "Arquivado" || p.modules === 0;
  const persistence: ProductPersistence = {
    update: async (req) => updateProduct(p.id, req),
    remove: async (req) => removeProduct(p.id, req),
  };
  // AuthContext já é reativo (setUserProducts dispara o re-render via
  // tenantProducts derivado) — não precisa de um refresh manual aqui,
  // diferente de ProductsList/ProductCard (productsService isolado).
  const { canManage, onContextMenu, portal } = useProductActions(p, persistence, () => {});

  return (
    <div className="relative">
      {portal}
      <button
        onClick={() => !disabled && onSelect(p)}
        onContextMenu={onContextMenu}
        disabled={disabled}
        className="group w-full rounded-2xl border border-border bg-card p-5 text-left transition hover:border-primary hover:shadow-[0_8px_30px_rgba(15,61,46,.08)] disabled:cursor-not-allowed disabled:opacity-40"
      >
        <div className="flex items-start justify-between gap-3">
          <div>
            <div className="flex items-center gap-1.5"><p className="font-semibold">{p.name}</p>{p.isFavorite && <Star size={12} className="fill-[#D97706] text-[#D97706]" />}</div>
            <p className="text-xs text-muted-foreground">{p.type}</p>
          </div>
          <ProductStatusBadge status={p.status} />
        </div>
        <div className="mt-4 flex items-center justify-between text-xs text-muted-foreground">
          <span>{p.modules} módulo{p.modules === 1 ? "" : "s"}{p.modules === 0 ? " · sem módulos" : ""}</span>
          {!disabled && <span className="text-primary opacity-0 transition group-hover:opacity-100">Abrir →</span>}
        </div>
        {disabled && canManage && <p className="mt-2 text-xs text-muted-foreground hidden lg:block">Botão direito para editar ou excluir.</p>}
      </button>
      {/* Botão direito não é uma interação confiável em touch — em mobile, este "⋮" visível abre o mesmo menu (ver ContextActionMenu). Fica fora do <button> do card para não criar botão-dentro-de-botão. */}
      {canManage && (
        <button
          onClick={onContextMenu}
          aria-label={`Ações de ${p.name}`}
          className="absolute right-2 top-2 rounded-lg p-1.5 text-muted-foreground transition hover:bg-muted lg:hidden"
        >
          <MoreVertical size={16} />
        </button>
      )}
    </div>
  );
}

function Sec({ title, icon, items, onSelect }: { title: string; icon?: ReactNode; items: ProductOption[]; onSelect: (p: ProductOption) => void }) {
  if (items.length === 0) return null;
  return (
    <div>
      <div className="mb-2 flex items-center gap-1.5 text-xs font-medium text-muted-foreground">{icon}{title}</div>
      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">{items.map((p) => <PCard key={p.id} p={p} onSelect={onSelect} />)}</div>
    </div>
  );
}

export function ProductSelectScreen() {
  const navigate = useNavigate();
  const { authUser, effectiveTenant, tenantProducts, selectProduct, selectTenant, logout } = useAuth();
  const [q, setQ] = useState("");
  const [sf, setSf] = useState("todos");
  const [showCreateProduct, setShowCreateProduct] = useState(false);

  if (!authUser || !effectiveTenant) return null;

  const canCreateProduct = !["editor", "viewer"].includes(authUser.role);
  const filtered = tenantProducts.filter((p) => (p.name + p.type).toLowerCase().includes(q.toLowerCase()) && (sf === "todos" || p.status === sf));
  const favs = filtered.filter((p) => p.isFavorite);
  const recents = filtered.filter((p) => p.isRecent && !p.isFavorite);
  const others = filtered.filter((p) => !p.isFavorite && !p.isRecent);

  const handleSelect = (p: ProductOption) => { selectProduct(p); navigate(getPostLoginLandingPath(authUser.role, p)); };
  const handleBack = () => { selectTenant(null); navigate("/select-tenant"); };
  const handleLogout = () => { logout(); navigate("/login"); };
  const handleProductCreated = (created: { id?: string; name: string; type: string; status: ProductOption["status"]; modules: number }) => {
    setShowCreateProduct(false);
    const product: ProductOption = { id: created.id ?? created.name, name: created.name, type: created.type, status: created.status, modules: created.modules };
    selectProduct(product);
    navigate(getPostLoginLandingPath(authUser.role, product));
  };

  return (
    <div className="min-h-screen bg-background">
      <header className="flex items-center justify-between border-b border-border bg-card px-4 py-4 shadow-[0_1px_0_rgba(0,0,0,0.06)] dark:shadow-none sm:px-6">
        <div className="flex min-w-0 items-center gap-3">
          <div className="flex shrink-0 items-center gap-2.5"><AegisLogo size="sm" /><span className="font-semibold tracking-[-.02em]">Aegis</span></div>
          <ChevronRight size={14} className="hidden shrink-0 text-muted-foreground sm:block" />
          <div className="flex min-w-0 items-center gap-1.5"><Building2 size={14} className="shrink-0 text-muted-foreground" /><span className="truncate text-sm font-medium">{effectiveTenant.name}</span></div>
        </div>
        <div className="hidden items-center gap-2 lg:flex">
          <button onClick={handleBack} className="flex items-center gap-1.5 rounded-xl border border-border bg-card px-3 py-1.5 text-sm transition hover:bg-muted"><ArrowLeft size={14} />Trocar tenant</button>
          <button onClick={handleLogout} className="flex items-center gap-1.5 rounded-xl border border-border bg-card px-3 py-1.5 text-sm transition hover:bg-muted"><LogOut size={14} />Sair</button>
        </div>
        <div className="shrink-0 lg:hidden">
          <MobileDrawerMenu label="Filtros de produto" title="Filtrar por status">
            {["todos", "Ativo", "Pendente", "Arquivado"].map((s) => (
              <Button key={s} onClick={() => setSf(s)} primary={sf === s} className="w-full">{s}</Button>
            ))}
            {canCreateProduct && <Button primary onClick={() => setShowCreateProduct(true)} className="w-full"><Plus size={15} />Criar Produto</Button>}
            <Button onClick={handleBack} className="w-full"><ArrowLeft size={14} />Trocar tenant</Button>
            <Button onClick={handleLogout} className="w-full"><LogOut size={14} />Sair</Button>
          </MobileDrawerMenu>
        </div>
      </header>
      <main className="mx-auto max-w-5xl px-4 py-10">
        <motion.div {...fade}>
          <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
            <div>
              <h1 className="text-2xl font-semibold tracking-[-.02em]">Selecione um produto</h1>
              <p className="mt-1 text-sm text-muted-foreground">{effectiveTenant.name} · {tenantProducts.length} produto{tenantProducts.length !== 1 ? "s" : ""} · Botão direito sobre um produto bloqueado/sem módulos para editar ou excluir.</p>
            </div>
            {canCreateProduct && <div className="hidden lg:block"><Button primary onClick={() => setShowCreateProduct(true)}><Plus size={15} />Criar Produto</Button></div>}
          </div>
          <div className="mb-4 flex flex-wrap gap-2">
            <div className="flex min-w-[200px] flex-1 items-center gap-2 rounded-xl border border-border bg-card px-3 py-2.5">
              <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Buscar produto..." className="w-full bg-transparent text-sm outline-none" />
            </div>
            <div className="hidden gap-2 lg:flex">
              {["todos", "Ativo", "Pendente", "Arquivado"].map((s) => (
                <button key={s} onClick={() => setSf(s)} className={`rounded-xl border px-3 py-2 text-sm transition ${sf === s ? "border-primary bg-primary/5 text-primary" : "border-border bg-card text-muted-foreground hover:bg-muted"}`}>{s}</button>
              ))}
            </div>
          </div>
          {filtered.length === 0 ? <EmptyState title="Nenhum produto encontrado" description="Ajuste os filtros ou limpe a busca." /> : (
            <div className="space-y-6">
              <Sec title="Favoritos" icon={<Star size={12} className="text-[#D97706]" />} items={favs} onSelect={handleSelect} />
              <Sec title="Acessados recentemente" icon={<Clock3 size={12} />} items={recents} onSelect={handleSelect} />
              <Sec title={favs.length > 0 || recents.length > 0 ? "Todos os produtos" : "Produtos"} items={others} onSelect={handleSelect} />
            </div>
          )}
        </motion.div>
      </main>
      <AnimatePresence>
        {showCreateProduct && (
          <CreateProductModal
            tenantId={effectiveTenant.id}
            tenantName={effectiveTenant.name}
            onClose={() => setShowCreateProduct(false)}
            onCreated={handleProductCreated}
          />
        )}
      </AnimatePresence>
    </div>
  );
}
