package br.com.byop.aegis.settings.repository;

import br.com.byop.aegis.settings.domain.ProductSecuritySettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repositorio de {@link ProductSecuritySettings}, escopado por produto — as
 * configuracoes sensiveis de um produto nunca sao compartilhadas com outro.
 */
@Repository
public interface ProductSecuritySettingsRepository extends JpaRepository<ProductSecuritySettings, UUID> {
}
