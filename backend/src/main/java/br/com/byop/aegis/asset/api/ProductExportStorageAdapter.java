package br.com.byop.aegis.asset.api;

import br.com.byop.aegis.asset.config.S3StorageProperties;
import br.com.byop.aegis.asset.storage.LocalStorageProvider;
import br.com.byop.aegis.asset.storage.S3StorageProvider;
import br.com.byop.aegis.product.api.AssetStorageStrategy;
import br.com.byop.aegis.product.api.ProductExportStoragePort;
import br.com.byop.aegis.product.api.ProductExportStoredFile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

@Service
public class ProductExportStorageAdapter implements ProductExportStoragePort {

    private static final String LOCAL_PROVIDER = "local";
    private static final String S3_PROVIDER = "s3";
    private static final String EXPORTS_FOLDER = "exports";
    private static final String S3_EXPORT_PREFIX = "aegis/pms/exports";

    private final Path basePath;
    private final S3Client s3Client;
    private final S3StorageProperties s3Properties;
    private final LocalStorageProvider localStorageProvider;
    private final S3StorageProvider s3StorageProvider;

    public ProductExportStorageAdapter(@Value("${aegis.storage.local-path:${AEGIS_STORAGE_LOCAL_PATH:./data/assets}}")
                                       String localStoragePath,
                                       S3Client s3Client,
                                       S3StorageProperties s3Properties,
                                       LocalStorageProvider localStorageProvider,
                                       S3StorageProvider s3StorageProvider) {
        this.basePath = Path.of(localStoragePath).toAbsolutePath().normalize();
        this.s3Client = s3Client;
        this.s3Properties = s3Properties;
        this.localStorageProvider = localStorageProvider;
        this.s3StorageProvider = s3StorageProvider;
    }

    @Override
    public boolean isStorageConfigured(AssetStorageStrategy strategy) {
        if (strategy != AssetStorageStrategy.S3) {
            return true;
        }
        return s3Properties.bucket() != null && !s3Properties.bucket().isBlank();
    }

    @Override
    public ProductExportStoredFile storeExport(Path temporaryZip, UUID tokenId, String filename, AssetStorageStrategy strategy) {
        if (strategy == AssetStorageStrategy.S3) {
            return storeS3(temporaryZip, tokenId, filename);
        }
        return storeLocal(temporaryZip, tokenId, filename);
    }

    @Override
    public InputStream openExport(String storageProvider, String zipPath) {
        if (S3_PROVIDER.equals(storageProvider)) {
            return s3Client.getObject(builder -> builder.bucket(s3Properties.bucket()).key(zipPath));
        }
        try {
            return Files.newInputStream(resolveLocalExportPath(zipPath));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to open export ZIP", exception);
        }
    }

    @Override
    public void deleteExport(String storageProvider, String zipPath) {
        if (S3_PROVIDER.equals(storageProvider)) {
            s3Client.deleteObject(builder -> builder.bucket(s3Properties.bucket()).key(zipPath));
            return;
        }
        try {
            Files.deleteIfExists(resolveLocalExportPath(zipPath));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to delete export ZIP", exception);
        }
    }

    @Override
    public InputStream openAsset(String storageProvider, String storageKey) {
        if (S3_PROVIDER.equals(storageProvider)) {
            return s3StorageProvider.openStream(storageKey);
        }
        return localStorageProvider.openStream(storageKey);
    }

    @Override
    public void deleteAsset(String storageProvider, String storageKey) {
        if (S3_PROVIDER.equals(storageProvider)) {
            s3StorageProvider.delete(storageKey);
            return;
        }
        localStorageProvider.delete(storageKey);
    }

    @Override
    public String filename(String zipPath) {
        int slashIndex = zipPath.lastIndexOf('/');
        return slashIndex < 0 ? zipPath : zipPath.substring(slashIndex + 1);
    }

    private ProductExportStoredFile storeLocal(Path temporaryZip, UUID tokenId, String filename) {
        Path target = localPath(tokenId, filename);
        try {
            Files.createDirectories(target.getParent());
            Files.move(temporaryZip, target, StandardCopyOption.REPLACE_EXISTING);
            long sizeBytes = Files.size(target);
            String zipPath = basePath.relativize(target).toString().replace('\\', '/');
            return new ProductExportStoredFile(LOCAL_PROVIDER, zipPath, filename, sizeBytes, target);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to store local export ZIP", exception);
        }
    }

    private ProductExportStoredFile storeS3(Path temporaryZip, UUID tokenId, String filename) {
        String key = s3Key(tokenId, filename);
        try {
            s3Client.putObject(builder -> builder.bucket(s3Properties.bucket()).key(key), RequestBody.fromFile(temporaryZip));
            long sizeBytes = Files.size(temporaryZip);
            Files.deleteIfExists(temporaryZip);
            return new ProductExportStoredFile(S3_PROVIDER, key, filename, sizeBytes, null);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Unable to store S3 export ZIP", exception);
        }
    }

    private String s3Key(UUID tokenId, String filename) {
        return "%s/%s/%s".formatted(S3_EXPORT_PREFIX, tokenId, filename);
    }

    private Path localPath(UUID tokenId, String filename) {
        return basePath.resolve(Path.of(EXPORTS_FOLDER, tokenId.toString(), filename)).normalize();
    }

    private Path resolveLocalExportPath(String zipPath) {
        Path resolved = basePath.resolve(zipPath).normalize();
        Path exportsRoot = basePath.resolve(EXPORTS_FOLDER).normalize();
        if (!resolved.startsWith(exportsRoot)) {
            throw new IllegalStateException("Invalid export ZIP path: " + zipPath.toLowerCase(Locale.ROOT));
        }
        return resolved;
    }
}
