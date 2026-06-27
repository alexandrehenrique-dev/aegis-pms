package br.com.byop.aegis.submission.repository;

import br.com.byop.aegis.submission.domain.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de {@link Submission}, escopado transitivamente por produto via
 * {@code formId}; guarda respostas recebidas e o JSON completo de answers.
 */
@Repository
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    /**
     * Lista submissions de um formulario em ordem cronologica decrescente.
     *
     * @param formId identificador do formulario publicado
     * @return submissions pertencentes ao formulario informado
     */
    List<Submission> findAllByFormIdOrderByDateDesc(UUID formId);

    /**
     * Lista submissions de varios formularios em ordem cronologica decrescente,
     * usado para a visao agregada de submissions do produto.
     *
     * @param formIds identificadores dos formularios do produto
     * @return submissions pertencentes aos formularios informados
     */
    List<Submission> findAllByFormIdInOrderByDateDesc(List<UUID> formIds);

    /**
     * Conta submissions de um formulario, usado no resumo de formularios.
     *
     * @param formId identificador do formulario
     * @return quantidade de submissions vinculadas ao formulario
     */
    long countByFormId(UUID formId);

    /**
     * Busca a submission mais recente de um formulario.
     *
     * @param formId identificador do formulario
     * @return submission mais recente, ou {@link Optional#empty()} quando nao houver respostas
     */
    Optional<Submission> findFirstByFormIdOrderByDateDesc(UUID formId);

    /**
     * Busca uma submission dentro de um formulario, garantindo o isolamento
     * transitivo por formulario/produto.
     *
     * @param formId identificador do formulario
     * @param id identificador da submission
     * @return submission encontrada, ou {@link Optional#empty()} quando inexistente no formulario
     */
    Optional<Submission> findByFormIdAndId(UUID formId, UUID id);
}
