/**
 * Wrapper único de HTTP para todo o frontend. Hoje nenhum domínio o usa de
 * fato (todos os services ainda leem de mocks locais — ver Sprint 07 para o
 * toggle mock/real), mas a interface já nasce pronta: quando um domínio
 * trocar de mock para chamada real, troca só a implementação do service,
 * nunca a forma de uso deste cliente.
 */

import { logApiCall } from "./devLog";
import { toast } from "../../core/notifications/toast";

export type ApiError = {
  status: number;
  message: string;
  /** Erros de campo, quando o backend retornar validação (ex.: 422). */
  fieldErrors?: Record<string, string>;
  /** Código de erro de negócio do backend (ex.: "PRODUCT_CONTENT_ACCESS_DENIED"). */
  code?: string;
};

type TokenProvider = () => string | null;

let tokenProvider: TokenProvider = () => null;

/**
 * Chamado pelo AuthProvider para informar de onde vem o token atual.
 * Hoje devolve um token mock; na Sprint 06 (Keycloak real) só a função
 * passada aqui muda — nada no apiClient ou nos services precisa mudar.
 */
export function setAuthTokenProvider(provider: TokenProvider) {
  tokenProvider = provider;
}

/** Exportado (Sprint 23) para montar links diretos fora do `apiClient` — ex.: `href` de download de anexo, que abre em nova aba em vez de passar por `request()`. */
export function resolveBaseUrl(): string {
  return import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api/v1";
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = tokenProvider();
  const headers = new Headers(init.headers);
  headers.set("Content-Type", "application/json");
  if (token) headers.set("Authorization", `Bearer ${token}`);

  logApiCall(init.method ?? "GET", path, init.body);

  let response: Response;
  try {
    response = await fetch(`${resolveBaseUrl()}${path}`, { ...init, headers });
  } catch {
    const networkError: ApiError = { status: 0, message: "Falha de rede ao contatar a API." };
    throw networkError;
  }

  if (!response.ok) {
    let message = `Erro ${response.status} ao chamar ${path}`;
    let fieldErrors: Record<string, string> | undefined;
    let code: string | undefined;
    try {
      const body = await response.json();
      message = body?.message ?? message;
      fieldErrors = body?.fieldErrors;
      code = body?.error;
    } catch {
      /* corpo de erro não é JSON; mantém a mensagem padrão */
    }

    // ADR-0018: SUPER_ADMIN sabe que o produto existe mas não tem
    // ProductAssignment para ele — diferente de um 403 genérico (sem
    // permissão de papel), por isso a mensagem é contextual, não um redirect
    // para a tela de "Sem permissão" genérica.
    if (response.status === 403 && code === "PRODUCT_CONTENT_ACCESS_DENIED") {
      toast.error("Você não tem acesso ao conteúdo deste produto. Solicite atribuição ao administrador.");
    }

    const apiError: ApiError = { status: response.status, message, fieldErrors, code };
    throw apiError;
  }

  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

export const apiClient = {
  get: <T>(path: string) => request<T>(path, { method: "GET" }),
  post: <T>(path: string, body?: unknown) => request<T>(path, { method: "POST", body: body !== undefined ? JSON.stringify(body) : undefined }),
  put: <T>(path: string, body?: unknown) => request<T>(path, { method: "PUT", body: body !== undefined ? JSON.stringify(body) : undefined }),
  delete: <T>(path: string) => request<T>(path, { method: "DELETE" }),
};
