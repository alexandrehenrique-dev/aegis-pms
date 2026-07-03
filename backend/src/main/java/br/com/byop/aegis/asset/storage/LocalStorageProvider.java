package br.com.byop.aegis.asset.storage;

import br.com.byop.aegis.asset.domain.AssetCategory;
import br.com.byop.aegis.asset.exception.AssetStorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Implementacao de {@link StorageProvider} que grava os assets em disco, sob
 * {@code ${AEGIS_STORAGE_LOCAL_PATH}/aegis/pms/{tenantId}/{productId}/{category}/{filename}}.
 * {@code resolve()} nunca expõe o caminho de disco — devolve uma URL servida
 * pelo proprio backend ({@code GET /api/v1/assets/{assetId}/file}).
 */
@Component
public class LocalStorageProvider implements StorageProvider {

    private final Path basePath;

    public LocalStorageProvider(
            @Value("${aegis.storage.local-path:${AEGIS_STORAGE_LOCAL_PATH:./data/assets}}")
            String localStoragePath
    ) {
        this.basePath = Path.of(localStoragePath).toAbsolutePath().normalize();
    }

    @Override
    public void provisionProductFolders(UUID tenantId, UUID productId) {
        for (AssetCategory category : AssetCategory.values()) {
            createDirectories(categoryFolder(tenantId, productId, category));
        }
    }

    @Override
    public String store(UUID tenantId, UUID productId, AssetCategory category, String originalFilename, byte[] content) {
        String sanitizedFilename = AssetFilenameSanitizer.sanitize(originalFilename);
        Path folder = categoryFolder(tenantId, productId, category);
        createDirectories(folder);
        String finalFilename = resolveCollisionFreeFilename(folder, sanitizedFilename);
        writeFile(folder.resolve(finalFilename), content);
        return "aegis/pms/%s/%s/%s/%s".formatted(tenantId, productId, category.contractValue(), finalFilename);
    }

    @Override
    public ResolvedLocation resolve(UUID assetId, String storageKey) {
        return new ResolvedLocation("/api/v1/assets/" + assetId + "/file", null);
    }

    @Override
    public InputStream openStream(String storageKey) {
        try {
            return Files.newInputStream(resolveAbsolutePath(storageKey));
        } catch (IOException exception) {
            throw new AssetStorageException("Failed to open asset file: " + storageKey, exception);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolveAbsolutePath(storageKey));
        } catch (IOException exception) {
            throw new AssetStorageException("Failed to delete asset file: " + storageKey, exception);
        }
    }

    /**
     * Le os bytes de um asset gravado localmente — usado exclusivamente pelo
     * endpoint {@code GET /api/v1/assets/{assetId}/file}, alvo da URL devolvida
     * por {@link #resolve}. Nunca chamado para assets em {@code s3}.
     *
     * @param storageKey chave de armazenamento retornada por {@link #store}
     * @return bytes do arquivo
     */
    public byte[] loadContent(String storageKey) {
        try (InputStream stream = openStream(storageKey)) {
            return stream.readAllBytes();
        } catch (IOException exception) {
            throw new AssetStorageException("Failed to read asset file: " + storageKey, exception);
        }
    }

    private Path resolveAbsolutePath(String storageKey) {
        return basePath.resolve(storageKey).normalize();
    }

    private Path categoryFolder(UUID tenantId, UUID productId, AssetCategory category) {
        return basePath.resolve(Path.of("aegis", "pms", tenantId.toString(), productId.toString(), category.contractValue()));
    }

    private String resolveCollisionFreeFilename(Path folder, String sanitizedFilename) {
        if (Files.notExists(folder.resolve(sanitizedFilename))) {
            return sanitizedFilename;
        }

        int dotIndex = sanitizedFilename.lastIndexOf('.');
        String base = dotIndex > 0 ? sanitizedFilename.substring(0, dotIndex) : sanitizedFilename;
        String extension = dotIndex > 0 ? sanitizedFilename.substring(dotIndex) : "";

        int suffix = 2;
        String candidate;
        do {
            candidate = base + "-" + suffix + extension;
            suffix++;
        } while (Files.exists(folder.resolve(candidate)));
        return candidate;
    }

    private void createDirectories(Path folder) {
        try {
            Files.createDirectories(folder);
        } catch (IOException exception) {
            throw new AssetStorageException("Failed to provision asset storage folder: " + folder, exception);
        }
    }

    private void writeFile(Path target, byte[] content) {
        try {
            Files.write(target, content);
        } catch (IOException exception) {
            throw new AssetStorageException("Failed to write asset file: " + target, exception);
        }
    }
}
