package br.com.byop.aegis.tenant.repository;

import br.com.byop.aegis.tenant.domain.TenantMembership;
import br.com.byop.aegis.tenant.domain.TenantMembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de {@link TenantMembership}, vinculo de autorizacao contextual
 * entre usuario autenticado e tenant.
 */
@Repository
public interface TenantMembershipRepository extends JpaRepository<TenantMembership, UUID> {

    /**
     * Busca a membership de um usuario dentro de um tenant.
     *
     * @param tenantId identificador do tenant
     * @param userSubject subject do usuario no Keycloak
     * @return membership encontrada, ou {@link Optional#empty()} quando inexistente
     */
    Optional<TenantMembership> findByTenantIdAndUserSubject(UUID tenantId, String userSubject);

    /**
     * Lista memberships ativas ou historicas de um usuario.
     *
     * @param userSubject subject do usuario no Keycloak
     * @return memberships vinculadas ao usuario
     */
    List<TenantMembership> findAllByUserSubject(String userSubject);

    /**
     * Verifica se um usuario possui membership ativa em um tenant.
     *
     * @param tenantId identificador do tenant
     * @param userSubject subject do usuario no Keycloak
     * @param status status esperado da membership
     * @return {@code true} quando existe membership com o status informado
     */
    boolean existsByTenantIdAndUserSubjectAndStatus(UUID tenantId, String userSubject, TenantMembershipStatus status);
}
