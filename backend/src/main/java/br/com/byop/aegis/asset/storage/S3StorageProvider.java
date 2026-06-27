package br.com.byop.aegis.asset.storage;

import br.com.byop.aegis.asset.config.S3StorageProperties;
import br.com.byop.aegis.asset.domain.AssetCategory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Implementacao de {@link StorageProvider} que grava os assets num bucket S3,
 * sob a chave {@code aegis/pms/{tenantId}/{productId}/{category}/{filename}}.
 * {@code resolve()} gera uma URL pre-assinada com TTL curto — nunca uma URL
 * publica permanente, e nunca expõe as credenciais do backend.
 */
@Component
public class S3StorageProvider implements StorageProvider {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3StorageProperties properties;

    public S3StorageProvider(S3Client s3Client, S3Presigner s3Presigner, S3StorageProperties properties) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.properties = properties;
    }

    @Override
    public void provisionProductFolders(UUID tenantId, UUID productId) {
        // No-op — o bucket ja existe e e compartilhado entre produtos, isolado so pelo prefixo da chave.
    }

    @Override
    public String store(UUID tenantId, UUID productId, AssetCategory category, String originalFilename, byte[] content) {
        String sanitizedFilename = AssetFilenameSanitizer.sanitize(originalFilename);
        String keyPrefix = "aegis/pms/%s/%s/%s/".formatted(tenantId, productId, category.contractValue());
        String finalFilename = resolveCollisionFreeFilename(keyPrefix, sanitizedFilename);
        String key = keyPrefix + finalFilename;

        s3Client.putObject(builder -> builder.bucket(properties.bucket()).key(key), RequestBody.fromBytes(content));
        return key;
    }

    @Override
    public ResolvedLocation resolve(UUID assetId, String storageKey) {
        Duration ttl = Duration.ofSeconds(properties.presignedUrlTtlSeconds());
        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(builder -> builder
                .signatureDuration(ttl)
                .getObjectRequest(getObjectBuilder -> getObjectBuilder.bucket(properties.bucket()).key(storageKey)));
        OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plus(ttl);
        return new ResolvedLocation(presigned.url().toString(), expiresAt);
    }

    @Override
    public void delete(String storageKey) {
        s3Client.deleteObject(builder -> builder.bucket(properties.bucket()).key(storageKey));
    }

    private String resolveCollisionFreeFilename(String keyPrefix, String sanitizedFilename) {
        if (!objectExists(keyPrefix + sanitizedFilename)) {
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
        } while (objectExists(keyPrefix + candidate));
        return candidate;
    }

    private boolean objectExists(String key) {
        try {
            s3Client.headObject(builder -> builder.bucket(properties.bucket()).key(key));
            return true;
        } catch (NoSuchKeyException _) {
            return false;
        }
    }
}
