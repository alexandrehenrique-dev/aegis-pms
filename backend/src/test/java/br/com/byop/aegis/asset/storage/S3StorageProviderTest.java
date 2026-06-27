package br.com.byop.aegis.asset.storage;

import br.com.byop.aegis.asset.config.S3StorageProperties;
import br.com.byop.aegis.asset.domain.AssetCategory;
import br.com.byop.aegis.asset.exception.InvalidAssetFilenameException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3StorageProviderTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PRODUCT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String SECRET = "super-secret-value";

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    private final S3StorageProperties properties = new S3StorageProperties("aegis-bucket", "us-east-1", "AKIA", SECRET, 900);

    private S3StorageProvider provider() {
        return new S3StorageProvider(s3Client, s3Presigner, properties);
    }

    @Test
    void shouldNotInteractWithS3WhenProvisioningFolders() {
        provider().provisionProductFolders(TENANT_ID, PRODUCT_ID);

        verifyNoInteractions(s3Client, s3Presigner);
    }

    @Test
    void shouldStoreObjectWithExpectedKeyWhenFreeOfCollision() {
        when(s3Client.headObject(any(Consumer.class))).thenThrow(NoSuchKeyException.builder().message("not found").build());

        String key = provider().store(TENANT_ID, PRODUCT_ID, AssetCategory.PDF, "curriculo.pdf", "conteudo".getBytes());

        assertThat(key).isEqualTo("aegis/pms/%s/%s/pdf/curriculo.pdf".formatted(TENANT_ID, PRODUCT_ID));
        PutObjectRequest built = capturedPutObjectRequest();
        assertThat(built.bucket()).isEqualTo("aegis-bucket");
        assertThat(built.key()).isEqualTo(key);
    }

    @Test
    void shouldUseConfiguredBucketAndCandidateKeyWhenCheckingCollision() {
        when(s3Client.headObject(any(Consumer.class))).thenThrow(NoSuchKeyException.builder().message("not found").build());

        String key = provider().store(TENANT_ID, PRODUCT_ID, AssetCategory.PDF, "curriculo.pdf", "conteudo".getBytes());

        HeadObjectRequest built = capturedHeadObjectRequest();
        assertThat(built.bucket()).isEqualTo("aegis-bucket");
        assertThat(built.key()).isEqualTo(key);
    }

    @Test
    void shouldAppendSuffixWhenKeyAlreadyExists() {
        when(s3Client.headObject(any(Consumer.class)))
                .thenReturn(HeadObjectResponse.builder().build())
                .thenThrow(NoSuchKeyException.builder().message("not found").build());

        String key = provider().store(TENANT_ID, PRODUCT_ID, AssetCategory.IMAGE, "foto.png", "x".getBytes());

        assertThat(key).isEqualTo("aegis/pms/%s/%s/image/foto-2.png".formatted(TENANT_ID, PRODUCT_ID));
    }

    @Test
    void shouldAppendSuffixForFilenameWithoutExtensionOnCollision() {
        when(s3Client.headObject(any(Consumer.class)))
                .thenReturn(HeadObjectResponse.builder().build())
                .thenThrow(NoSuchKeyException.builder().message("not found").build());

        String key = provider().store(TENANT_ID, PRODUCT_ID, AssetCategory.DOCUMENT, "README", "x".getBytes());

        assertThat(key).endsWith("/readme-2");
    }

    @Test
    void shouldKeepIncrementingSuffixWhenMultipleCollisionsExist() {
        when(s3Client.headObject(any(Consumer.class)))
                .thenReturn(HeadObjectResponse.builder().build())
                .thenReturn(HeadObjectResponse.builder().build())
                .thenThrow(NoSuchKeyException.builder().message("not found").build());

        String key = provider().store(TENANT_ID, PRODUCT_ID, AssetCategory.IMAGE, "foto.png", "x".getBytes());

        assertThat(key).endsWith("/foto-3.png");
    }

    @Test
    void shouldRejectFilenameThatBecomesEmptyAfterSanitization() {
        S3StorageProvider provider = provider();
        byte[] content = "x".getBytes();

        assertThatThrownBy(() -> provider.store(TENANT_ID, PRODUCT_ID, AssetCategory.IMAGE, "###.png", content))
                .isInstanceOf(InvalidAssetFilenameException.class);
        verifyNoInteractions(s3Client);
    }

    @Test
    void shouldResolveToPresignedUrlWithExpiration() throws MalformedURLException {
        URL presignedUrl = URI.create("https://aegis-bucket.s3.amazonaws.com/aegis/pms/x/y/image/foto.png?X-Amz-Signature=abc").toURL();
        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(presignedUrl);
        when(s3Presigner.presignGetObject(any(Consumer.class))).thenReturn(presigned);

        ResolvedLocation location = provider().resolve(UUID.randomUUID(), "aegis/pms/x/y/image/foto.png");

        assertThat(location.url()).isEqualTo(presignedUrl.toString());
        assertThat(location.url()).doesNotContain(SECRET);
        assertThat(location.expiresAt()).isNotNull();
    }

    @Test
    void shouldUseConfiguredPrefixAndKeyWhenPresigning() {
        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(uncheckedUrl("https://aegis-bucket.s3.amazonaws.com/key"));
        when(s3Presigner.presignGetObject(any(Consumer.class))).thenReturn(presigned);

        provider().resolve(UUID.randomUUID(), "aegis/pms/x/y/image/foto.png");

        GetObjectPresignRequest built = capturedPresignRequest();
        assertThat(built.getObjectRequest().bucket()).isEqualTo("aegis-bucket");
        assertThat(built.getObjectRequest().key()).isEqualTo("aegis/pms/x/y/image/foto.png");
        assertThat(built.signatureDuration().toSeconds()).isEqualTo(900L);
    }

    @Test
    void shouldDeleteObjectByKey() {
        provider().delete("aegis/pms/x/y/image/foto.png");

        DeleteObjectRequest built = capturedDeleteObjectRequest();
        assertThat(built.bucket()).isEqualTo("aegis-bucket");
        assertThat(built.key()).isEqualTo("aegis/pms/x/y/image/foto.png");
        verify(s3Client, never()).putObject(any(Consumer.class), any(RequestBody.class));
    }

    private PutObjectRequest capturedPutObjectRequest() {
        ArgumentCaptor<Consumer<PutObjectRequest.Builder>> captor = ArgumentCaptor.captor();
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        PutObjectRequest.Builder builder = PutObjectRequest.builder();
        captor.getValue().accept(builder);
        return builder.build();
    }

    private DeleteObjectRequest capturedDeleteObjectRequest() {
        ArgumentCaptor<Consumer<DeleteObjectRequest.Builder>> captor = ArgumentCaptor.captor();
        verify(s3Client).deleteObject(captor.capture());
        DeleteObjectRequest.Builder builder = DeleteObjectRequest.builder();
        captor.getValue().accept(builder);
        return builder.build();
    }

    private HeadObjectRequest capturedHeadObjectRequest() {
        ArgumentCaptor<Consumer<HeadObjectRequest.Builder>> captor = ArgumentCaptor.captor();
        verify(s3Client).headObject(captor.capture());
        HeadObjectRequest.Builder builder = HeadObjectRequest.builder();
        captor.getValue().accept(builder);
        return builder.build();
    }

    private GetObjectPresignRequest capturedPresignRequest() {
        ArgumentCaptor<Consumer<GetObjectPresignRequest.Builder>> captor = ArgumentCaptor.captor();
        verify(s3Presigner).presignGetObject(captor.capture());
        GetObjectPresignRequest.Builder builder = GetObjectPresignRequest.builder();
        captor.getValue().accept(builder);
        return builder.build();
    }

    private URL uncheckedUrl(String value) {
        try {
            return URI.create(value).toURL();
        } catch (MalformedURLException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
