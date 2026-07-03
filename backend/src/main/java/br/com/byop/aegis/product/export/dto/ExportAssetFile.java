package br.com.byop.aegis.product.export.dto;

public record ExportAssetFile(
        String id,
        String name,
        String category,
        String storageProvider,
        String storageKey,
        long sizeBytes
) {
}
