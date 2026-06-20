export function logApiCall(method: string, path: string, payload?: unknown) {
  console.log(`[mock→backend] ${method} ${path}`, payload ?? "");
}
