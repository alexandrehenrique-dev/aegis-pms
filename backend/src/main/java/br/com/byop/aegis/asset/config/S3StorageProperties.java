package br.com.byop.aegis.asset.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracao do {@code S3StorageProvider} — credenciais nunca versionadas
 * em {@code application.yml}, sempre via variavel de ambiente/secret.
 */
@ConfigurationProperties(prefix = "aegis.storage.s3")
public record S3StorageProperties(
        String bucket,
        String region,
        String accessKeyId,
        String secretAccessKey,
        long presignedUrlTtlSeconds
) {
}
