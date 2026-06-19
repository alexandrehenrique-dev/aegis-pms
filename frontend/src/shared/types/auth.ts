import type { ProductStatus } from "./screen";

export type AuthScreen = "login" | "forgotPassword" | "forgotPasswordSent" | "resetPassword" | "invite";
export type UserRole = "super_admin" | "tenant_admin" | "product_manager" | "editor" | "viewer";
export type LoginError = "" | "invalid" | "blocked" | "expired" | "server";
export type InviteStatus = "valid" | "expired" | "revoked" | "used" | "activated";

export type AuthUser = { id: string; name: string; email: string; role: UserRole; initials: string };
export type TenantOption = { id: string; name: string; plan: string; productCount: number; lastAccess: string; status: "ativo" | "suspenso" };
export type ProductOption = { id: string; name: string; type: string; status: ProductStatus; modules: number; isFavorite?: boolean; isRecent?: boolean };
export type AuthState = { user: AuthUser; tenant: TenantOption; product: ProductOption };
