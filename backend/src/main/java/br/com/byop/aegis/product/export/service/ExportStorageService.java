package br.com.byop.aegis.product.export.service;

import br.com.byop.aegis.product.api.AssetStorageStrategy;
import br.com.byop.aegis.product.api.ProductExportStoragePort;
import br.com.byop.aegis.product.api.ProductExportStoredFile;
import br.com.byop.aegis.product.export.domain.ExportToken;
import br.com.byop.aegis.product.export.dto.StoredExport;
import br.com.byop.aegis.product.export.exception.ExportStorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.UUID;

@Slf4j
@Service
public class ExportStorageService {

    private final ProductExportStoragePort storagePort;

    public ExportStorageService(ProductExportStoragePort storagePort) {
        this.storagePort = storagePort;
    }

    public StoredExport store(Path temporaryZip, UUID tokenId, String filename, AssetStorageStrategy strategy) {
        log.debug("store: tokenId='{}', filename='{}'", tokenId, filename);
        try {
            ProductExportStoredFile stored = storagePort.storeExport(temporaryZip, tokenId, filename, strategy);
            log.info("store: export armazenado tokenId='{}', sizeBytes='{}'", tokenId, stored.sizeBytes());
            return new StoredExport(stored.storageProvider(), stored.zipPath(), stored.filename(), stored.sizeBytes(), stored.localPath());
        } catch (RuntimeException exception) {
            log.warn("store: falha ao armazenar export tokenId='{}'", tokenId);
            throw new ExportStorageException("Unable to store export ZIP", exception);
        }
    }

    public InputStream openStream(ExportToken token) {
        log.debug("openStream: tokenId='{}'", token.getId());
        try {
            return storagePort.openExport(token.getStorageProvider(), token.getZipPath());
        } catch (RuntimeException exception) {
            log.warn("openStream: falha ao abrir export tokenId='{}'", token.getId());
            throw new ExportStorageException("Unable to open export ZIP", exception);
        }
    }

    public void delete(ExportToken token) {
        log.debug("delete: tokenId='{}'", token.getId());
        try {
            storagePort.deleteExport(token.getStorageProvider(), token.getZipPath());
            log.info("delete: export removido tokenId='{}'", token.getId());
        } catch (RuntimeException exception) {
            log.warn("delete: falha ao remover export tokenId='{}'", token.getId());
            throw new ExportStorageException("Unable to delete export ZIP", exception);
        }
    }

    public String filename(String zipPath) {
        return storagePort.filename(zipPath);
    }
}
