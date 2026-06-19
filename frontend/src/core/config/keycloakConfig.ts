/**
 * Configuração do Keycloak lida de variáveis de ambiente — nunca hardcoded,
 * já que a porta mudou uma vez nesta mesma sessão de planejamento (8181 ->
 * 8282). O fluxo de login real via este config é implementado na Sprint 06;
 * por enquanto isto só disponibiliza a configuração.
 */
export type KeycloakConfig = {
  url: string;
  realm: string;
  clientId: string;
};

export function getKeycloakConfig(): KeycloakConfig {
  return {
    url: import.meta.env.VITE_KEYCLOAK_URL || "http://localhost:8282",
    realm: import.meta.env.VITE_KEYCLOAK_REALM || "aegis",
    clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || "aegis-web",
  };
}

export function getApiMode(): "mock" | "real" {
  return import.meta.env.VITE_API_MODE === "real" ? "real" : "mock";
}
