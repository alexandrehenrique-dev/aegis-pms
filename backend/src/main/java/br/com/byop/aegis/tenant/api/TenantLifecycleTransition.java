package br.com.byop.aegis.tenant.api;

/**
 * Transicoes de status de {@code Tenant} que disparam notificacao interna aos
 * usuarios do tenant (Etapa 26, retrofit da etapa 09 — "suspender/reativar um
 * tenant era silencioso"). Só as duas transicoes abaixo notificam; nenhuma
 * outra mudanca de status (ex. para {@code ARCHIVED}) ou ausencia de mudanca
 * publica {@link TenantStatusChangedEvent}.
 */
public enum TenantLifecycleTransition {
    SUSPENDED,
    REACTIVATED
}
