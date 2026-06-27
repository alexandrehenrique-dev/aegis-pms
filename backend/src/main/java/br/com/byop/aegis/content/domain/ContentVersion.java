package br.com.byop.aegis.content.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "content_versions")
public class ContentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "content_id", nullable = false)
    private UUID contentId;

    @Column(name = "version_label", nullable = false, length = 20)
    private String versionLabel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snapshot_json", nullable = false, columnDefinition = "jsonb")
    private String snapshotJson;

    @Column(name = "created_by_subject", nullable = false, length = 160)
    private String createdBySubject;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected ContentVersion() {
    }

    public ContentVersion(UUID contentId, String versionLabel, String snapshotJson, String createdBySubject) {
        this.contentId = Objects.requireNonNull(contentId, "contentId is required");
        this.versionLabel = Objects.requireNonNull(versionLabel, "versionLabel is required");
        this.snapshotJson = Objects.requireNonNull(snapshotJson, "snapshotJson is required");
        this.createdBySubject = Objects.requireNonNull(createdBySubject, "createdBySubject is required");
    }

    @PrePersist
    void prePersist() {
        createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getId() {
        return id;
    }

    public UUID getContentId() {
        return contentId;
    }

    public String getVersionLabel() {
        return versionLabel;
    }

    public String getSnapshotJson() {
        return snapshotJson;
    }

    public String getCreatedBySubject() {
        return createdBySubject;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
