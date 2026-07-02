package br.com.byop.aegis.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(name = "body_markdown", nullable = false, columnDefinition = "text")
    private String bodyMarkdown;

    @Enumerated(EnumType.STRING)
    @Column(name = "presentation_mode", nullable = false, length = 40)
    private NotificationPresentationMode presentationMode;

    @Column(name = "created_by_subject", nullable = false, length = 160)
    private String createdBySubject;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Notification() {
    }

    public Notification(NotificationType type, String title, String bodyMarkdown,
                        NotificationPresentationMode presentationMode, String createdBySubject) {
        this.type = Objects.requireNonNull(type, "type is required");
        this.title = Objects.requireNonNull(title, "title is required");
        this.bodyMarkdown = Objects.requireNonNull(bodyMarkdown, "bodyMarkdown is required");
        this.presentationMode = Objects.requireNonNull(presentationMode, "presentationMode is required");
        this.createdBySubject = Objects.requireNonNull(createdBySubject, "createdBySubject is required");
    }

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getId() {
        return id;
    }

    public NotificationType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getBodyMarkdown() {
        return bodyMarkdown;
    }

    public NotificationPresentationMode getPresentationMode() {
        return presentationMode;
    }

    public String getCreatedBySubject() {
        return createdBySubject;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
