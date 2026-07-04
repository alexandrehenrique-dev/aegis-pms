export function logApiCall(method: string, path: string, payload?: unknown) {
  if (!import.meta.env.PROD) {
    console.log(`[mock→backend] ${method} ${path}`, payload ?? "");
  }
}
