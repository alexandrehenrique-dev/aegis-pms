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
@Table(name = "asset_tag_assignments")
public class AssetTagAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Column(name = "asset_tag_id", nullable = false)
    private UUID assetTagId;

    protected AssetTagAssignment() {
    }

    public AssetTagAssignment(UUID assetId, UUID assetTagId) {
        this.assetId = Objects.requireNonNull(assetId, "assetId is required");
        this.assetTagId = Objects.requireNonNull(assetTagId, "assetTagId is required");
    }

    public UUID getId() {
        return id;
    }

    public UUID getAssetId() {
        return assetId;
    }

    public UUID getAssetTagId() {
        return assetTagId;
    }
}
