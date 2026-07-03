package br.com.byop.aegis.feedback.repository;

import br.com.byop.aegis.feedback.domain.Feedback;
import br.com.byop.aegis.feedback.domain.FeedbackStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de {@link Feedback}, sempre escopado por tenant e opcionalmente
 * por produto, preservando o identificador publico legivel do reporte.
 */
@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {

    /**
     * Lista todos os feedbacks em ordem cronologica reversa para operacao de plataforma.
     *
     * @return feedbacks ordenados por criacao recente
     */
    List<Feedback> findAllByOrderByCreatedAtDesc();

    /**
     * Lista feedbacks de um tenant em ordem cronologica reversa.
     *
     * @param tenantId identificador do tenant proprietario
     * @return feedbacks do tenant ordenados por criacao recente
     */
    List<Feedback> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    /**
     * Lista feedbacks de um tenant filtrando por status operacional.
     *
     * @param tenantId identificador do tenant proprietario
     * @param status status operacional esperado
     * @return feedbacks do tenant com o status informado
     */
    List<Feedback> findAllByTenantIdAndStatusOrderByCreatedAtDesc(UUID tenantId, FeedbackStatus status);

    /**
     * Busca um feedback pelo identificador publico legivel.
     *
     * @param publicId identificador legivel no formato AGS-####
     * @return feedback encontrado, ou {@link Optional#empty()} quando ausente
     */
    Optional<Feedback> findByPublicId(String publicId);

    /**
     * Obtem o proximo numero da sequence usada para gerar o identificador publico.
     *
     * @return proximo valor sequencial da sequence {@code feedback_public_id_seq}
     */
    @Query(value = "select nextval('feedback_public_id_seq')", nativeQuery = true)
    long nextPublicIdSequence();
}
