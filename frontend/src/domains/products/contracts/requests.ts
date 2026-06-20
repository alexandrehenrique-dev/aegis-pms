import type { ProductStatus } from "../../../shared/types";

export type CreateProductRequest = {
  name: string;
  slug: string;
  type: string;
  language: string;
  description: string;
  template: string;
  initialModules: string[];
  /** Presente quando o produto nasce dentro do wizard de Super Admin (Sprint 09, Tarefa C). */
  tenantId?: string;
};

export type UpdateProductRequest = {
  name: string;
  type: string;
  status: ProductStatus;
};

export type DeleteProductRequest = {
  /** Texto digitado pelo usuário para confirmar a exclusão (ex.: o nome do produto). Validado na própria tela, não pelo service — aqui só registra a intenção para o backend futuro auditar. Mapeia para o `DELETE /api/v1/admin/products/{productId}` (soft delete) descrito em docs/AEGIS_PMS_V1.md §8.4/§8.5 — produto nunca é apagado fisicamente. */
  confirmationText: string;
};
