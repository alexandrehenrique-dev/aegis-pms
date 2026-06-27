package br.com.byop.aegis.asset.repository;

import br.com.byop.aegis.asset.domain.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de {@link Asset}, sempre escopado por produto — biblioteca de
 * midia (imagem, pdf, audio, video, documento) com metadados editaveis e
 * estrategia de storage herdada do produto no momento do upload.
 */
@Repository
public interface AssetRepository extends JpaRepository<Asset, UUID> {

    /**
     * Lista os assets de um produto.
     *
     * @param productId identificador do produto proprietario
     * @return assets pertencentes ao produto informado
     */
    List<Asset> findAllByProductId(UUID productId);

    /**
     * Busca um asset pelo identificador dentro de um produto, garantindo o
     * isolamento por produto (asset de outro produto nunca e retornado).
     *
     * @param productId identificador do produto proprietario
     * @param id identificador do asset
     * @return asset encontrado, ou {@link Optional#empty()} quando inexistente no produto
     */
    Optional<Asset> findByProductIdAndId(UUID productId, UUID id);
}
