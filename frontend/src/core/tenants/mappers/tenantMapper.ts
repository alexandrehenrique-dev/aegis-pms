import type { TenantOption } from "../../../shared/types";

export type TenantSummaryDto = {
  id: string;
  key: string;
  name: string;
  status: string;
  plan?: string | null;
  createdAt?: string;
  updatedAt?: string;
};

export function mapTenantSummary(dto: TenantSummaryDto): TenantOption {
  return {
    id: dto.id,
    name: dto.name,
    plan: dto.plan ?? "Starter",
    productCount: 0,
    lastAccess: "—",
    // Backend retorna "ativo"/"suspenso" (TenantMapper.toContractStatus) — aceita
    // também os valores ingleses para compatibilidade futura.
    status: (dto.status === "ACTIVE" || dto.status === "ativo") ? "ativo" : "suspenso",
  };
}

export function mapTenantSummaries(dtos: TenantSummaryDto[]): TenantOption[] {
  return dtos.map(mapTenantSummary);
}
