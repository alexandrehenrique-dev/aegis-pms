package br.com.byop.aegis.asset.repository;

import br.com.byop.aegis.asset.domain.AssetUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio de {@link AssetUsage} — rastreia onde um
 * {@link br.com.byop.aegis.asset.domain.Asset} esta sendo referenciado por
 * outros dominios (ex.: conteudo, SEO), usado para impedir exclusao acidental
 * de um asset em uso.
 */
@Repository
public interface AssetUsageRepository extends JpaRepository<AssetUsage, UUID> {

    /**
     * Lista os usos registrados de um asset.
     *
     * @param assetId identificador do asset
     * @return usos existentes para o asset informado
     */
    List<AssetUsage> findAllByAssetId(UUID assetId);

    /**
     * Verifica se um asset possui pelo menos um uso registrado — usado para
     * exigir confirmacao explicita ({@code force=true}) antes da exclusao.
     *
     * @param assetId identificador do asset
     * @return {@code true} se existe pelo menos um uso registrado
     */
    boolean existsByAssetId(UUID assetId);
}
