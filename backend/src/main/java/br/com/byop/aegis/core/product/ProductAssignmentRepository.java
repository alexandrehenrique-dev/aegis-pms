package br.com.byop.aegis.core.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de {@link ProductAssignment}, vinculo explicito de acesso de um
 * usuario a um produto.
 */
@Repository
public interface ProductAssignmentRepository extends JpaRepository<ProductAssignment, UUID> {

    /**
     * Busca a atribuicao de um usuario dentro de um produto.
     *
     * @param productId identificador do produto
     * @param userSubject subject do usuario no Keycloak
     * @return atribuicao encontrada, ou {@link Optional#empty()} quando inexistente
     */
    Optional<ProductAssignment> findByProductIdAndUserSubject(UUID productId, String userSubject);

    /**
     * Lista atribuicoes de produto para um usuario e status.
     *
     * @param userSubject subject do usuario no Keycloak
     * @param status status esperado da atribuicao
     * @return atribuicoes do usuario com o status informado
     */
    List<ProductAssignment> findAllByUserSubjectAndStatus(String userSubject, ProductAssignmentStatus status);

    /**
     * Verifica se um usuario possui atribuicao com status especifico em um produto.
     *
     * @param productId identificador do produto
     * @param userSubject subject do usuario no Keycloak
     * @param status status esperado da atribuicao
     * @return {@code true} quando a atribuicao existe com o status informado
     */
    boolean existsByProductIdAndUserSubjectAndStatus(UUID productId, String userSubject, ProductAssignmentStatus status);
}
