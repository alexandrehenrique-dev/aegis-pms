package br.com.byop.aegis.asset.dto;

public record AssetUsageSummary(
        String usedInType,
        String usedInRefId,
        String usedInLabel
) {
}
