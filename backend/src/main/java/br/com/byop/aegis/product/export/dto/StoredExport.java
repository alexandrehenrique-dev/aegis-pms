package br.com.byop.aegis.product.export.dto;

import java.nio.file.Path;

public record StoredExport(
        String storageProvider,
        String zipPath,
        String filename,
        long sizeBytes,
        Path localPath
) {
}
