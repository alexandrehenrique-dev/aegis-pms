package br.com.byop.aegis.notification.repository;

import br.com.byop.aegis.core.RepositoryTestSupport;
import br.com.byop.aegis.notification.domain.Notification;
import br.com.byop.aegis.notification.domain.NotificationPresentationMode;
import br.com.byop.aegis.notification.domain.NotificationType;
import br.com.byop.aegis.notification.domain.UserNotificationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserNotificationStatusRepository statusRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldSaveNotificationAndStatus() {
        Notification notification = notificationRepository.saveAndFlush(notification("Aviso", NotificationPresentationMode.MODAL_ONCE));

        UserNotificationStatus status = statusRepository.saveAndFlush(new UserNotificationStatus(notification, "user-1"));

        assertThat(status.getId()).isNotNull();
        assertThat(status.getNotificationId()).isEqualTo(notification.getId());
        assertThat(status.getUserSubject()).isEqualTo("user-1");
        assertThat(status.isAutoShown()).isFalse();
        assertThat(status.isRead()).isFalse();
        assertThat(status.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldRejectDuplicateStatusForSameNotificationAndUser() {
        Notification notification = notificationRepository.saveAndFlush(notification("Duplicada", NotificationPresentationMode.MODAL_ONCE));
        statusRepository.saveAndFlush(new UserNotificationStatus(notification, "user-1"));
        UserNotificationStatus duplicate = new UserNotificationStatus(notification, "user-1");

        assertThatThrownBy(() -> statusRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldListUserNotificationsByNotificationCreatedAtDesc() {
        Notification older = notificationRepository.saveAndFlush(notification("Antiga", NotificationPresentationMode.MODAL_ONCE));
        Notification newer = notificationRepository.saveAndFlush(notification("Nova", NotificationPresentationMode.BELL_ONLY));
        setCreatedAt(older, "2026-07-02T10:00:00Z");
        setCreatedAt(newer, "2026-07-02T11:00:00Z");
        statusRepository.saveAndFlush(new UserNotificationStatus(older, "user-1"));
        statusRepository.saveAndFlush(new UserNotificationStatus(newer, "user-1"));

        assertThat(statusRepository.findAllByUserSubjectOrderByNotificationCreatedAtDesc("user-1"))
                .extracting(status -> status.getNotification().getTitle())
                .containsExactly("Nova", "Antiga");
    }

    @Test
    void shouldFindOldestPendingModalAndIgnoreBellOnly() {
        Notification modal = notificationRepository.saveAndFlush(notification("Modal", NotificationPresentationMode.MODAL_ONCE));
        Notification newerModal = notificationRepository.saveAndFlush(notification("Modal nova", NotificationPresentationMode.MODAL_ONCE));
        Notification bellOnly = notificationRepository.saveAndFlush(notification("Sino", NotificationPresentationMode.BELL_ONLY));
        setCreatedAt(modal, "2026-07-02T10:00:00Z");
        setCreatedAt(newerModal, "2026-07-02T11:00:00Z");
        setCreatedAt(bellOnly, "2026-07-02T12:00:00Z");
        statusRepository.saveAndFlush(new UserNotificationStatus(newerModal, "user-1"));
        statusRepository.saveAndFlush(new UserNotificationStatus(bellOnly, "user-1"));
        statusRepository.saveAndFlush(new UserNotificationStatus(modal, "user-1"));

        assertThat(statusRepository
                .findFirstByUserSubjectAndNotificationPresentationModeAndAutoShownFalseOrderByNotificationCreatedAtAsc(
                        "user-1",
                        NotificationPresentationMode.MODAL_ONCE
                ))
                .get()
                .extracting(status -> status.getNotification().getTitle())
                .isEqualTo("Modal");
    }

    @Test
    void shouldQueryStatusesByNotificationIdAndUserSubject() {
        Notification notification = notificationRepository.saveAndFlush(notification("Aviso", NotificationPresentationMode.MODAL_ONCE));
        statusRepository.saveAndFlush(new UserNotificationStatus(notification, "user-1"));

        assertThat(statusRepository.findByNotificationIdAndUserSubject(notification.getId(), "user-1"))
                .isPresent()
                .get()
                .extracting(UserNotificationStatus::getUserSubject)
                .isEqualTo("user-1");
        assertThat(statusRepository.existsByNotificationIdAndUserSubject(notification.getId(), "user-1")).isTrue();
        assertThat(statusRepository.existsByNotificationIdAndUserSubject(notification.getId(), "missing")).isFalse();
        assertThat(statusRepository.countByNotificationId(notification.getId())).isEqualTo(1);
        assertThat(statusRepository.findUserSubjectsByNotificationId(notification.getId()))
                .containsExactly("user-1");
    }

    @Test
    void shouldFindSeededOnboardingNotification() {
        assertThat(notificationRepository.findFirstByType(NotificationType.ONBOARDING)).isPresent();
    }

    private Notification notification(String title, NotificationPresentationMode presentationMode) {
        return new Notification(NotificationType.GENERAL, title, "<p>body</p>", presentationMode, "admin");
    }

    private void setCreatedAt(Notification notification, String createdAt) {
        OffsetDateTime timestamp = OffsetDateTime.parse(createdAt);
        jdbcTemplate.update(
                "update notifications set created_at = ?, updated_at = ? where id = ?",
                timestamp,
                timestamp,
                notification.getId()
        );
    }
}
