package br.com.byop.aegis.product.api;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.UUID;

public interface ProductExportStoragePort {

    ProductExportStoredFile storeExport(Path temporaryZip, UUID tokenId, String filename, AssetStorageStrategy strategy);

    InputStream openExport(String storageProvider, String zipPath);

    void deleteExport(String storageProvider, String zipPath);

    InputStream openAsset(String storageProvider, String storageKey);

    void deleteAsset(String storageProvider, String storageKey);

    String filename(String zipPath);
}
