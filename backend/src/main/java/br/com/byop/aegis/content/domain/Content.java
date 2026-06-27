package br.com.byop.aegis.content.domain;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "contents")
public class Content {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false, length = 240)
    private String title;

    @Column(nullable = false, length = 80)
    private String type;

    @Column(nullable = false, length = 16)
    private String lang;

    @Column(name = "author_subject", nullable = false, length = 160)
    private String authorSubject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContentStatus status;

    @Column(length = 160)
    private String publication;

    @Column(name = "current_version", nullable = false)
    private int currentVersion;

    @Column(length = 480)
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level", length = 20)
    private DifficultyLevel difficultyLevel;

    @Column(name = "body_markdown")
    private String bodyMarkdown;

    @Column(length = 160)
    private String category;

    @Column(length = 160)
    private String topic;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json", columnDefinition = "jsonb")
    private String metadataJson;

    @Column(name = "graph_node_id")
    private UUID graphNodeId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Content() {
    }

    public Content(UUID tenantId, UUID productId, String title, String type, String lang, String authorSubject) {
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId is required");
        this.productId = Objects.requireNonNull(productId, "productId is required");
        this.title = Objects.requireNonNull(title, "title is required");
        this.type = Objects.requireNonNull(type, "type is required");
        this.lang = Objects.requireNonNull(lang, "lang is required");
        this.authorSubject = Objects.requireNonNull(authorSubject, "authorSubject is required");
        this.status = ContentStatus.DRAFT;
        this.currentVersion = 1;
    }

    public void applyEdit(Edit edit) {
        this.title = Objects.requireNonNull(edit.title(), "title is required");
        this.type = Objects.requireNonNull(edit.type(), "type is required");
        this.lang = Objects.requireNonNull(edit.lang(), "lang is required");
        this.bodyMarkdown = edit.bodyMarkdown();
        this.summary = edit.summary();
        this.difficultyLevel = edit.difficultyLevel();
        this.category = edit.category();
        this.topic = edit.topic();
        this.metadataJson = edit.metadataJson();
    }

    public void changeStatus(ContentStatus status) {
        this.status = Objects.requireNonNull(status, "status is required");
    }

    public void bumpVersion() {
        this.currentVersion = this.currentVersion + 1;
    }

    public void markPublication(String publication) {
        this.publication = publication;
    }

    public void linkGraphNode(UUID graphNodeId) {
        this.graphNodeId = graphNodeId;
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

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
    }

    public String getLang() {
        return lang;
    }

    public String getAuthorSubject() {
        return authorSubject;
    }

    public ContentStatus getStatus() {
        return status;
    }

    public String getPublication() {
        return publication;
    }

    public int getCurrentVersion() {
        return currentVersion;
    }

    public String getSummary() {
        return summary;
    }

    public DifficultyLevel getDifficultyLevel() {
        return difficultyLevel;
    }

    public String getBodyMarkdown() {
        return bodyMarkdown;
    }

    public String getCategory() {
        return category;
    }

    public String getTopic() {
        return topic;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public UUID getGraphNodeId() {
        return graphNodeId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Agrupa os campos editaveis de {@link Content} num unico parametro de
     * {@link #applyEdit(Edit)} — evita um metodo com lista longa de parametros
     * posicionais (java:S107) sem tornar a entidade anemica.
     */
    public record Edit(
            String title,
            String type,
            String lang,
            String bodyMarkdown,
            String summary,
            DifficultyLevel difficultyLevel,
            String category,
            String topic,
            String metadataJson
    ) {
    }
}
