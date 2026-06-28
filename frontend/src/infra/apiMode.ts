/**
 * Verdadeiro quando `VITE_API_MODE=api` — todos os services devem
 * verificar esta constante antes de usar mocks ou `apiClient`.
 *
 * Em modo mock (default), o comportamento é idêntico ao anterior a esta
 * sprint — nenhum mock é removido, apenas bugs são corrigidos.
 *
 * Em modo api, cada service usa `apiClient` (src/shared/services/apiClient.ts)
 * com o `baseURL` apontando para `VITE_API_BASE_URL`.
 */
export const IS_API_MODE = import.meta.env.VITE_API_MODE === 'api';
