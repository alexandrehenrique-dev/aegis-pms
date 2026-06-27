package br.com.byop.aegis.content.repository;

import br.com.byop.aegis.content.domain.Content;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de {@link Content}, sempre escopado por produto — conteudo editorial
 * (artigo, manifesto, post) com workflow de publicacao, versionamento e autoria.
 */
@Repository
public interface ContentRepository extends JpaRepository<Content, UUID> {

    /**
     * Lista o conteudo de um produto.
     *
     * @param productId identificador do produto proprietario
     * @return conteudos pertencentes ao produto informado
     */
    List<Content> findAllByProductId(UUID productId);

    /**
     * Busca um conteudo pelo identificador dentro de um produto, garantindo o
     * isolamento por produto (conteudo de outro produto nunca e retornado).
     *
     * @param productId identificador do produto proprietario
     * @param id identificador do conteudo
     * @return conteudo encontrado, ou {@link Optional#empty()} quando inexistente no produto
     */
    Optional<Content> findByProductIdAndId(UUID productId, UUID id);
}
