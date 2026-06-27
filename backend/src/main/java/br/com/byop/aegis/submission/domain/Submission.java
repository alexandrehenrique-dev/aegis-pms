package br.com.byop.aegis.submission.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "form_submissions")
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "form_id", nullable = false)
    private UUID formId;

    @Column(nullable = false)
    private OffsetDateTime date;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 120)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private SubmissionStatus status;

    @Column(name = "owner_subject", length = 160)
    private String ownerSubject;

    @Column
    private Integer score;

    @Column(name = "answers_json", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String answersJson;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Submission() {
    }

    public Submission(Creation creation) {
        this.formId = Objects.requireNonNull(creation.formId(), "formId is required");
        this.date = Objects.requireNonNull(creation.date(), "date is required");
        this.name = Objects.requireNonNull(creation.name(), "name is required");
        this.email = Objects.requireNonNull(creation.email(), "email is required");
        this.source = Objects.requireNonNull(creation.source(), "source is required");
        this.status = Objects.requireNonNull(creation.status(), "status is required");
        this.ownerSubject = creation.ownerSubject();
        this.score = creation.score();
        this.answersJson = Objects.requireNonNull(creation.answersJson(), "answersJson is required");
    }

    @PrePersist
    void prePersist() {
        createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getId() {
        return id;
    }

    public UUID getFormId() {
        return formId;
    }

    public OffsetDateTime getDate() {
        return date;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getSource() {
        return source;
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public String getOwnerSubject() {
        return ownerSubject;
    }

    public Integer getScore() {
        return score;
    }

    public String getAnswersJson() {
        return answersJson;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public record Creation(
            UUID formId,
            OffsetDateTime date,
            String name,
            String email,
            String source,
            SubmissionStatus status,
            String ownerSubject,
            Integer score,
            String answersJson
    ) {
    }
}
