package br.com.byop.aegis.settings.repository;

import br.com.byop.aegis.settings.domain.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de {@link RolePermission}, sempre escopado por tenant — a
 * matriz de permissoes de um tenant nunca e visivel nem afetada pela matriz
 * de outro tenant (ADR-0019).
 */
@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, UUID> {

    /**
     * Lista todas as permissoes configuradas para um tenant, de todos os
     * papeis — usada para montar a matriz completa (roles/permission-matrix).
     *
     * @param tenantId tenant ao qual as permissoes devem pertencer
     * @return permissoes do tenant, em qualquer ordem
     */
    List<RolePermission> findAllByTenantId(UUID tenantId);

    /**
     * Lista as permissoes de um unico papel dentro de um tenant — usada pelo
     * preview (somente leitura) de um papel especifico.
     *
     * @param tenantId tenant ao qual as permissoes devem pertencer
     * @param role papel canonico (ADR-0014), ex.: {@code "TENANT_ADMIN"}
     * @return permissoes do papel informado dentro do tenant
     */
    List<RolePermission> findAllByTenantIdAndRole(UUID tenantId, String role);

    /**
     * Busca a permissao de uma chave especifica para um papel de um tenant —
     * usada para resolver upsert (atualizar se existir, criar se nao existir)
     * ao salvar a matriz.
     *
     * @param tenantId tenant ao qual a permissao deve pertencer
     * @param role papel canonico (ADR-0014)
     * @param permissionKey chave de permissao (mesma string usada pelo frontend)
     * @return a permissao encontrada, ou {@link Optional#empty()} se nao existir
     */
    Optional<RolePermission> findByTenantIdAndRoleAndPermissionKey(UUID tenantId, String role, String permissionKey);

    /**
     * Remove todas as permissoes configuradas para um tenant — usado por
     * {@code restore-defaults} antes de regravar o catalogo de fabrica.
     *
     * @param tenantId tenant cujas permissoes devem ser removidas
     */
    void deleteAllByTenantId(UUID tenantId);
}
