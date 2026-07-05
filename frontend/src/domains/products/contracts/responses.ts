import type { ComponentType } from "react";
import type { ModuleState, ProductStatus } from "../../../shared/types";

export type ProductSummary = {
  id?: string;
  key?: string;
  name: string;
  type: string;
  status: ProductStatus;
  modules: number;
  last: string;
  score: string;
  tenantId?: string;
  /** Módulos selecionados (chaves de `core/products/moduleDefaults.ts`) — editável, mesma UI da criação (ver EditProductModal). */
  modulesList?: string[];
  /** Papel do caller neste produto (`ProductAssignmentRole` do backend), independente do papel de plataforma — ver `ProductOption.callerAssignedRole`. */
  callerAssignedRole?: string | null;
};

export type ListProductsResponse = ProductSummary[];

/**
 * Shape mínimo para Editar/Excluir um produto (ver `useProductActions`).
 * `ProductSummary` (esta tela) e `ProductOption` (shared/types/auth —
 * usado por `ProductSelectScreen`, core/auth) satisfazem este tipo
 * estruturalmente, então o mesmo hook/componentes servem as duas telas
 * sem reusar o modelo de dados de uma na outra.
 */
export type EditableProduct = {
  id?: string;
  name: string;
  type: string;
  status: ProductStatus;
  modulesList?: string[];
  /** Sprint 15, Tarefa E.1 — presente apenas quando o chamador é `ProductSelectScreen` (vem de `ProductOption`); habilita o item Favoritar/Desfavoritar no menu de contexto. */
  isFavorite?: boolean;
};

export type ModuleCatalogItem = {
  Icon: ComponentType<{ size?: number; className?: string }>;
  name: string;
  desc: string;
  state: ModuleState;
  maturity: string;
  dependency: string;
  impact: string;
};

export type ListModulesResponse = ModuleCatalogItem[];
