package br.com.byop.aegis.product.export.service;

import br.com.byop.aegis.product.api.ProductExportStoragePort;
import br.com.byop.aegis.product.export.dto.ExportAssetFile;
import br.com.byop.aegis.product.export.dto.ProductExportData;
import br.com.byop.aegis.product.export.exception.ProductExportException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ExportZipBuilder {

    private static final int BUFFER_SIZE = 8192;

    private final ProductExportStoragePort storagePort;
    private final Path exportTempDirectory;

    @Autowired
    public ExportZipBuilder(ProductExportStoragePort storagePort,
                            @Value("${aegis.export.temp-dir:${AEGIS_EXPORT_TEMP_DIR:./data/exports/tmp}}")
                            String exportTempDirectory) {
        this(storagePort, Path.of(exportTempDirectory));
    }

    ExportZipBuilder(ProductExportStoragePort storagePort, Path exportTempDirectory) {
        this.storagePort = storagePort;
        this.exportTempDirectory = exportTempDirectory.toAbsolutePath().normalize();
    }

    public Path build(ProductExportData data) {
        try {
            Files.createDirectories(exportTempDirectory);
            Path temporaryZip = Files.createTempFile(exportTempDirectory, "aegis-export-", ".zip");
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(temporaryZip))) {
                writeJsonEntries(zipOutputStream, data.jsonEntries());
                writeAssets(zipOutputStream, data.assets());
            }
            return temporaryZip;
        } catch (IOException exception) {
            throw new ProductExportException("Unable to build export ZIP", exception);
        }
    }

    private void writeJsonEntries(ZipOutputStream zipOutputStream, Map<String, byte[]> entries) throws IOException {
        for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
            zipOutputStream.putNextEntry(new ZipEntry(entry.getKey()));
            zipOutputStream.write(entry.getValue());
            zipOutputStream.closeEntry();
        }
    }

    private void writeAssets(ZipOutputStream zipOutputStream, Iterable<ExportAssetFile> assets) throws IOException {
        Set<String> usedPaths = new HashSet<>();
        byte[] buffer = new byte[BUFFER_SIZE];
        for (ExportAssetFile asset : assets) {
            String entryName = uniqueAssetEntryName(asset, usedPaths);
            zipOutputStream.putNextEntry(new ZipEntry(entryName));
            try (InputStream inputStream = openAssetStream(asset)) {
                int read;
                while ((read = inputStream.read(buffer)) >= 0) {
                    zipOutputStream.write(buffer, 0, read);
                }
            }
            zipOutputStream.closeEntry();
        }
    }

    private InputStream openAssetStream(ExportAssetFile asset) {
        return storagePort.openAsset(asset.storageProvider(), asset.storageKey());
    }

    private String uniqueAssetEntryName(ExportAssetFile asset, Set<String> usedPaths) {
        String candidate = "assets/files/%s/%s".formatted(asset.category(), asset.name());
        if (usedPaths.add(candidate)) {
            return candidate;
        }
        String suffixed = "assets/files/%s/%s".formatted(asset.category(), suffixedName(asset.name(), asset.id()));
        usedPaths.add(suffixed);
        return suffixed;
    }

    private String suffixedName(String filename, String assetId) {
        int dotIndex = filename.lastIndexOf('.');
        String suffix = "-" + assetId.substring(0, Math.min(8, assetId.length()));
        if (dotIndex > 0) {
            return filename.substring(0, dotIndex) + suffix + filename.substring(dotIndex);
        }
        return filename + suffix;
    }
}
