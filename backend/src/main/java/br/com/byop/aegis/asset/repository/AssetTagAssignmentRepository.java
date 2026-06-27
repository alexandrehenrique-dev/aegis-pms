package br.com.byop.aegis.asset.repository;

import br.com.byop.aegis.asset.domain.AssetTagAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio de {@link AssetTagAssignment}, tabela de juncao entre
 * {@link br.com.byop.aegis.asset.domain.Asset} e
 * {@link br.com.byop.aegis.asset.domain.AssetTag}.
 */
@Repository
public interface AssetTagAssignmentRepository extends JpaRepository<AssetTagAssignment, UUID> {

    /**
     * Lista todas as atribuicoes de tag de um asset especifico.
     *
     * @param assetId identificador do asset
     * @return atribuicoes existentes para o asset informado
     */
    List<AssetTagAssignment> findAllByAssetId(UUID assetId);

    /**
     * Verifica se um asset ja possui uma tag especifica atribuida — usado
     * para evitar atribuicao duplicada.
     *
     * @param assetId identificador do asset
     * @param assetTagId identificador da tag
     * @return {@code true} se a atribuicao ja existe
     */
    boolean existsByAssetIdAndAssetTagId(UUID assetId, UUID assetTagId);

    /**
     * Remove todas as atribuicoes de um asset para uma tag especifica —
     * usado ao reconciliar a lista de tags na edicao de metadados.
     *
     * @param assetId identificador do asset
     * @param assetTagId identificador da tag
     */
    void deleteByAssetIdAndAssetTagId(UUID assetId, UUID assetTagId);
}
