package br.com.byop.aegis.notification.api;

import br.com.byop.aegis.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Porta publica do modulo notification para alertas originados por feedback.
 */
@Slf4j
@Service
public class FeedbackNotificationService {

    private final NotificationService notificationService;

    public FeedbackNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void notifyCriticalFeedback(CriticalFeedbackNotificationRequest request) {
        log.debug("notifyCriticalFeedback: publicId='{}', category='{}', priority='{}'",
                request.publicId(), request.category(), request.priority());
        notificationService.notifyCriticalFeedback(request);
        log.info("notifyCriticalFeedback: notificacao enviada publicId='{}', tenantId='{}', productId='{}'",
                request.publicId(), request.tenantId(), request.productId());
    }
}
