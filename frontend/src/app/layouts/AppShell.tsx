import { Suspense, useEffect, useRef, useState } from "react";
import { Outlet, useLocation, useNavigate } from "react-router";
import { AnimatePresence, motion } from "motion/react";
import { AlertTriangle, Building2, CheckCircle2, Clock3, HelpCircle, LogOut, Menu, Monitor, Moon, Sun, X } from "lucide-react";

import { useAuth } from "../../core/auth/useAuth";
import { useViewAsRole } from "../../core/permissions/useViewAsRole";
import { roleDescriptions, roleLabels, roleVisibleNav } from "../../core/permissions/roles";
import { resolveEnabledModules } from "../../core/products/moduleDefaults";
import { isRouteBlocked } from "../../core/permissions/roles";
import { SimulationBanner } from "../../core/permissions/components/SimulationBanner";
import { ReadOnlyBanner } from "../../core/permissions/components/ReadOnlyBanner";
import { ToasterHost } from "../../core/notifications/components/ToasterHost";
import { FeedbackModal } from "../../core/notifications/components/FeedbackModal";
import { useFeedbackModal } from "../../core/notifications/useFeedbackModal";
import { PendingNotificationGate } from "../../core/notifications/PendingNotificationGate";
import { feedbackService } from "../../core/notifications/services/feedbackService";

import { AegisLogo } from "../../shared/components/AegisLogo";
import { Switcher, type SwitcherItem } from "../../shared/components/Switcher";
import { GlobalSearch } from "../../shared/components/GlobalSearch";
import { Notifications } from "../../shared/components/Notifications";
import { useTheme } from "../providers/useTheme";
import { useAsyncData } from "../../shared/hooks/useAsyncData";

import { nav, tabsForPath } from "./navConfig";
import { AppNav } from "./AppNav";
import { ModuleTabs } from "./ModuleTabs";
import { ScreenSkeleton } from "./ScreenSkeleton";

import type { UserRole } from "../../shared/types";

// Routes where the read-only banner for "viewer" should NOT show (the viewer
// can browse these read-only-friendly sections without a banner). Mirrors the
// original screen-name allowlist from AppShell in App.tsx.
const viewerQuietRoutes = [
  "/dashboard", "/products", "/analytics", "/content",
];

