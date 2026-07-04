package br.com.byop.aegis.product.export.service;

import br.com.byop.aegis.product.export.domain.ExportToken;
import br.com.byop.aegis.product.export.dto.ExportAssetFile;
import br.com.byop.aegis.product.export.dto.ProductExportData;
import br.com.byop.aegis.product.export.dto.StoredExport;
import br.com.byop.aegis.product.export.exception.ProductExportException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
public class ExportIntegrityService {

    private static final Set<String> REQUIRED_ENTRIES = Set.of(
            "manifest.json",
            "product.json",
            "modules.json",
            "content/entries.json",
            "pages/pages.json",
            "forms/forms.json",
            "forms/submissions.json",
            "assets/metadata.json",
            "knowledge-graph/nodes.json",
            "knowledge-graph/edges.json",
            "users/assignments.json",
            "audit/events.json"
    );

    private final ExportStorageService exportStorageService;

    public ExportIntegrityService(ExportStorageService exportStorageService) {
        this.exportStorageService = exportStorageService;
    }

    public void validateStoredZip(ProductExportData data, StoredExport storedExport, ExportToken token) {
        log.debug("validateStoredZip: productId='{}'", data.productId());
        Set<String> entries = readEntries(token);
        if (!entries.containsAll(REQUIRED_ENTRIES)) {
            log.warn("validateStoredZip: ZIP incompleto (entradas obrigatorias ausentes) productId='{}'", data.productId());
            throw new ProductExportException("Stored ZIP is missing required JSON entries", null);
        }
        for (ExportAssetFile asset : data.assets()) {
            if (!containsAsset(entries, asset)) {
                log.warn("validateStoredZip: asset ausente no ZIP productId='{}', assetId='{}'", data.productId(), asset.id());
                throw new ProductExportException("Stored ZIP is missing asset file " + asset.name(), null);
            }
        }
        if (storedExport.sizeBytes() <= 0) {
            log.warn("validateStoredZip: ZIP vazio productId='{}'", data.productId());
            throw new ProductExportException("Stored ZIP is empty", null);
        }
    }

    private Set<String> readEntries(ExportToken token) {
        Set<String> entries = new HashSet<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(exportStorageService.openStream(token))) {
            ZipEntry entry = zipInputStream.getNextEntry();
            while (entry != null) {
                entries.add(entry.getName());
                entry = zipInputStream.getNextEntry();
            }
            return entries;
        } catch (IOException exception) {
            throw new ProductExportException("Unable to validate stored ZIP", exception);
        }
    }

    private boolean containsAsset(Set<String> entries, ExportAssetFile asset) {
        String prefix = "assets/files/%s/".formatted(asset.category());
        return entries.stream()
                .filter(entry -> entry.startsWith(prefix))
                .anyMatch(entry -> entry.endsWith("/" + asset.name()) || entry.contains(asset.id().substring(0, 8)));
    }
}
