/**
 * Wrapper único de HTTP para todo o frontend. Hoje nenhum domínio o usa de
 * fato (todos os services ainda leem de mocks locais — ver Sprint 07 para o
 * toggle mock/real), mas a interface já nasce pronta: quando um domínio
 * trocar de mock para chamada real, troca só a implementação do service,
 * nunca a forma de uso deste cliente.
 */

import { logApiCall } from "./devLog";

export type ApiError = {
  status: number;
  message: string;
  /** Erros de campo, quando o backend retornar validação (ex.: 422). */
  fieldErrors?: Record<string, string>;
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

function resolveBaseUrl(): string {
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
    try {
      const body = await response.json();
      message = body?.message ?? message;
      fieldErrors = body?.fieldErrors;
    } catch {
      /* corpo de erro não é JSON; mantém a mensagem padrão */
    }
    const apiError: ApiError = { status: response.status, message, fieldErrors };
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
