package br.com.byop.aegis.notification.api;

import br.com.byop.aegis.notification.service.NotificationService;
import org.springframework.stereotype.Service;

/**
 * Porta publica para vincular usuarios recem-criados ao onboarding persistido.
 */
@Service
public class NotificationOnboardingService {

    private final NotificationService notificationService;

    public NotificationOnboardingService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void assignOnboarding(String userSubject) {
        notificationService.assignOnboarding(userSubject);
    }
}
