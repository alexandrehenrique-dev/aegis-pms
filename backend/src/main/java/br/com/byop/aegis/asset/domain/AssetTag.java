package br.com.byop.aegis.asset.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "asset_tags")
public class AssetTag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false, length = 120)
    private String name;

    protected AssetTag() {
    }

    public AssetTag(UUID productId, String name) {
        this.productId = Objects.requireNonNull(productId, "productId is required");
        this.name = Objects.requireNonNull(name, "name is required");
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }
}
