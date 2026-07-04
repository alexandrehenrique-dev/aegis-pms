package br.com.byop.aegis.notification.api;

import br.com.byop.aegis.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Porta publica para vincular usuarios recem-criados ao onboarding persistido.
 */
@Slf4j
@Service
public class NotificationOnboardingService {

    private final NotificationService notificationService;

    public NotificationOnboardingService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void assignOnboarding(String userSubject) {
        log.debug("assignOnboarding: userSubject='{}'", userSubject);
        notificationService.assignOnboarding(userSubject);
        log.info("assignOnboarding: onboarding atribuido userSubject='{}'", userSubject);
    }
}
