package br.com.byop.aegis.notification.api;

import br.com.byop.aegis.notification.service.NotificationService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class FeedbackNotificationServiceTest {

    @Test
    void shouldDelegateCriticalFeedbackNotificationToNotificationService() {
        NotificationService notificationService = mock(NotificationService.class);
        FeedbackNotificationService service = new FeedbackNotificationService(notificationService);
        CriticalFeedbackNotificationRequest request = new CriticalFeedbackNotificationRequest(
                "AGS-0043",
                "Bug",
                "crítica",
                "Descricao",
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                null,
                "editor-subject",
                "/dashboard",
                null
        );

        service.notifyCriticalFeedback(request);

        verify(notificationService).notifyCriticalFeedback(request);
    }
}
