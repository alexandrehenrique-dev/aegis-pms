package br.com.byop.aegis.asset.api;

import br.com.byop.aegis.asset.config.S3StorageProperties;
import br.com.byop.aegis.asset.storage.LocalStorageProvider;
import br.com.byop.aegis.asset.storage.S3StorageProvider;
import br.com.byop.aegis.product.api.AssetStorageStrategy;
import br.com.byop.aegis.product.api.ProductExportStoredFile;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductExportStorageAdapterTest {

    private static final UUID TOKEN_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final S3StorageProperties S3_PROPERTIES = new S3StorageProperties("aegis-bucket", "us-east-1", "", "", 900);

    @TempDir
    private Path tempDir;

    @Test
    void shouldStoreOpenAndDeleteLocalExport() throws IOException {
        ProductExportStorageAdapter adapter = adapter(mock(S3Client.class), mock(LocalStorageProvider.class), mock(S3StorageProvider.class));
        Path temporaryZip = Files.writeString(tempDir.resolve("source.zip"), "zip");

        ProductExportStoredFile stored = adapter.storeExport(temporaryZip, TOKEN_ID, "export.zip", AssetStorageStrategy.LOCAL);

        assertThat(stored.storageProvider()).isEqualTo("local");
        assertThat(stored.zipPath()).isEqualTo("exports/%s/export.zip".formatted(TOKEN_ID));
        assertThat(stored.filename()).isEqualTo("export.zip");
        assertThat(stored.sizeBytes()).isEqualTo(3L);
        assertThat(Files.exists(stored.localPath())).isTrue();
        assertThat(new String(adapter.openExport("local", stored.zipPath()).readAllBytes())).isEqualTo("zip");

        adapter.deleteExport("local", stored.zipPath());
        assertThat(Files.exists(stored.localPath())).isFalse();
    }

    @Test
    void shouldRejectLocalPathTraversal() {
        ProductExportStorageAdapter adapter = adapter(mock(S3Client.class), mock(LocalStorageProvider.class), mock(S3StorageProvider.class));

        assertThatThrownBy(() -> adapter.openExport("local", "../outside.zip"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid export ZIP path");
    }

    @Test
    void shouldWrapLocalExportStorageFailures() throws IOException {
        ProductExportStorageAdapter adapter = adapter(mock(S3Client.class), mock(LocalStorageProvider.class), mock(S3StorageProvider.class));
        Path missingZip = tempDir.resolve("missing.zip");
        Files.createDirectories(tempDir.resolve("exports/token/non-empty.zip/child"));

        assertThatThrownBy(() -> adapter.storeExport(missingZip, TOKEN_ID, "export.zip", AssetStorageStrategy.LOCAL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to store local export ZIP");
        assertThatThrownBy(() -> adapter.openExport("local", "exports/token/missing.zip"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to open export ZIP");
        assertThatThrownBy(() -> adapter.deleteExport("local", "exports/token/non-empty.zip"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to delete export ZIP");
    }

    @Test
    void shouldStoreOpenAndDeleteS3Export() throws IOException {
        S3Client s3Client = mock(S3Client.class);
        ResponseInputStream<GetObjectResponse> response = new ResponseInputStream<>(
                GetObjectResponse.builder().build(),
                AbortableInputStream.create(InputStream.nullInputStream())
        );
        when(s3Client.getObject(any(Consumer.class))).thenReturn(response);
        ProductExportStorageAdapter adapter = adapter(s3Client, mock(LocalStorageProvider.class), mock(S3StorageProvider.class));
        Path temporaryZip = Files.writeString(tempDir.resolve("source.zip"), "zip");

        ProductExportStoredFile stored = adapter.storeExport(temporaryZip, TOKEN_ID, "export.zip", AssetStorageStrategy.S3);
        InputStream stream = adapter.openExport("s3", stored.zipPath());
        adapter.deleteExport("s3", stored.zipPath());

        assertThat(stored.storageProvider()).isEqualTo("s3");
        assertThat(stored.zipPath()).isEqualTo("aegis/pms/exports/%s/export.zip".formatted(TOKEN_ID));
        assertThat(stored.localPath()).isNull();
        assertThat(stream).isSameAs(response);
        assertThat(adapter.filename(stored.zipPath())).isEqualTo("export.zip");
        assertThat(adapter.filename("export.zip")).isEqualTo("export.zip");
        assertThat(Files.exists(temporaryZip)).isFalse();
        assertThat(capturedPutObjectRequest(s3Client).bucket()).isEqualTo("aegis-bucket");
        assertThat(capturedGetObjectRequest(s3Client).key()).isEqualTo(stored.zipPath());
        assertThat(capturedDeleteObjectRequest(s3Client).key()).isEqualTo(stored.zipPath());
    }

    @Test
    void shouldWrapS3ExportStorageFailure() {
        S3Client s3Client = mock(S3Client.class);
        ProductExportStorageAdapter adapter = adapter(s3Client, mock(LocalStorageProvider.class), mock(S3StorageProvider.class));
        Path missingZip = tempDir.resolve("missing-s3.zip");

        assertThatThrownBy(() -> adapter.storeExport(missingZip, TOKEN_ID, "export.zip", AssetStorageStrategy.S3))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to store S3 export ZIP");
    }

    @Test
    void shouldDelegateAssetStreamsAndDeletes() {
        LocalStorageProvider localStorageProvider = mock(LocalStorageProvider.class);
        S3StorageProvider s3StorageProvider = mock(S3StorageProvider.class);
        ByteArrayInputStream localStream = new ByteArrayInputStream("local".getBytes());
        ByteArrayInputStream s3Stream = new ByteArrayInputStream("s3".getBytes());
        when(localStorageProvider.openStream("local-key")).thenReturn(localStream);
        when(s3StorageProvider.openStream("s3-key")).thenReturn(s3Stream);
        ProductExportStorageAdapter adapter = adapter(mock(S3Client.class), localStorageProvider, s3StorageProvider);

        assertThat(adapter.openAsset("local", "local-key")).isSameAs(localStream);
        assertThat(adapter.openAsset("s3", "s3-key")).isSameAs(s3Stream);
        adapter.deleteAsset("local", "local-key");
        adapter.deleteAsset("s3", "s3-key");

        verify(localStorageProvider).delete("local-key");
        verify(s3StorageProvider).delete("s3-key");
    }

    private ProductExportStorageAdapter adapter(S3Client s3Client, LocalStorageProvider localStorageProvider,
                                                S3StorageProvider s3StorageProvider) {
        return new ProductExportStorageAdapter(tempDir.toString(), s3Client, S3_PROPERTIES, localStorageProvider, s3StorageProvider);
    }

    private PutObjectRequest capturedPutObjectRequest(S3Client s3Client) {
        ArgumentCaptor<Consumer<PutObjectRequest.Builder>> captor = ArgumentCaptor.captor();
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        PutObjectRequest.Builder builder = PutObjectRequest.builder();
        captor.getValue().accept(builder);
        return builder.build();
    }

    private GetObjectRequest capturedGetObjectRequest(S3Client s3Client) {
        ArgumentCaptor<Consumer<GetObjectRequest.Builder>> captor = ArgumentCaptor.captor();
        verify(s3Client).getObject(captor.capture());
        GetObjectRequest.Builder builder = GetObjectRequest.builder();
        captor.getValue().accept(builder);
        return builder.build();
    }

    private DeleteObjectRequest capturedDeleteObjectRequest(S3Client s3Client) {
        ArgumentCaptor<Consumer<DeleteObjectRequest.Builder>> captor = ArgumentCaptor.captor();
        verify(s3Client).deleteObject(captor.capture());
        DeleteObjectRequest.Builder builder = DeleteObjectRequest.builder();
        captor.getValue().accept(builder);
        return builder.build();
    }
}
