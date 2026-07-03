package br.com.byop.aegis.identity.auth.repository;

import br.com.byop.aegis.identity.auth.domain.AuthActionStatus;
import br.com.byop.aegis.identity.auth.domain.AuthActionToken;
import br.com.byop.aegis.identity.auth.domain.AuthActionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repositorio de {@link AuthActionToken}, token publico temporario usado para
 * ativacao de convite e redefinicao de senha controladas pelo Aegis.
 */
@Repository
public interface AuthActionTokenRepository extends JpaRepository<AuthActionToken, UUID> {

    /**
     * Conta tokens recentes de um e-mail e tipo para rate limit manual.
     *
     * @param userEmail e-mail normalizado do destinatario
     * @param type tipo de acao autenticavel
     * @param createdAtAfter limite inferior da janela de tempo
     * @return quantidade de tokens criados na janela
     */
    long countByUserEmailIgnoreCaseAndTypeAndCreatedAtAfter(String userEmail, AuthActionType type,
                                                            Instant createdAtAfter);

    /**
     * Lista tokens pendentes de um usuario e tipo para invalidacao.
     *
     * @param keycloakId identificador do usuario no Keycloak
     * @param type tipo de token
     * @param status status esperado
     * @return tokens pendentes do usuario
     */
    List<AuthActionToken> findAllByKeycloakIdAndTypeAndStatus(String keycloakId, AuthActionType type,
                                                              AuthActionStatus status);

    /**
     * Lista tokens pendentes expirados para limpeza agendada.
     *
     * @param status status pendente
     * @param now instante de referencia
     * @return tokens expirados ainda nao marcados como expirados
     */
    @Query("""
            select token from AuthActionToken token
            where token.status = :status
              and token.expiresAt < :now
            """)
    List<AuthActionToken> findExpiredPending(@Param("status") AuthActionStatus status, @Param("now") Instant now);
}
