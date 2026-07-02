package br.com.byop.aegis.pages.repository;

import br.com.byop.aegis.pages.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de {@link Event}, sempre escopado por produto — entidade propria
 * de agenda (Secao A.1 da Sprint 23), nunca resolvida como "content type".
 */
@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    /**
     * Lista os eventos de um produto, ordenados pelo instante do evento.
     *
     * @param productId identificador do produto proprietario
     * @return eventos pertencentes ao produto informado
     */
    List<Event> findAllByProductIdOrderByDatetimeAsc(UUID productId);

    /**
     * Busca um evento pelo identificador dentro de um produto, garantindo o
     * isolamento por produto (evento de outro produto nunca e retornado).
     *
     * @param productId identificador do produto proprietario
     * @param id identificador do evento
     * @return evento encontrado, ou {@link Optional#empty()} quando inexistente no produto
     */
    Optional<Event> findByProductIdAndId(UUID productId, UUID id);
}
