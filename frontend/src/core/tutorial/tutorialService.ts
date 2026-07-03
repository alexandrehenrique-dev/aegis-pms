import { IS_API_MODE } from "../../infra/apiMode";
import { apiClient } from "../../shared/services/apiClient";
import { logApiCall } from "../../shared/services/devLog";

const LS_KEY = "aegis:tutorial:completed";

/**
 * Garantia "nunca mostrar novamente" (Sprint 22): `localStorage` evita o
 * flash de reabrir na mesma sessão/dispositivo; a flag no backend
 * (`GET /me` → `tutorialCompleted`, ver `AuthContext.tsx`) reconstrói esse
 * cache local em outro dispositivo ou após limpar o storage.
 */
export const tutorialService = {
  isCompleted(): boolean {
    try {
      return localStorage.getItem(LS_KEY) === "true";
    } catch {
      return false;
    }
  },

  markCompletedLocally(): void {
    try {
      localStorage.setItem(LS_KEY, "true");
    } catch {
      /* noop — storage indisponível não deve travar o fluxo */
    }
  },

  async markComplete(): Promise<void> {
    tutorialService.markCompletedLocally();
    if (IS_API_MODE) {
      await apiClient.post("/tutorial/complete");
      return;
    }
    logApiCall("POST", "/api/v1/tutorial/complete");
  },
};
