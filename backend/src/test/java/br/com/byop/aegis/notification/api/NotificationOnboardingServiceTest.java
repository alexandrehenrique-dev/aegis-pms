package br.com.byop.aegis.notification.api;

import br.com.byop.aegis.notification.service.NotificationService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationOnboardingServiceTest {

    @Test
    void shouldDelegateOnboardingAssignmentToNotificationService() {
        NotificationService notificationService = mock(NotificationService.class);
        NotificationOnboardingService service = new NotificationOnboardingService(notificationService);

        service.assignOnboarding("user-1");

        verify(notificationService).assignOnboarding("user-1");
    }
}
