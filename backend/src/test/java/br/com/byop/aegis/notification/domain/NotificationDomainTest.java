package br.com.byop.aegis.notification.domain;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationDomainTest {

    private static final OffsetDateTime READ_AT = OffsetDateTime.parse("2026-07-02T10:00:00Z");
    private static final OffsetDateTime SHOWN_AT = OffsetDateTime.parse("2026-07-02T11:00:00Z");

    @Test
    void shouldUpdateNotificationTimestampOnUpdate() {
        Notification notification = notification();
        notification.prePersist();
        OffsetDateTime createdAt = notification.getCreatedAt();

        notification.preUpdate();

        assertThat(notification.getCreatedAt()).isEqualTo(createdAt);
        assertThat(notification.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldExposeShownAtWhenNotificationWasShown() {
        UserNotificationStatus status = new UserNotificationStatus(notification(), "user-1");
        status.markShown(SHOWN_AT);

        assertThat(status.getShownAt()).isEqualTo(SHOWN_AT);
    }

    @Test
    void shouldKeepReadAndShownTimestampsOnRepeatedMarks() {
        UserNotificationStatus status = new UserNotificationStatus(notification(), "user-1");
        status.markRead(READ_AT);
        status.markShown(SHOWN_AT);

        status.markRead(READ_AT.plusHours(1));
        status.markShown(SHOWN_AT.plusHours(1));

        assertThat(status.getReadAt()).isEqualTo(READ_AT);
        assertThat(status.getShownAt()).isEqualTo(SHOWN_AT);
    }

    private Notification notification() {
        return new Notification(
                NotificationType.GENERAL,
                "Aviso",
                "<p>body</p>",
                NotificationPresentationMode.MODAL_ONCE,
                "admin"
        );
    }
}
