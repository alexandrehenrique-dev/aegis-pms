package br.com.byop.aegis.asset.storage;

import br.com.byop.aegis.asset.config.S3StorageProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Clientes do AWS SDK v2 usados por {@link S3StorageProvider}. Credenciais
 * vem exclusivamente de {@link S3StorageProperties} (variavel de
 * ambiente/secret, nunca de {@code application.yml} versionado). Sem
 * credenciais configuradas (produto nenhum usando {@code assetStorageStrategy: "s3"}),
 * o bean ainda precisa existir — usa um provider anonimo que nunca chega a
 * ser exercitado em chamadas reais.
 */
@Configuration
class S3ClientConfiguration {

    @Bean
    S3Client s3Client(S3StorageProperties properties) {
        return S3Client.builder()
                .region(resolveRegion(properties))
                .credentialsProvider(credentialsProvider(properties))
                .build();
    }

    @Bean
    S3Presigner s3Presigner(S3StorageProperties properties) {
        return S3Presigner.builder()
                .region(resolveRegion(properties))
                .credentialsProvider(credentialsProvider(properties))
                .build();
    }

    private AwsCredentialsProvider credentialsProvider(S3StorageProperties properties) {
        if (isBlank(properties.accessKeyId()) || isBlank(properties.secretAccessKey())) {
            return AnonymousCredentialsProvider.create();
        }
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(properties.accessKeyId(), properties.secretAccessKey()));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private Region resolveRegion(S3StorageProperties properties) {
        return Region.of(properties.region() == null || properties.region().isBlank() ? "us-east-1" : properties.region());
    }
}
