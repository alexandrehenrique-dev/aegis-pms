package br.com.byop.aegis.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "user_notification_statuses", uniqueConstraints = @UniqueConstraint(
        name = "uq_user_notification_status",
        columnNames = {"notification_id", "user_subject"}
))
public class UserNotificationStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @Column(name = "user_subject", nullable = false, length = 160)
    private String userSubject;

    @Column(name = "auto_shown", nullable = false)
    private boolean autoShown;

    @Column(name = "read", nullable = false)
    private boolean read;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    @Column(name = "shown_at")
    private OffsetDateTime shownAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected UserNotificationStatus() {
    }

    public UserNotificationStatus(Notification notification, String userSubject) {
        this.notification = Objects.requireNonNull(notification, "notification is required");
        this.userSubject = Objects.requireNonNull(userSubject, "userSubject is required");
        this.autoShown = false;
        this.read = false;
    }

    public void markShown(OffsetDateTime shownAt) {
        if (autoShown) {
            return;
        }
        this.autoShown = true;
        this.shownAt = Objects.requireNonNull(shownAt, "shownAt is required");
    }

    public void markRead(OffsetDateTime readAt) {
        if (read) {
            return;
        }
        this.read = true;
        this.readAt = Objects.requireNonNull(readAt, "readAt is required");
    }

    @PrePersist
    void prePersist() {
        createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getId() {
        return id;
    }

    public Notification getNotification() {
        return notification;
    }

    public UUID getNotificationId() {
        return notification.getId();
    }

    public String getUserSubject() {
        return userSubject;
    }

    public boolean isAutoShown() {
        return autoShown;
    }

    public boolean isRead() {
        return read;
    }

    public OffsetDateTime getReadAt() {
        return readAt;
    }

    public OffsetDateTime getShownAt() {
        return shownAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
