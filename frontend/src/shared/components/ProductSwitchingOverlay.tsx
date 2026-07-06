import { AegisLogo } from "./AegisLogo";
import { SkeletonLines } from "./Primitives";
import type { ProductOption } from "../types";

/**
 * Feedback visual de troca de contexto de produto (E.8, BUG-SPRINT
 * consolidado) — antes, trocar de produto via `Switcher`/`ProductCard`
 * atualizava a interface instantaneamente sem nenhum indicador, dando a
 * impressão de que nada aconteceu ou de que dados do produto anterior ainda
 * estavam na tela.
 */
export function ProductSwitchingOverlay({ product }: { product: ProductOption | null }) {
  return (
    <div className="fixed inset-0 z-50 flex flex-col items-center justify-center gap-4 bg-background">
      <AegisLogo size="lg" className="animate-pulse" />
      <p className="text-sm font-medium text-muted-foreground">Carregando {product?.name ?? "produto"}…</p>
      <div className="mt-4 w-full max-w-xl space-y-3 px-8"><SkeletonLines /></div>
    </div>
  );
}
