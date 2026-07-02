package br.com.byop.aegis.notification.repository;

import br.com.byop.aegis.notification.domain.NotificationPresentationMode;
import br.com.byop.aegis.notification.domain.UserNotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de {@link UserNotificationStatus}, estado de leitura/exibicao de
 * uma notificacao para um unico destinatario.
 */
@Repository
public interface UserNotificationStatusRepository extends JpaRepository<UserNotificationStatus, UUID> {

    /**
     * Lista notificacoes de um usuario autenticado, sempre ordenadas pela
     * notificacao mais recente.
     *
     * @param userSubject subject do usuario autenticado
     * @return status do usuario com suas notificacoes
     */
    List<UserNotificationStatus> findAllByUserSubjectOrderByNotificationCreatedAtDesc(String userSubject);

    /**
     * Busca a notificacao modal pendente mais antiga para um usuario.
     *
     * @param userSubject subject do usuario autenticado
     * @param presentationMode modo de apresentacao modal unico
     * @return status pendente mais antigo, ou {@link Optional#empty()} quando ausente
     */
    Optional<UserNotificationStatus> findFirstByUserSubjectAndNotificationPresentationModeAndAutoShownFalseOrderByNotificationCreatedAtAsc(
            String userSubject,
            NotificationPresentationMode presentationMode
    );

    /**
     * Busca o status de uma notificacao dentro do escopo de um usuario.
     *
     * @param notificationId identificador da notificacao
     * @param userSubject subject do usuario autenticado
     * @return status encontrado, ou {@link Optional#empty()} quando inexistente
     */
    @Query("""
            select status
              from UserNotificationStatus status
             where status.notification.id = :notificationId
               and status.userSubject = :userSubject
            """)
    Optional<UserNotificationStatus> findByNotificationIdAndUserSubject(
            @Param("notificationId") UUID notificationId,
            @Param("userSubject") String userSubject
    );

    /**
     * Verifica se um destinatario ja recebeu uma notificacao.
     *
     * @param notificationId identificador da notificacao
     * @param userSubject subject do usuario destinatario
     * @return {@code true} quando o status ja existe
     */
    @Query("""
            select count(status) > 0
              from UserNotificationStatus status
             where status.notification.id = :notificationId
               and status.userSubject = :userSubject
            """)
    boolean existsByNotificationIdAndUserSubject(
            @Param("notificationId") UUID notificationId,
            @Param("userSubject") String userSubject
    );

    /**
     * Conta os status materializados para uma notificacao.
     *
     * @param notificationId identificador da notificacao
     * @return quantidade de destinatarios materializados
     */
    @Query("""
            select count(status)
              from UserNotificationStatus status
             where status.notification.id = :notificationId
            """)
    long countByNotificationId(@Param("notificationId") UUID notificationId);

    /**
     * Lista subjects ja materializados para uma notificacao.
     *
     * @param notificationId identificador da notificacao
     * @return subjects que ja possuem status
     */
    @Query("""
            select status.userSubject
              from UserNotificationStatus status
             where status.notification.id = :notificationId
            """)
    List<String> findUserSubjectsByNotificationId(@Param("notificationId") UUID notificationId);
}
