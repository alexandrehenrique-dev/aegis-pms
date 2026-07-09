import { useLocation, useNavigate } from "react-router";
import { ChevronRight } from "lucide-react";
import { useAuth } from "../../core/auth/useAuth";
import { nav } from "./navConfig";

const sectionLabels: Record<string, string> = Object.fromEntries(nav.map((n) => [n.path, n.label]));

/**
 * Breadcrumb-style navigation header, replacing the original `AppNav` that
 * derived its trail from the `Screen` union + `parentModule` map. Now derives
 * the trail directly from the current route path.
 */
export function AppNav() {
  const { effectiveTenant, effectiveProduct } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  if (!effectiveTenant) return null;

  const segments = location.pathname.split("/").filter(Boolean);
  const root = segments[0] ? `/${segments[0]}` : "/dashboard";
  const isRoot = root === "/dashboard";
  const isTopOnly = root === "/products" && segments.length <= 1 || location.pathname === "/products/new";
  const isProductCtx = !isRoot && !isTopOnly && !!effectiveProduct;
  const sectionLabel = sectionLabels[root];

  const sep = <ChevronRight size={11} className="shrink-0 text-border" />;
  const btn = (label: string, path: string) => (
    <button onClick={() => navigate(path)} className="transition-colors hover:text-foreground">{label}</button>
  );

  return (
    <nav className="mb-5 flex flex-wrap items-center gap-1.5 text-xs text-muted-foreground">
      {btn(effectiveTenant.name, "/dashboard")}
      {!isRoot && <>{sep}</>}
      {isTopOnly && <span className="text-foreground">{location.pathname === "/products/new" ? "Novo Produto" : "Produtos"}</span>}
      {isProductCtx && (
        <>
          {btn("Produtos", "/products")}
          {sep}
          {btn(effectiveProduct?.name ?? "Produto", "/dashboard")}
          {sectionLabel && <>{sep}<span className="text-foreground">{sectionLabel}</span></>}
        </>
      )}
    </nav>
  );
}
