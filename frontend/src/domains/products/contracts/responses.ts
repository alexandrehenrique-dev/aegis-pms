import type { ComponentType } from "react";
import type { ModuleState, ProductStatus } from "../../../shared/types";

export type ProductSummary = {
  id?: string;
  name: string;
  type: string;
  status: ProductStatus;
  modules: number;
  last: string;
  score: string;
  tenantId?: string;
};

export type ListProductsResponse = ProductSummary[];

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
