package br.com.byop.aegis.pages.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

/**
 * Navbar, footer e redes sociais de um produto (ADR-0013) — configuracao unica
 * 1:1 por produto, nunca uma secao de pagina. {@code createdAt} existe apenas
 * para rastreabilidade interna (Constituicao, Artigo X); o contrato REST
 * ({@code ProductGlobalsResponse}) nunca o expoe.
 */
@Entity
@Table(name = "product_globals", uniqueConstraints = @UniqueConstraint(
        name = "uq_product_globals_product",
        columnNames = {"product_id"}
))
public class ProductGlobals {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "navbar_json", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String navbarJson;

    @Column(name = "footer_json", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String footerJson;

    @Column(name = "social_links_json", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String socialLinksJson;

    @Column(name = "floating_whatsapp_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String floatingWhatsappJson;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ProductGlobals() {
    }

    public ProductGlobals(UUID productId, String navbarJson, String footerJson, String socialLinksJson,
                          String floatingWhatsappJson) {
        this.productId = Objects.requireNonNull(productId, "productId is required");
        this.navbarJson = Objects.requireNonNull(navbarJson, "navbarJson is required");
        this.footerJson = Objects.requireNonNull(footerJson, "footerJson is required");
        this.socialLinksJson = Objects.requireNonNull(socialLinksJson, "socialLinksJson is required");
        this.floatingWhatsappJson = floatingWhatsappJson;
    }

    public void apply(String navbarJson, String footerJson, String socialLinksJson, String floatingWhatsappJson) {
        this.navbarJson = Objects.requireNonNull(navbarJson, "navbarJson is required");
        this.footerJson = Objects.requireNonNull(footerJson, "footerJson is required");
        this.socialLinksJson = Objects.requireNonNull(socialLinksJson, "socialLinksJson is required");
        this.floatingWhatsappJson = floatingWhatsappJson;
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

    public UUID getProductId() {
        return productId;
    }

    public String getNavbarJson() {
        return navbarJson;
    }

    public String getFooterJson() {
        return footerJson;
    }

    public String getSocialLinksJson() {
        return socialLinksJson;
    }

    public String getFloatingWhatsappJson() {
        return floatingWhatsappJson;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
