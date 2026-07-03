package br.com.byop.aegis.product.export.dto;

import br.com.byop.aegis.product.api.AssetStorageStrategy;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ProductExportData(
        UUID tenantId,
        UUID productId,
        String productKey,
        String productName,
        AssetStorageStrategy assetStorageStrategy,
        String filename,
        Map<String, byte[]> jsonEntries,
        List<ExportAssetFile> assets,
        Map<String, Object> manifest,
        Map<String, Object> entityCounts
) {
}
