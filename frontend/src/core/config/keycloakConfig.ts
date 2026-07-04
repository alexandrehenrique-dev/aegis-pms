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
  // "api" e' o valor canonico de infra/apiMode.ts (IS_API_MODE) — antes este
  // getter comparava com "real", nunca batendo com VITE_API_MODE=api (valor
  // de producao), o que fazia o painel de contas demo aparecer em producao.
  return import.meta.env.VITE_API_MODE === "api" ? "real" : "mock";
}
