package br.com.byop.aegis.tenant.repository;

import br.com.byop.aegis.tenant.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de {@link Tenant}, raiz de isolamento multi-tenant do Aegis.
 * A chave do tenant e unica globalmente.
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    /**
     * Busca um tenant pela chave unica global.
     *
     * @param key chave semantica do tenant
     * @return tenant encontrado, ou {@link Optional#empty()} quando inexistente
     */
    Optional<Tenant> findByKey(String key);

    /**
     * Verifica se ja existe tenant com a chave global informada.
     *
     * @param key chave semantica do tenant
     * @return {@code true} quando a chave ja esta em uso
     */
    boolean existsByKey(String key);
}