export function AppShell() {
  const { authUser, effectiveTenant, effectiveProduct, logout, userTenants, tenantProducts, switchTenant, switchProduct } = useAuth();
  const { viewAsRole, setViewAsRole, restore } = useViewAsRole();
  const { theme, setTheme } = useTheme();
  const navigate = useNavigate();
  const location = useLocation();

  const [mobile, setMobile] = useState(false);
  const [showUserMenu, setShowUserMenu] = useState(false);
  const userMenuRef = useRef<HTMLDivElement>(null);
  const { open: showFeedback, setOpen: setShowFeedback } = useFeedbackModal();
  const [transitioning, setTransitioning] = useState(false);

  const prevRoot = useRef<string>(location.pathname.split("/")[1] ?? "dashboard");
  const firstRender = useRef(true);

  useEffect(() => {
    const root = location.pathname.split("/")[1] ?? "dashboard";
    if (firstRender.current) { firstRender.current = false; prevRoot.current = root; return; }
    if (root !== prevRoot.current) {
      prevRoot.current = root;
      setTransitioning(true);
      const t = setTimeout(() => setTransitioning(false), 360);
      return () => clearTimeout(t);
    }
  }, [location.pathname]);

  // Sprint 15, Tarefa D.2 — único dropdown do AppShell sem overlay nem
  // clique-fora (diferente de `Notifications.tsx`, que já tem overlay).
  // Mesmo padrão de "ref + listener de mousedown no document" de
  // `ContextActionMenu.tsx`, adaptado porque aqui o gatilho fica no próprio
  // header (sem portal) em vez de um menu de contexto posicionado livre.
  useEffect(() => {
    if (!showUserMenu) return;
    const handleClickOutside = (e: MouseEvent) => {
      if (userMenuRef.current && !userMenuRef.current.contains(e.target as Node)) setShowUserMenu(false);
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [showUserMenu]);

  // Sprint 23 — badge de contagem no item "Feedbacks" da sidebar, só
  // relevante para super_admin (único papel com o item visível, ver roles.ts).
  // `location.pathname` entra nas deps para refletir feedbacks criados via
  // FeedbackModal durante a sessão — o store mock é um array em memória sem
  // pub/sub, então cada navegação é o gatilho barato de recontagem (mesmo
  // comportamento "eventualmente consistente" que a UI já assume em outros
  // badges deste app).
  const { data: feedbackList } = useAsyncData(
    () => (viewAsRole === "super_admin" ? feedbackService.listAll() : Promise.resolve([])),
    [viewAsRole, location.pathname],
  );
  const openFeedbackCount = (feedbackList ?? []).filter((f) => f.status === "aberto").length;

  if (!authUser || !effectiveTenant || !effectiveProduct) return null;

  const handleLogout = () => { logout(); navigate("/login"); };

  const themeIcon = { light: <Sun size={16} />, dark: <Moon size={16} />, auto: <Monitor size={16} /> }[theme];
  const themeNext = { light: "dark", dark: "auto", auto: "light" } as Record<typeof theme, typeof theme>;

  const userTenantSwitcherItems: SwitcherItem[] = userTenants.filter((t) => t.status === "ativo").map((t) => ({ id: t.id, name: t.name, meta: t.plan }));
  const productSwitcherItems: SwitcherItem[] = tenantProducts.filter((p) => p.status !== "Arquivado" && p.modules > 0).map((p) => ({ id: p.id, name: p.name, meta: p.type }));

  // Sprint 15, Tarefa B (ADR-0015) — item de nav ligado a um módulo opcional
  // só aparece quando o módulo está habilitado NESTE produto, além do papel
  // permitir; antes só o papel era checado, então até Super Admin via
  // "Knowledge Graph" num produto sem o módulo habilitado.
  const enabledModules = resolveEnabledModules(effectiveProduct);
  const visibleNav = nav.filter((item) => roleVisibleNav[viewAsRole].has(item.path) && (!item.moduleKey || enabledModules.includes(item.moduleKey)));

  const tabs = tabsForPath(location.pathname);
  const blocked = isRouteBlocked(viewAsRole, location.pathname);

  const showViewerBanner = viewAsRole === "viewer" && !viewerQuietRoutes.some((r) => location.pathname.startsWith(r));
  const showEditorBanner = viewAsRole === "editor" && (location.pathname.includes("/publish") || location.pathname.startsWith("/settings/permissions") || location.pathname.startsWith("/settings/roles"));

  const side = (
    <aside className="flex h-full w-[272px] shrink-0 flex-col overflow-y-auto border-r border-border bg-sidebar p-3">
      <div className="mb-6 flex items-center gap-3 px-2 pt-1">
        <AegisLogo size="sm" />
        <div><p className="font-semibold tracking-[-.02em]">Aegis PMS</p><p className="text-[11px] text-muted-foreground">Product OS</p></div>
      </div>
      <nav data-tour="sidebar-nav" className="space-y-1">
        {visibleNav.map((item) => {
          const Icon = item.icon;
          const active = location.pathname === item.path || location.pathname.startsWith(item.path + "/");
          const badge = item.path === "/admin/feedback" ? openFeedbackCount : 0;
          return (
            <button key={item.path} onClick={() => { navigate(item.path); setMobile(false); }} className={`flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm transition ${active ? "bg-sidebar-accent text-sidebar-accent-foreground" : "text-muted-foreground hover:bg-muted hover:text-foreground"}`}>
              <Icon size={17} /><span>{item.label}</span>
              {badge > 0 && <span className="ml-auto grid h-[18px] min-w-[18px] place-items-center rounded-full bg-primary px-1 text-[10px] font-semibold text-primary-foreground">{badge}</span>}
            </button>
          );
        })}
      </nav>
      <div className="mt-auto rounded-xl border border-border bg-[var(--byop-violet-soft)] p-3 text-xs">
        <p className="flex items-center gap-1.5 font-medium text-[var(--byop-violet-dark)]"><Clock3 size={13} />Recentemente</p>
        <p className="mt-1 text-muted-foreground">{effectiveProduct.name} recebeu respostas e exige revisão.</p>
      </div>
      <p className="mt-2 text-center text-[9px] text-muted-foreground/40">Aegis PMS · Sprint 19 · Protótipo</p>
    </aside>
  );

  return (
    <div className="flex h-screen flex-col overflow-hidden bg-background text-foreground">
      <ToasterHost />
      <header className="shrink-0 z-30 flex h-14 items-center gap-2 border-b border-border bg-background/80 px-3 backdrop-blur-xl md:px-5" style={{ backdropFilter: "blur(20px) saturate(180%)" }}>
        <button onClick={() => setMobile(true)} className="rounded-xl border border-border bg-card p-2 transition hover:bg-muted lg:hidden"><Menu size={17} /></button>
        <div className="hidden lg:block">
          <Switcher label="Tenant" active={effectiveTenant.name} items={userTenantSwitcherItems} onSelect={(item) => { switchTenant(item.id); navigate("/dashboard"); }} />
        </div>
        <div className="hidden md:block">
          <Switcher label="Produto" active={effectiveProduct.name} items={productSwitcherItems} onSelect={(item) => { switchProduct(item.id); navigate("/dashboard"); }} />
        </div>
        <div className="ml-auto flex items-center gap-1.5">
          {viewAsRole === "super_admin" && (
            <button onClick={() => navigate("/select-tenant")} className="flex items-center gap-1.5 rounded-xl border border-border bg-card px-3 py-1.5 text-sm transition hover:bg-muted" title="Ir para a tela de Super Admin">
              <Building2 size={15} /><span className="hidden sm:inline">Super Admin</span>
            </button>
          )}
          <GlobalSearch />
          <Notifications />
          <button onClick={() => setTheme(themeNext[theme])} className="rounded-xl border border-border bg-card p-2 transition hover:bg-muted" title="Alternar tema">{themeIcon}</button>
          <button onClick={() => navigate("/help")} className="rounded-xl border border-border bg-card p-2 transition hover:bg-muted" title="Central de Ajuda"><HelpCircle size={16} /></button>
          <div className="relative" ref={userMenuRef}>
            <button onClick={() => setShowUserMenu(!showUserMenu)} className="flex items-center gap-2 rounded-full border border-border bg-card py-1 pl-1 pr-2.5 text-sm transition hover:bg-muted">
              <div className="grid h-7 w-7 place-items-center rounded-full bg-primary text-primary-foreground text-xs font-semibold">{authUser.initials}</div>
              <span className="hidden sm:block">{authUser.name.split(" ")[0]}</span>
            </button>
            <AnimatePresence>
              {showUserMenu && (
                <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: 10 }} className="absolute right-0 top-11 z-40 w-60 rounded-2xl border border-border bg-card p-2 shadow-[0_8px_32px_rgba(0,0,0,0.12)] dark:shadow-[0_8px_32px_rgba(0,0,0,0.4)]">
                  <div className="px-3 py-2.5 mb-1">
                    <p className="font-medium text-sm">{authUser.name}</p>
                    <p className="text-xs text-muted-foreground">{authUser.email}</p>
                    <div className="mt-1.5"><span className="rounded-full bg-[#ede9fe] px-2 py-1 text-[11px] font-medium text-[#7c3aed]">{roleLabels[authUser.role]}</span></div>
                  </div>
                  <div className="border-t border-border pt-1">
                    <button onClick={() => { setShowFeedback(true); setShowUserMenu(false); }} className="flex w-full items-center gap-2 rounded-xl px-3 py-2 text-sm transition hover:bg-muted"><AlertTriangle size={14} className="text-[#b45309]" />Reportar problema</button>
                  </div>
                  <div className="border-t border-border py-1">
                    <p className="px-3 pb-1 pt-2 text-[10px] font-medium uppercase tracking-wide text-muted-foreground">Simular perfil</p>
                    {(["super_admin", "tenant_admin", "product_manager", "editor", "viewer"] as UserRole[]).map((r) => (
                      <button key={r} onClick={() => { setViewAsRole(r); setShowUserMenu(false); }} className={`flex w-full items-center justify-between rounded-xl px-3 py-1.5 text-xs transition hover:bg-muted ${viewAsRole === r ? "font-semibold text-primary" : ""}`}>
                        <span>{roleLabels[r]}</span>
                        {viewAsRole === r ? <CheckCircle2 size={12} className="text-primary" /> : <span className="text-[10px] text-muted-foreground">{roleDescriptions[r].split(" ")[0]}</span>}
                      </button>
                    ))}
                  </div>
                  <div className="border-t border-border pt-1">
                    <button onClick={() => { setShowUserMenu(false); setTheme(themeNext[theme]); }} className="flex w-full items-center gap-2 rounded-xl px-3 py-2 text-sm hover:bg-muted"><span className="text-muted-foreground">{themeIcon}</span>Tema: {theme === "light" ? "Claro" : theme === "dark" ? "Escuro" : "Automático"}</button>
                    <button onClick={() => { setShowUserMenu(false); handleLogout(); }} className="flex w-full items-center gap-2 rounded-xl px-3 py-2 text-sm text-destructive hover:bg-muted"><LogOut size={15} />Sair da plataforma</button>
                  </div>
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        </div>
      </header>

      <SimulationBanner viewAs={viewAsRole} actual={authUser.role} onRestore={restore} />

      <PendingNotificationGate />
      <AnimatePresence>{showFeedback && <FeedbackModal key="fb" screenName={location.pathname} onClose={() => setShowFeedback(false)} />}</AnimatePresence>
      <AnimatePresence>
        {mobile && (
          <motion.div className="fixed inset-0 z-50 bg-black/20" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
            <motion.div className="h-full" initial={{ x: -280 }} animate={{ x: 0 }} exit={{ x: -280 }}>
              {side}
              <button onClick={() => setMobile(false)} className="absolute left-[292px] top-4 rounded-full bg-card p-2"><X size={18} /></button>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      <div className="flex flex-1 overflow-hidden">
        <div className="hidden lg:flex">{side}</div>
        <main className="min-w-0 flex-1 overflow-y-auto px-4 py-5 md:px-8 lg:px-10">
          <AppNav />
          {transitioning ? <ScreenSkeleton /> : (
            <>
              {!blocked && showViewerBanner && <ReadOnlyBanner role={viewAsRole} />}
              {!blocked && showEditorBanner && <ReadOnlyBanner role={viewAsRole} />}
              {tabs && <ModuleTabs tabs={tabs} />}
              <Suspense fallback={<ScreenSkeleton />}><Outlet /></Suspense>
            </>
          )}
        </main>
      </div>
    </div>
  );
}
