package br.com.byop.aegis.product.api;

import java.nio.file.Path;

public record ProductExportStoredFile(
        String storageProvider,
        String zipPath,
        String filename,
        long sizeBytes,
        Path localPath
) {
}
