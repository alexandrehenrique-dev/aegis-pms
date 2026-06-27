package br.com.byop.aegis.form.repository;

import br.com.byop.aegis.form.domain.FormDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de {@link FormDefinition}, sempre escopado por produto — guarda
 * definicoes de formulario, campos JSON e configuracao de entrega das respostas.
 */
@Repository
public interface FormDefinitionRepository extends JpaRepository<FormDefinition, UUID> {

    /**
     * Lista as definicoes de formulario de um produto.
     *
     * @param productId identificador do produto proprietario
     * @return formularios pertencentes ao produto informado
     */
    List<FormDefinition> findAllByProductId(UUID productId);

    /**
     * Busca uma definicao de formulario dentro de um produto, garantindo
     * isolamento por produto.
     *
     * @param productId identificador do produto proprietario
     * @param id identificador do formulario
     * @return formulario encontrado, ou {@link Optional#empty()} quando inexistente no produto
     */
    Optional<FormDefinition> findByProductIdAndId(UUID productId, UUID id);
}
