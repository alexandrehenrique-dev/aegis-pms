package br.com.byop.aegis.asset.repository;

import br.com.byop.aegis.asset.domain.AssetTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de {@link AssetTag}, catalogo de tags de um produto — nome unico
 * por produto, reaproveitado tanto pela tela de gestao de tags quanto pela
 * edicao de metadados de um {@link br.com.byop.aegis.asset.domain.Asset}.
 */
@Repository
public interface AssetTagRepository extends JpaRepository<AssetTag, UUID> {

    /**
     * Lista todas as tags cadastradas para um produto, em qualquer ordem.
     *
     * @param productId identificador do produto proprietario
     * @return tags pertencentes ao produto informado
     */
    List<AssetTag> findAllByProductId(UUID productId);

    /**
     * Busca uma tag pelo nome dentro de um produto, garantindo o isolamento
     * por produto (tag de outro produto nunca e retornada).
     *
     * @param productId identificador do produto proprietario
     * @param name nome exato da tag
     * @return tag encontrada, ou {@link Optional#empty()} quando inexistente no produto
     */
    Optional<AssetTag> findByProductIdAndName(UUID productId, String name);

    /**
     * Verifica se ja existe uma tag com o mesmo nome no produto — usado para
     * rejeitar criacao duplicada com 409.
     *
     * @param productId identificador do produto proprietario
     * @param name nome exato da tag
     * @return {@code true} se uma tag com este nome ja existe no produto
     */
    boolean existsByProductIdAndName(UUID productId, String name);

    /**
     * Busca varias tags pelo identificador de uma vez — usado ao reconciliar
     * os nomes atuais das tags ja atribuidas a um asset.
     *
     * @param ids identificadores das tags
     * @return tags encontradas, em qualquer ordem
     */
    List<AssetTag> findAllByIdIn(Collection<UUID> ids);
}
