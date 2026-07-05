import type { ProductStatus } from "../../../shared/types";
import type { ProductSummary } from "../contracts/responses";

export type ProductSummaryDto = {
  id: string;
  tenantId: string;
  key: string;
  name?: string | null;
  type: string;
  status: string;
  defaultLocale?: string;
  assetStorageStrategy?: string;
  createdAt?: string;
  updatedAt?: string;
  enabledModuleCount?: number;
};

const PRODUCT_STATUS: Record<string, ProductStatus> = {
  ACTIVE: "Ativo",
  PENDING: "Pendente",
  ARCHIVED: "Arquivado",
  INACTIVE: "Sem módulos",
  SUSPENDED: "Sem módulos",
  DELETING: "Arquivado",
  DELETED: "Arquivado",
  EXPORT_FAILED: "Pendente",
  DELETE_FAILED: "Pendente",
};

const PRODUCT_TYPES: Record<string, string> = {
  SITE_INSTITUCIONAL: "Site Institucional",
  PORTAL: "Portal",
  KNOWLEDGE_BASE: "Knowledge Base",
  PORTFOLIO: "Portfolio",
  LIBRARY_BOOKS_MUSIC: "Library/Books/Music",
  PRODUTO_SAAS: "Produto SaaS",
  CUSTOM: "Custom",
};

export function mapProductSummary(dto: ProductSummaryDto): ProductSummary {
  return {
    id: dto.id,
    key: dto.key,
    name: dto.name ?? dto.key,
    type: mapProductType(dto.type),
    status: mapProductStatus(dto.status),
    modules: dto.enabledModuleCount ?? 0,
    last: dto.updatedAt ?? dto.createdAt ?? "—",
    score: "—",
    tenantId: dto.tenantId,
  };
}

export function mapProductSummaries(dtos: ProductSummaryDto[]): ProductSummary[] {
  return dtos.map(mapProductSummary);
}

export function mapProductStatus(status: string): ProductStatus {
  return PRODUCT_STATUS[status] ?? "Pendente";
}

function mapProductType(type: string): string {
  return PRODUCT_TYPES[type] ?? type;
}
