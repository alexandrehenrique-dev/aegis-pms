package br.com.byop.aegis.form.domain;

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
@Table(name = "form_definitions")
public class FormDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 80)
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FormStatus status;

    @Column(name = "fields_json", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String fieldsJson;

    @Column(name = "delivery_channels_json", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String deliveryChannelsJson;

    @Column(length = 255)
    private String publication;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected FormDefinition() {
    }

    public FormDefinition(Creation creation) {
        this.tenantId = Objects.requireNonNull(creation.tenantId(), "tenantId is required");
        this.productId = Objects.requireNonNull(creation.productId(), "productId is required");
        this.name = Objects.requireNonNull(creation.name(), "name is required");
        this.type = Objects.requireNonNull(creation.type(), "type is required");
        this.fieldsJson = Objects.requireNonNull(creation.fieldsJson(), "fieldsJson is required");
        this.deliveryChannelsJson = Objects.requireNonNull(creation.deliveryChannelsJson(), "deliveryChannelsJson is required");
        this.status = FormStatus.DRAFT;
    }

    public void applyDefinition(Edit edit) {
        this.name = Objects.requireNonNull(edit.name(), "name is required");
        this.type = Objects.requireNonNull(edit.type(), "type is required");
        this.fieldsJson = Objects.requireNonNull(edit.fieldsJson(), "fieldsJson is required");
    }

    public void updateDeliveryChannels(String deliveryChannelsJson) {
        this.deliveryChannelsJson = Objects.requireNonNull(deliveryChannelsJson, "deliveryChannelsJson is required");
    }

    public void publish(String publication) {
        this.status = FormStatus.PUBLISHED;
        this.publication = Objects.requireNonNull(publication, "publication is required");
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

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public FormStatus getStatus() {
        return status;
    }

    public String getFieldsJson() {
        return fieldsJson;
    }

    public String getDeliveryChannelsJson() {
        return deliveryChannelsJson;
    }

    public String getPublication() {
        return publication;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public record Creation(
            UUID tenantId,
            UUID productId,
            String name,
            String type,
            String fieldsJson,
            String deliveryChannelsJson
    ) {
    }

    public record Edit(
            String name,
            String type,
            String fieldsJson
    ) {
    }
}
