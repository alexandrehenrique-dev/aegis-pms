package br.com.byop.aegis.product.export.service;

import br.com.byop.aegis.product.api.AssetStorageStrategy;
import br.com.byop.aegis.product.api.ProductExportStoragePort;
import br.com.byop.aegis.product.api.ProductExportStoredFile;
import br.com.byop.aegis.product.export.domain.ExportToken;
import br.com.byop.aegis.product.export.dto.StoredExport;
import br.com.byop.aegis.product.export.exception.ExportStorageException;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class ExportStorageService {

    private final ProductExportStoragePort storagePort;

    public ExportStorageService(ProductExportStoragePort storagePort) {
        this.storagePort = storagePort;
    }

    public StoredExport store(Path temporaryZip, UUID tokenId, String filename, AssetStorageStrategy strategy) {
        try {
            ProductExportStoredFile stored = storagePort.storeExport(temporaryZip, tokenId, filename, strategy);
            return new StoredExport(stored.storageProvider(), stored.zipPath(), stored.filename(), stored.sizeBytes(), stored.localPath());
        } catch (RuntimeException exception) {
            throw new ExportStorageException("Unable to store export ZIP", exception);
        }
    }

    public InputStream openStream(ExportToken token) {
        try {
            return storagePort.openExport(token.getStorageProvider(), token.getZipPath());
        } catch (RuntimeException exception) {
            throw new ExportStorageException("Unable to open export ZIP", exception);
        }
    }

    public void delete(ExportToken token) {
        try {
            storagePort.deleteExport(token.getStorageProvider(), token.getZipPath());
        } catch (RuntimeException exception) {
            throw new ExportStorageException("Unable to delete export ZIP", exception);
        }
    }

    public String filename(String zipPath) {
        return storagePort.filename(zipPath);
    }
}
