package br.com.byop.aegis.audit.domain;

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

/**
 * Evento de auditoria imutavel, gravado exclusivamente por
 * {@code br.com.byop.aegis.audit.api.AuditService#record}. Nunca exposto a
 * escrita via API publica.
 */
@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "actor_subject", nullable = false, length = 160)
    private String actorSubject;

    @Column(nullable = false, length = 120)
    private String action;

    @Column(name = "target_type", length = 120)
    private String targetType;

    @Column(name = "target_id", length = 160)
    private String targetId;

    @Column(name = "target_label", length = 255)
    private String targetLabel;

    @Column(length = 80)
    private String module;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditRisk risk;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "diff_json", columnDefinition = "jsonb")
    private String diffJson;

    @Column(name = "trace_id", length = 80)
    private String traceId;

    @Column(length = 64)
    private String ip;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected AuditEvent() {
    }

    public AuditEvent(Creation creation) {
        this.tenantId = creation.tenantId();
        this.productId = creation.productId();
        this.actorSubject = Objects.requireNonNull(creation.actorSubject(), "actorSubject is required");
        this.action = Objects.requireNonNull(creation.action(), "action is required");
        this.targetType = creation.targetType();
        this.targetId = creation.targetId();
        this.targetLabel = creation.targetLabel();
        this.module = creation.module();
        this.risk = Objects.requireNonNull(creation.risk(), "risk is required");
        this.diffJson = creation.diffJson();
        this.traceId = creation.traceId();
        this.ip = creation.ip();
        this.userAgent = creation.userAgent();
    }

    @PrePersist
    void prePersist() {
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
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

    public String getActorSubject() {
        return actorSubject;
    }

    public String getAction() {
        return action;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getTargetLabel() {
        return targetLabel;
    }

    public String getModule() {
        return module;
    }

    public AuditRisk getRisk() {
        return risk;
    }

    public String getDiffJson() {
        return diffJson;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getIp() {
        return ip;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Dados necessarios para gravar um novo {@link AuditEvent}. Campos
     * opcionais (productId, targetType/targetId/targetLabel, module,
     * diffJson, traceId, ip, userAgent) podem ser {@code null}.
     */
    public record Creation(
            UUID tenantId,
            UUID productId,
            String actorSubject,
            String action,
            String targetType,
            String targetId,
            String targetLabel,
            String module,
            AuditRisk risk,
            String diffJson,
            String traceId,
            String ip,
            String userAgent
    ) {
    }
}
