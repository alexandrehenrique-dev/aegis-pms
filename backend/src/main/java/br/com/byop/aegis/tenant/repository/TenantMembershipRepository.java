package br.com.byop.aegis.tenant.repository;

import br.com.byop.aegis.tenant.domain.TenantMembership;
import br.com.byop.aegis.tenant.domain.TenantMembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Lista memberships vinculadas a um tenant.
     *
     * @param tenantId identificador do tenant
     * @return memberships do tenant informado
     */
    List<TenantMembership> findAllByTenantId(UUID tenantId);

    /**
     * Busca memberships por subject e status.
     *
     * @param userSubject subject do usuario no Keycloak
     * @param status status esperado da membership
     * @return memberships do usuario com o status informado
     */
    List<TenantMembership> findAllByUserSubjectAndStatus(String userSubject, TenantMembershipStatus status);

    /**
     * Conta memberships de um tenant por papeis e status.
     *
     * @param tenantId identificador do tenant
     * @param roles papeis considerados administrativos
     * @param status status esperado da membership
     * @return quantidade de memberships encontradas
     */
    long countByTenantIdAndRoleInAndStatus(UUID tenantId, List<String> roles, TenantMembershipStatus status);

    /**
     * Verifica se existe membership com o mesmo tenant e e-mail normalizado no subject.
     *
     * @param tenantId identificador do tenant
     * @param userSubject subject do usuario no Keycloak
     * @return {@code true} quando ja existe membership para o usuario no tenant
     */
    boolean existsByTenantIdAndUserSubject(UUID tenantId, String userSubject);

    /**
     * Lista subjects distintos com ao menos uma membership ativa.
     *
     * @param status status ativo esperado
     * @return subjects distintos de usuarios ativos em algum tenant
     */
    @Query("""
            select distinct membership.userSubject
              from TenantMembership membership
             where membership.status = :status
            """)
    List<String> findDistinctUserSubjectsByStatus(@Param("status") TenantMembershipStatus status);

    /**
     * Lista subjects distintos com membership ativa em um tenant.
     *
     * @param tenantId identificador do tenant
     * @param status status ativo esperado
     * @return subjects distintos de usuarios ativos no tenant informado
     */
    @Query("""
            select distinct membership.userSubject
              from TenantMembership membership
             where membership.tenant.id = :tenantId
               and membership.status = :status
            """)
    List<String> findDistinctUserSubjectsByTenantIdAndStatus(
            @Param("tenantId") UUID tenantId,
            @Param("status") TenantMembershipStatus status
    );

    /**
     * Lista subjects distintos com membership ativa em um papel especifico.
     *
     * @param role papel canonico da membership
     * @param status status ativo esperado
     * @return subjects distintos com o papel e status informados
     */
    @Query("""
            select distinct membership.userSubject
              from TenantMembership membership
             where membership.role = :role
               and membership.status = :status
            """)
    List<String> findDistinctUserSubjectsByRoleAndStatus(
            @Param("role") String role,
            @Param("status") TenantMembershipStatus status
    );
}
