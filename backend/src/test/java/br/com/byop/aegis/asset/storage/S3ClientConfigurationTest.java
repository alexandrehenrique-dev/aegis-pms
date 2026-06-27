package br.com.byop.aegis.asset.storage;

import br.com.byop.aegis.asset.config.S3StorageProperties;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class S3ClientConfigurationTest {

    private final S3ClientConfiguration configuration = new S3ClientConfiguration();

    @ParameterizedTest
    @MethodSource("clientProperties")
    void shouldBuildS3Client(S3StorageProperties properties) {
        S3Client client = configuration.s3Client(properties);

        assertThat(client).isNotNull();
        client.close();
    }

    @ParameterizedTest
    @MethodSource("presignerProperties")
    void shouldBuildS3Presigner(S3StorageProperties properties) {
        S3Presigner presigner = configuration.s3Presigner(properties);

        assertThat(presigner).isNotNull();
        presigner.close();
    }

    private static Stream<S3StorageProperties> clientProperties() {
        return Stream.of(
                new S3StorageProperties("bucket", "eu-west-1", "AKIA", "secret", 900),
                new S3StorageProperties("bucket", "", "AKIA", "secret", 900),
                new S3StorageProperties("bucket", "us-east-1", "", "secret", 900),
                new S3StorageProperties("bucket", "us-east-1", "AKIA", null, 900)
        );
    }

    private static Stream<S3StorageProperties> presignerProperties() {
        return Stream.of(
                new S3StorageProperties("bucket", "us-east-1", "AKIA", "secret", 900),
                new S3StorageProperties("bucket", null, "AKIA", "secret", 900),
                new S3StorageProperties("bucket", "us-east-1", "", "", 900)
        );
    }
}
