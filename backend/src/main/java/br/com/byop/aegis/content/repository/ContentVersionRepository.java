package br.com.byop.aegis.content.repository;

import br.com.byop.aegis.content.domain.ContentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio de {@link ContentVersion} — snapshot gravado a cada transicao de
 * workflow de um {@link br.com.byop.aegis.content.domain.Content}, sempre escopado
 * pelo conteudo de origem.
 */
@Repository
public interface ContentVersionRepository extends JpaRepository<ContentVersion, UUID> {

    /**
     * Lista as versoes de um conteudo em ordem cronologica (mais antiga primeiro).
     *
     * @param contentId identificador do conteudo proprietario
     * @return versoes do conteudo informado, ordenadas por data de criacao ascendente
     */
    List<ContentVersion> findAllByContentIdOrderByCreatedAtAsc(UUID contentId);
}
