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
@Table(name = "asset_usages")
public class AssetUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Column(name = "used_in_type", nullable = false, length = 80)
    private String usedInType;

    @Column(name = "used_in_ref_id", nullable = false, length = 160)
    private String usedInRefId;

    @Column(name = "used_in_label", nullable = false, length = 255)
    private String usedInLabel;

    protected AssetUsage() {
    }

    public AssetUsage(UUID assetId, String usedInType, String usedInRefId, String usedInLabel) {
        this.assetId = Objects.requireNonNull(assetId, "assetId is required");
        this.usedInType = Objects.requireNonNull(usedInType, "usedInType is required");
        this.usedInRefId = Objects.requireNonNull(usedInRefId, "usedInRefId is required");
        this.usedInLabel = Objects.requireNonNull(usedInLabel, "usedInLabel is required");
    }

    public UUID getId() {
        return id;
    }

    public UUID getAssetId() {
        return assetId;
    }

    public String getUsedInType() {
        return usedInType;
    }

    public String getUsedInRefId() {
        return usedInRefId;
    }

    public String getUsedInLabel() {
        return usedInLabel;
    }
}
