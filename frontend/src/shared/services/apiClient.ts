/** Wrapper único de HTTP para todo o frontend — cada service decide, via `IS_API_MODE`, se lê de mocks locais ou chama a API real através deste cliente. */

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
type RefreshHandler = () => Promise<boolean>;
type ProgressHandler = (percent: number) => void;

let tokenProvider: TokenProvider = () => null;
let refreshHandler: RefreshHandler = async () => false;

/**
 * Chamado pelo AuthProvider para informar de onde vem o token atual.
 * Hoje devolve um token mock; na Sprint 06 (Keycloak real) só a função
 * passada aqui muda — nada no apiClient ou nos services precisa mudar.
 */
export function setAuthTokenProvider(provider: TokenProvider) {
  tokenProvider = provider;
}

/**
 * Chamado pelo AuthProvider (Sprint de Integração 01) para tentar renovar o
 * token quando uma chamada volta 401. Deve devolver `true` se o refresh deu
 * certo (o `tokenProvider` já vai ler o novo token na retentativa) e `false`
 * caso o refresh também tenha falhado — nesse caso o AuthProvider já cuidou
 * de limpar a sessão e redirecionar para /login.
 */
export function setRefreshHandler(handler: RefreshHandler) {
  refreshHandler = handler;
}

/** Exportado (Sprint 23) para montar links diretos fora do `apiClient` — ex.: `href` de download de anexo, que abre em nova aba em vez de passar por `request()`. */
export function resolveBaseUrl(): string {
  return import.meta.env.VITE_API_BASE_URL || "/api/v1";
}

async function request<T>(path: string, init: RequestInit = {}, isRetry = false): Promise<T> {
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

  // Renovação automática de token (Sprint de Integração 01, Seção E): uma
  // única retentativa por chamada, nunca para /auth/refresh em si (evita
  // loop infinito caso o próprio refresh volte 401).
  if (response.status === 401 && !isRetry && path !== "/auth/refresh") {
    const refreshed = await refreshHandler();
    if (refreshed) return request<T>(path, init, true);
  }

  if (!response.ok) {
    let message = `Erro ${response.status} ao chamar ${path}`;
    let fieldErrors: Record<string, string> | undefined;
    let code: string | undefined;
    try {
      const body = await response.json();
      message = body?.message ?? message;
      fieldErrors = body?.fieldErrors;
      // AuthController devolve `{ code }` (login/refresh/logout); os fluxos
      // de convite/reset devolvem `{ error, message }` — aceita os dois.
      code = body?.code ?? body?.error;
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

/**
 * Upload multipart — não passa por `request()` porque este fixa
 * `Content-Type: application/json` em todo request (Sprint de Integração 02,
 * Seção E). Nunca setar `Content-Type` manualmente aqui: o browser injeta o
 * boundary correto do `multipart/form-data` sozinho a partir do `FormData`.
 */
async function upload<T>(path: string, formData: FormData): Promise<T> {
  const token = tokenProvider();
  const headers = new Headers();
  if (token) headers.set("Authorization", `Bearer ${token}`);

  logApiCall("POST", path, `multipart (${formData.has("file") ? "1 arquivo" : "sem arquivo"})`);

  let response: Response;
  try {
    response = await fetch(`${resolveBaseUrl()}${path}`, { method: "POST", headers, body: formData });
  } catch {
    const networkError: ApiError = { status: 0, message: "Falha de rede ao contatar a API." };
    throw networkError;
  }

  if (!response.ok) {
    let message = `Erro ${response.status} ao enviar arquivo para ${path}`;
    let code: string | undefined;
    try {
      const body = await response.json();
      message = body?.message ?? message;
      code = body?.code ?? body?.error;
    } catch {
      /* corpo de erro não é JSON; mantém a mensagem padrão */
    }
    const apiError: ApiError = { status: response.status, message, code };
    throw apiError;
  }

  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

async function getBlob(path: string, isRetry = false): Promise<Blob> {
  const token = tokenProvider();
  const headers = new Headers();
  if (token) headers.set("Authorization", `Bearer ${token}`);

  logApiCall("GET", path);

  let response: Response;
  try {
    response = await fetch(`${resolveBaseUrl()}${path}`, { method: "GET", headers });
  } catch {
    const networkError: ApiError = { status: 0, message: "Falha de rede ao baixar arquivo da API." };
    throw networkError;
  }

  if (response.status === 401 && !isRetry) {
    const refreshed = await refreshHandler();
    if (refreshed) return getBlob(path, true);
  }

  if (!response.ok) {
    const apiError: ApiError = { status: response.status, message: `Erro ${response.status} ao baixar arquivo de ${path}` };
    throw apiError;
  }

  return response.blob();
}

function uploadWithProgress<T>(path: string, formData: FormData, onProgress?: ProgressHandler): Promise<T> {
  const token = tokenProvider();
  logApiCall("POST", path, `multipart (${formData.has("file") ? "1 arquivo" : "sem arquivo"})`);

  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open("POST", `${resolveBaseUrl()}${path}`);
    if (token) xhr.setRequestHeader("Authorization", `Bearer ${token}`);

    xhr.upload.onprogress = (event) => {
      if (!event.lengthComputable) return;
      onProgress?.(Math.min(99, Math.round((event.loaded / event.total) * 100)));
    };

    xhr.onload = () => {
      if (xhr.status < 200 || xhr.status >= 300) {
        let message = `Erro ${xhr.status} ao enviar arquivo para ${path}`;
        let code: string | undefined;
        try {
          const body = JSON.parse(xhr.responseText);
          message = body?.message ?? message;
          code = body?.code ?? body?.error;
        } catch {
          /* corpo de erro não é JSON; mantém a mensagem padrão */
        }
        reject({ status: xhr.status, message, code } satisfies ApiError);
        return;
      }

      onProgress?.(100);
      if (xhr.status === 204 || !xhr.responseText) {
        resolve(undefined as T);
        return;
      }
      resolve(JSON.parse(xhr.responseText) as T);
    };

    xhr.onerror = () => reject({ status: 0, message: "Falha de rede ao enviar arquivo para a API." } satisfies ApiError);
    xhr.send(formData);
  });
}

export const apiClient = {
  get: <T>(path: string) => request<T>(path, { method: "GET" }),
  post: <T>(path: string, body?: unknown) => request<T>(path, { method: "POST", body: body !== undefined ? JSON.stringify(body) : undefined }),
  put: <T>(path: string, body?: unknown) => request<T>(path, { method: "PUT", body: body !== undefined ? JSON.stringify(body) : undefined }),
  delete: <T>(path: string, body?: unknown) => request<T>(path, { method: "DELETE", body: body !== undefined ? JSON.stringify(body) : undefined }),
  upload,
  uploadWithProgress,
  getBlob,
};
