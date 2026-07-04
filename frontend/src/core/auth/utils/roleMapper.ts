import type { UserRole } from "../../../shared/types";

const KNOWN_ROLES: readonly UserRole[] = ["super_admin", "tenant_admin", "product_manager", "editor", "viewer"];

/**
 * O backend (`MeResponseMapper`) já resolve o papel Keycloak (`AEGIS_SUPER_ADMIN`
 * etc.) para o mesmo valor público em minúsculo que o frontend usa — isto só
 * valida contra a lista conhecida e cai para `viewer` (o papel de menor
 * privilégio) se `/me` devolver algo inesperado.
 */
export function toUserRole(role: string): UserRole {
  return (KNOWN_ROLES as readonly string[]).includes(role) ? (role as UserRole) : "viewer";
}
