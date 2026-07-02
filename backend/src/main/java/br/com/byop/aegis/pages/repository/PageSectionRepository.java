package br.com.byop.aegis.pages.repository;

import br.com.byop.aegis.pages.domain.PageSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de {@link PageSection}, sempre escopado por {@link
 * br.com.byop.aegis.pages.domain.Page} — bloco de conteudo tipado que compoe
 * uma pagina, com posicao propria ({@code order}) dentro dela.
 */
@Repository
public interface PageSectionRepository extends JpaRepository<PageSection, UUID> {

    /**
     * Lista as secoes de uma pagina, ordenadas pela posicao definida em
     * {@code order}.
     *
     * @param pageId identificador da pagina proprietaria
     * @return secoes da pagina, na ordem de exibicao
     */
    List<PageSection> findAllByPageIdOrderByOrderAsc(UUID pageId);

    /**
     * Busca uma secao pelo identificador dentro de uma pagina, garantindo o
     * isolamento por pagina (secao de outra pagina nunca e retornada).
     *
     * @param pageId identificador da pagina proprietaria
     * @param id identificador da secao
     * @return secao encontrada, ou {@link Optional#empty()} quando inexistente na pagina
     */
    Optional<PageSection> findByPageIdAndId(UUID pageId, UUID id);

    /**
     * Verifica se alguma secao (de qualquer pagina, de qualquer produto) tem um
     * bloco {@code contact}/{@code form} referenciando o {@code formId} informado
     * dentro de {@code contentJson} — consulta exposta para o dominio {@code form}
     * decidir se um {@code FormDefinition} pode ser excluido (Secao E da Sprint 23:
     * {@code pages} nunca bloqueia a exclusao por conta propria, so expoe a consulta).
     *
     * @param formId identificador do {@code FormDefinition} referenciado
     * @return {@code true} se pelo menos uma secao referencia o {@code formId}
     */
    @Query(value = "SELECT EXISTS (SELECT 1 FROM page_sections WHERE content_json ->> 'formId' = CAST(:formId AS text))",
            nativeQuery = true)
    boolean existsByFormIdReference(@Param("formId") UUID formId);
}
