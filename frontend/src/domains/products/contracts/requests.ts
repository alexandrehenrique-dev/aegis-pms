import type { ProductStatus } from "../../../shared/types";

export type AssetStorageStrategy = "local" | "s3";

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
  /**
   * Escolhida no passo "Armazenamento de assets" do wizard (Sprint 13,
   * Tarefa H.3) — `"local"` é o default (o backend cria a estrutura de
   * pastas do produto); `"s3"` exige bucket/região, que podem ficar em
   * branco e ser configurados depois em Configurações do produto.
   */
  assetStorageStrategy: AssetStorageStrategy;
  s3Bucket?: string;
  s3Region?: string;
};

export type UpdateProductRequest = {
  name: string;
  type: string;
  status: ProductStatus;
  /** Mesma seleção de módulos da criação (Field/checkbox por módulo, defaults por tipo) — ver EditProductModal/core/products/moduleDefaults.ts. */
  modules: string[];
};

export type DeleteProductRequest = {
  /** Texto digitado pelo usuário para confirmar a exclusão (ex.: o nome do produto). Validado na própria tela, não pelo service — aqui só registra a intenção para o backend futuro auditar. Mapeia para o `DELETE /api/v1/admin/products/{productId}` (soft delete) descrito em docs/AEGIS_PMS_V1.md §8.4/§8.5 — produto nunca é apagado fisicamente. */
  confirmationText: string;
};
