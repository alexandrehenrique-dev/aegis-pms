package br.com.byop.aegis.tenant.api;

import java.util.UUID;

/**
 * Publicado por {@code TenantService.updateTenant} quando o status do tenant
 * transiciona entre ativo e suspenso, em qualquer direcao (Etapa 26). Permite
 * que o modulo {@code notification} crie a notificacao interna correspondente
 * sem que {@code tenant} precise depender de {@code notification} — {@code
 * notification} ja depende de {@code tenant.api} (ver {@link
 * TenantUserAccessService}), e uma chamada direta no sentido contrario criaria
 * um ciclo entre os dois modulos.
 */
public record TenantStatusChangedEvent(
        UUID tenantId,
        TenantLifecycleTransition transition,
        String actorSubject
) {
}
