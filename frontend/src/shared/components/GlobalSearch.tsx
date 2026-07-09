import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router";
import { AnimatePresence, motion } from "motion/react";
import { Command, Search, X } from "lucide-react";
import { EmptyState } from "./Primitives";
import { fade } from "./motion";
import { useAuth } from "../../core/auth/useAuth";
import { useViewAsRole } from "../../core/permissions/useViewAsRole";
import { contentService } from "../../domains/content/services/contentService";
import { assetsService } from "../../domains/assets/services/assetsService";
import { formsService } from "../../domains/forms/services/formsService";
import type { ProductOption } from "../types";

type SearchEntry = {
  cat: string;
  label: string;
  sub: string;
  path: string;
  product?: ProductOption;
  tenantId?: string;
};

function productStatusLabel(status: string): string {
  return status || "sem status";
}

function productSub(product: ProductOption): string {
  return `${product.type} · ${productStatusLabel(product.status)}`;
}

function uniqueProducts(userProducts: Record<string, ProductOption[]>): SearchEntry[] {
  const seen = new Set<string>();
  return Object.entries(userProducts).flatMap(([tenantId, products]) => (
    products
      .filter((product) => {
        const key = product.id || `${tenantId}:${product.name}`;
        if (seen.has(key)) return false;
        seen.add(key);
        return true;
      })
      .map((product) => ({
        cat: "Produtos",
        label: product.name,
        sub: productSub(product),
        path: product.id ? `/products/${product.id}` : "/select-product",
        product,
        tenantId,
      }))
  ));
}

export function GlobalSearch() {
  const navigate = useNavigate();
  const { effectiveProduct, selectProduct, selectTenant, userProducts, userTenants } = useAuth();
  const { viewAsRole } = useViewAsRole();
  const [open, setOpen] = useState(false);
  const [q, setQ] = useState("");
  const [scopedEntries, setScopedEntries] = useState<SearchEntry[]>([]);

  useEffect(() => {
    const f = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "k") { e.preventDefault(); setOpen(true); }
    };
    window.addEventListener("keydown", f);
    return () => window.removeEventListener("keydown", f);
  }, []);

  useEffect(() => {
    if (!open || !effectiveProduct?.id) {
      setScopedEntries([]);
      return;
    }

    let alive = true;
    Promise.allSettled([
      contentService.listContentByProduct(effectiveProduct.id),
      assetsService.listAssets(effectiveProduct.id),
      formsService.listForms(effectiveProduct.id),
    ]).then(([contentResult, assetsResult, formsResult]) => {
      if (!alive) return;
      const entries: SearchEntry[] = [];
      if (contentResult.status === "fulfilled") {
        entries.push(...contentResult.value.map((item) => ({
          cat: "Conteúdo",
          label: item.title,
          sub: `${item.status} · ${item.lang}`,
          path: `/content/${item.id}/editor`,
        })));
      }
      if (assetsResult.status === "fulfilled") {
        entries.push(...assetsResult.value.map((item) => ({
          cat: "Assets",
          label: item.name,
          sub: `${item.type} · ${item.size}`,
          path: item.id ? `/assets/${item.id}` : "/assets",
        })));
      }
      if (formsResult.status === "fulfilled") {
        entries.push(...formsResult.value.map((item) => ({
          cat: "Formulários",
          label: item.name,
          sub: `${item.responses} respostas · ${item.publication}`,
          path: `/forms/${item.id}/builder`,
        })));
      }
      setScopedEntries(entries);
    });

    return () => { alive = false; };
  }, [effectiveProduct?.id, open]);

  const productEntries = useMemo(() => {
    const allProducts = uniqueProducts(userProducts);
    if (viewAsRole === "super_admin" || viewAsRole === "tenant_admin" || !effectiveProduct) return allProducts;
    return allProducts.filter((entry) => entry.product?.id === effectiveProduct.id || entry.product?.name === effectiveProduct.name);
  }, [effectiveProduct, userProducts, viewAsRole]);

  const searchIndex = useMemo(() => [
    ...productEntries,
    ...scopedEntries,
  ], [productEntries, scopedEntries]);

  const results = q.length > 0 ? searchIndex.filter((e) => (e.label + e.sub + e.cat).toLowerCase().includes(q.toLowerCase())) : searchIndex;
  const grouped = results.reduce((acc, e) => { (acc[e.cat] ??= []).push(e); return acc; }, {} as Record<string, SearchEntry[]>);
  const handleSelect = (e: SearchEntry) => {
    if (e.product) {
      const tenant = userTenants.find((item) => item.id === e.tenantId) ?? null;
      selectTenant(tenant);
      selectProduct(e.product);
    }
    navigate(e.path);
    setOpen(false);
    setQ("");
  };

  return (
    <>
      <button onClick={() => setOpen(true)} className="hidden min-w-[240px] items-center justify-between rounded-xl border border-border bg-card px-3 py-1.5 text-sm text-muted-foreground transition hover:bg-muted lg:flex">
        <span className="flex items-center gap-2"><Search size={14} />Buscar</span>
        <kbd className="rounded border bg-muted px-1.5 py-0.5 font-mono text-[11px]">⌘K</kbd>
      </button>
      <button onClick={() => setOpen(true)} className="rounded-xl border border-border bg-card p-2 transition hover:bg-muted lg:hidden">
        <Search size={16} />
      </button>
      <AnimatePresence>
        {open && (
          <motion.div className="fixed inset-0 z-50 bg-black/20 p-3 pt-[10vh] backdrop-blur-[4px]" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={() => setOpen(false)}>
            <motion.div {...fade} className="mx-auto max-w-xl overflow-hidden rounded-2xl border border-border bg-card shadow-[0_24px_80px_rgba(0,0,0,0.2)]" onClick={(e) => e.stopPropagation()}>
              <div className="flex items-center gap-3 border-b border-border px-4 py-3">
                <Command size={16} className="text-muted-foreground" />
                <input autoFocus value={q} onChange={(e) => setQ(e.target.value)} placeholder="Buscar produtos, conteúdo, assets, formulários..." className="w-full bg-transparent text-sm outline-none" />
                {q && <button onClick={() => setQ("")} className="text-muted-foreground hover:text-foreground"><X size={15} /></button>}
                <button onClick={() => setOpen(false)} className="rounded-lg p-1 text-muted-foreground hover:bg-muted hover:text-foreground"><kbd className="font-mono text-xs">ESC</kbd></button>
              </div>
              <div className="max-h-[60vh] overflow-y-auto p-2">
                {Object.keys(grouped).length === 0
                  ? <EmptyState compact title="Sem resultados" description="Nenhum item corresponde à busca." />
                  : Object.entries(grouped).map(([cat, items]) => (
                    <div key={cat} className="mb-2">
                      <p className="mb-1 px-2 font-mono text-[10px] uppercase tracking-wide text-muted-foreground">{cat}</p>
                      {items.map((item) => (
                        <button key={item.label + item.sub} onClick={() => handleSelect(item)} className="flex w-full items-center justify-between rounded-xl px-3 py-2 text-left text-sm transition hover:bg-muted">
                          <span className="font-medium">{item.label}</span>
                          <span className="text-xs text-muted-foreground">{item.sub}</span>
                        </button>
                      ))}
                    </div>
                  ))}
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
}
