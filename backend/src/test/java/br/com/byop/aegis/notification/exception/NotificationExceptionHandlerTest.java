package br.com.byop.aegis.notification.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationExceptionHandlerTest {

    private final NotificationExceptionHandler handler = new NotificationExceptionHandler();

    @Test
    void shouldMapNotificationErrors() {
        assertThat(handler.handleNotificationNotFound().error()).isEqualTo("NOTIFICATION_NOT_FOUND");
        assertThat(handler.handleNotificationTargetUserNotFound().error()).isEqualTo("NOTIFICATION_TARGET_USER_NOT_FOUND");
        assertThat(handler.handleOnboardingNotificationNotFound().error()).isEqualTo("ONBOARDING_NOTIFICATION_NOT_FOUND");
        assertThat(handler.handleInvalidNotificationTarget().error()).isEqualTo("INVALID_NOTIFICATION_TARGET");
        assertThat(handler.handleInsufficientNotificationRole().error()).isEqualTo("NOTIFICATION_ADMIN_FORBIDDEN");
        assertThat(handler.handleIllegalArgument().error()).isEqualTo("INVALID_NOTIFICATION_REQUEST");
    }
}
