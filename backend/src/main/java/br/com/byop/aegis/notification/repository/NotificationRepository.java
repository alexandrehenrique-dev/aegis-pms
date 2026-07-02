package br.com.byop.aegis.notification.repository;

import br.com.byop.aegis.notification.domain.Notification;
import br.com.byop.aegis.notification.domain.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de {@link Notification}, notificacao global com fan-out
 * materializado por destinatario em {@code UserNotificationStatus}.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Lista todas as notificacoes administrativas ordenadas por criacao recente.
     *
     * @return notificacoes ordenadas por {@code createdAt} decrescente
     */
    List<Notification> findAllByOrderByCreatedAtDesc();

    /**
     * Busca a notificacao tecnica de onboarding.
     *
     * @param type tipo da notificacao esperada
     * @return notificacao encontrada, ou {@link Optional#empty()} quando ausente
     */
    Optional<Notification> findFirstByType(NotificationType type);
}
