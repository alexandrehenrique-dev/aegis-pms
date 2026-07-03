package br.com.byop.aegis.product.export.repository;

import br.com.byop.aegis.product.export.domain.ExportToken;
import br.com.byop.aegis.product.export.domain.ExportTokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de {@link ExportToken}, token publico temporario que autoriza o
 * download de um ZIP de exportacao sem expor caminho fisico de storage.
 */
@Repository
public interface ExportTokenRepository extends JpaRepository<ExportToken, UUID> {

    /**
     * Busca token disponivel ainda valido para download publico.
     *
     * @param id identificador do token publico
     * @param status status exigido
     * @param now instante de referencia para validade
     * @return token encontrado, ou {@link Optional#empty()} quando indisponivel
     */
    Optional<ExportToken> findByIdAndStatusAndExpiresAtAfter(UUID id, ExportTokenStatus status, Instant now);

    /**
     * Lista tokens vencidos que ainda nao foram marcados como expirados.
     *
     * @param now instante de referencia
     * @return tokens vencidos pendentes de limpeza
     */
    @Query("""
            select token from ExportToken token
            where token.expiresAt < :now
              and token.status <> br.com.byop.aegis.product.export.domain.ExportTokenStatus.EXPIRED
            """)
    List<ExportToken> findExpired(@Param("now") Instant now);
}
