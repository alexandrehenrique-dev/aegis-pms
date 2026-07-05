package br.com.byop.aegis.identity.api;

/**
 * Evento publicado por {@code AuthActivationService} imediatamente após um
 * usuário convidado ativar sua conta (define senha + habilita Keycloak).
 * <p>
 * Listeners no módulo {@code product} usam o evento para transicionar
 * {@code ProductAssignment} de {@code INVITED} para {@code ASSIGNED}; o
 * módulo {@code tenant} ativa a {@code TenantMembership} correspondente.
 *
 * @param keycloakId subject Keycloak do usuário que acabou de ativar
 */
public record IdentityUserInviteActivatedEvent(String keycloakId) {
}
