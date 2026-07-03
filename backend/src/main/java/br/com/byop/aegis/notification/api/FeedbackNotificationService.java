package br.com.byop.aegis.notification.api;

import br.com.byop.aegis.notification.service.NotificationService;
import org.springframework.stereotype.Service;

/**
 * Porta publica do modulo notification para alertas originados por feedback.
 */
@Service
public class FeedbackNotificationService {

    private final NotificationService notificationService;

    public FeedbackNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void notifyCriticalFeedback(CriticalFeedbackNotificationRequest request) {
        notificationService.notifyCriticalFeedback(request);
    }
}
