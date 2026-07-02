package br.com.byop.aegis.notification.mapper;

import br.com.byop.aegis.notification.domain.Notification;
import br.com.byop.aegis.notification.domain.NotificationPresentationMode;
import br.com.byop.aegis.notification.domain.NotificationType;
import br.com.byop.aegis.notification.domain.UserNotificationStatus;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationMapperTest {

    private static final UUID NOTIFICATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-07-02T10:00:00Z");
    private static final OffsetDateTime UPDATED_AT = OffsetDateTime.parse("2026-07-02T11:00:00Z");
    private static final OffsetDateTime READ_AT = OffsetDateTime.parse("2026-07-02T12:00:00Z");

    private final NotificationMapper mapper = Mappers.getMapper(NotificationMapper.class);

    @Test
    void shouldMapNotificationToAdminResponse() {
        Notification notification = notification();

        var response = mapper.toResponse(notification);

        assertThat(response.id()).isEqualTo(NOTIFICATION_ID);
        assertThat(response.type()).isEqualTo("FEATURE");
        assertThat(response.presentationMode()).isEqualTo("MODAL_ONCE");
        assertThat(response.createdAt()).isEqualTo(CREATED_AT);
        assertThat(response.updatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    void shouldMapStatusToUserResponse() {
        UserNotificationStatus status = new UserNotificationStatus(notification(), "user-1");
        status.markRead(READ_AT);

        var response = mapper.toWithStatus(status);

        assertThat(response.id()).isEqualTo(NOTIFICATION_ID);
        assertThat(response.type()).isEqualTo("FEATURE");
        assertThat(response.title()).isEqualTo("Nova feature");
        assertThat(response.bodyMarkdown()).isEqualTo("<p>body</p>");
        assertThat(response.presentationMode()).isEqualTo("MODAL_ONCE");
        assertThat(response.createdAt()).isEqualTo(CREATED_AT);
        assertThat(response.read()).isTrue();
        assertThat(response.readAt()).isEqualTo(READ_AT);
    }

    @Test
    void shouldMapNullValuesToNullResponses() {
        assertThat(mapper.toResponse(null)).isNull();
        assertThat(mapper.toWithStatus(null)).isNull();
    }

    @Test
    void shouldMapNotificationWithNullEnums() {
        Notification notification = notification();
        ReflectionTestUtils.setField(notification, "type", null);
        ReflectionTestUtils.setField(notification, "presentationMode", null);

        var response = mapper.toResponse(notification);

        assertThat(response.type()).isNull();
        assertThat(response.presentationMode()).isNull();
    }

    @Test
    void shouldMapStatusWithoutNotificationAsNullNotificationFields() {
        UserNotificationStatus status = new UserNotificationStatus(notification(), "user-1");
        ReflectionTestUtils.setField(status, "notification", null);

        var response = mapper.toWithStatus(status);

        assertThat(response.id()).isNull();
        assertThat(response.type()).isNull();
        assertThat(response.title()).isNull();
        assertThat(response.bodyMarkdown()).isNull();
        assertThat(response.presentationMode()).isNull();
        assertThat(response.createdAt()).isNull();
    }

    private Notification notification() {
        Notification notification = new Notification(
                NotificationType.FEATURE,
                "Nova feature",
                "<p>body</p>",
                NotificationPresentationMode.MODAL_ONCE,
                "admin"
        );
        ReflectionTestUtils.setField(notification, "id", NOTIFICATION_ID);
        ReflectionTestUtils.setField(notification, "createdAt", CREATED_AT);
        ReflectionTestUtils.setField(notification, "updatedAt", UPDATED_AT);
        return notification;
    }
}
