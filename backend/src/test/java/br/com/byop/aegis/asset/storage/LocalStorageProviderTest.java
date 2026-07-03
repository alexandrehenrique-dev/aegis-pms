package br.com.byop.aegis.asset.storage;

import br.com.byop.aegis.asset.domain.AssetCategory;
import br.com.byop.aegis.asset.exception.AssetStorageException;
import br.com.byop.aegis.asset.exception.InvalidAssetFilenameException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

class LocalStorageProviderTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PRODUCT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @TempDir
    Path tempDir;

    @Test
    void shouldProvisionOneFolderPerCategory() {
        LocalStorageProvider provider = newProvider();

        provider.provisionProductFolders(TENANT_ID, PRODUCT_ID);

        for (AssetCategory category : AssetCategory.values()) {
            assertThat(tempDir.resolve(Path.of("aegis", "pms", TENANT_ID.toString(), PRODUCT_ID.toString(), category.contractValue())))
                    .isDirectory();
        }
    }

    @Test
    void shouldStoreFileAndReturnRelativeStorageKey() {
        LocalStorageProvider provider = newProvider();

        String storageKey = provider.store(TENANT_ID, PRODUCT_ID, AssetCategory.PDF, "curriculo.pdf", "conteudo".getBytes(StandardCharsets.UTF_8));

        assertThat(storageKey).isEqualTo("aegis/pms/%s/%s/pdf/curriculo.pdf".formatted(TENANT_ID, PRODUCT_ID));
        assertThat(tempDir.resolve(storageKey)).exists();
    }

    @Test
    void shouldSanitizeAccentsAndSpacesInFilename() {
        LocalStorageProvider provider = newProvider();

        String storageKey = provider.store(TENANT_ID, PRODUCT_ID, AssetCategory.IMAGE, "Foto da Equipe (2026).PNG", "x".getBytes(StandardCharsets.UTF_8));

        assertThat(storageKey).endsWith("/foto-da-equipe-2026.png");
    }

    @Test
    void shouldRejectPathTraversalByExtractingFileNameOnly() {
        LocalStorageProvider provider = newProvider();

        String storageKey = provider.store(TENANT_ID, PRODUCT_ID, AssetCategory.DOCUMENT, "../../etc/passwd", "x".getBytes(StandardCharsets.UTF_8));

        assertThat(storageKey).endsWith("/passwd");
        assertThat(tempDir.resolve(storageKey).normalize().startsWith(tempDir)).isTrue();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"###.png", "   ", "/"})
    void shouldRejectInvalidFilenames(String invalidFilename) {
        LocalStorageProvider provider = newProvider();
        byte[] content = "x".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> provider.store(TENANT_ID, PRODUCT_ID, AssetCategory.IMAGE, invalidFilename, content))
                .isInstanceOf(InvalidAssetFilenameException.class);
    }

    @Test
    void shouldSanitizeFilenameWithoutExtension() {
        LocalStorageProvider provider = newProvider();

        String storageKey = provider.store(TENANT_ID, PRODUCT_ID, AssetCategory.DOCUMENT, "README", "x".getBytes(StandardCharsets.UTF_8));

        assertThat(storageKey).endsWith("/readme");
    }

    @Test
    void shouldAppendSuffixOnNameCollision() {
        LocalStorageProvider provider = newProvider();
        provider.store(TENANT_ID, PRODUCT_ID, AssetCategory.IMAGE, "foto.png", "1".getBytes(StandardCharsets.UTF_8));

        String secondKey = provider.store(TENANT_ID, PRODUCT_ID, AssetCategory.IMAGE, "foto.png", "2".getBytes(StandardCharsets.UTF_8));
        String thirdKey = provider.store(TENANT_ID, PRODUCT_ID, AssetCategory.IMAGE, "foto.png", "3".getBytes(StandardCharsets.UTF_8));

        assertThat(secondKey).endsWith("/foto-2.png");
        assertThat(thirdKey).endsWith("/foto-3.png");
    }

    @Test
    void shouldNeverExposeAbsoluteDiskPathOnResolve() {
        LocalStorageProvider provider = newProvider();
        UUID assetId = UUID.randomUUID();

        ResolvedLocation location = provider.resolve(assetId, "aegis/pms/x/y/image/foto.png");

        assertThat(location.url()).isEqualTo("/api/v1/assets/" + assetId + "/file");
        assertThat(location.url()).doesNotContain(tempDir.toString());
        assertThat(location.expiresAt()).isNull();
    }

    @Test
    void shouldLoadStoredContent() {
        LocalStorageProvider provider = newProvider();
        String storageKey = provider.store(TENANT_ID, PRODUCT_ID, AssetCategory.IMAGE, "foto.png", "conteudo".getBytes(StandardCharsets.UTF_8));

        byte[] loaded = provider.loadContent(storageKey);

        assertThat(new String(loaded, StandardCharsets.UTF_8)).isEqualTo("conteudo");
    }

    @Test
    void shouldDeleteStoredFile() {
        LocalStorageProvider provider = newProvider();
        String storageKey = provider.store(TENANT_ID, PRODUCT_ID, AssetCategory.IMAGE, "foto.png", "x".getBytes(StandardCharsets.UTF_8));
        assertThat(tempDir.resolve(storageKey)).exists();

        provider.delete(storageKey);

        assertThat(Files.exists(tempDir.resolve(storageKey))).isFalse();
    }

    @Test
    void shouldNotFailWhenDeletingMissingFile() {
        LocalStorageProvider provider = newProvider();

        assertThatCode(() -> provider.delete("aegis/pms/missing/missing/image/missing.png"))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAppendSuffixOnCollisionForFilenameWithoutExtension() {
        LocalStorageProvider provider = newProvider();
        provider.store(TENANT_ID, PRODUCT_ID, AssetCategory.DOCUMENT, "README", "1".getBytes(StandardCharsets.UTF_8));

        String secondKey = provider.store(TENANT_ID, PRODUCT_ID, AssetCategory.DOCUMENT, "README", "2".getBytes(StandardCharsets.UTF_8));

        assertThat(secondKey).endsWith("/readme-2");
    }

    @Test
    void shouldWrapIOExceptionWhenProvisioningFolders() {
        LocalStorageProvider provider = newProvider();
        try (MockedStatic<Files> mocked = mockStatic(Files.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> Files.createDirectories(any(Path.class))).thenThrow(new IOException("disk full"));

            assertThatThrownBy(() -> provider.provisionProductFolders(TENANT_ID, PRODUCT_ID))
                    .isInstanceOf(AssetStorageException.class);
        }
    }

    @Test
    void shouldWrapIOExceptionWhenWritingFile() {
        LocalStorageProvider provider = newProvider();
        byte[] content = "x".getBytes(StandardCharsets.UTF_8);
        try (MockedStatic<Files> mocked = mockStatic(Files.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> Files.write(any(Path.class), any(byte[].class))).thenThrow(new IOException("disk full"));

            assertThatThrownBy(() -> provider.store(TENANT_ID, PRODUCT_ID, AssetCategory.IMAGE, "foto.png", content))
                    .isInstanceOf(AssetStorageException.class);
        }
    }

    @Test
    void shouldWrapIOExceptionWhenDeletingFile() {
        LocalStorageProvider provider = newProvider();
        try (MockedStatic<Files> mocked = mockStatic(Files.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> Files.deleteIfExists(any(Path.class))).thenThrow(new IOException("disk error"));

            assertThatThrownBy(() -> provider.delete("aegis/pms/x/y/image/foto.png"))
                    .isInstanceOf(AssetStorageException.class);
        }
    }

    @Test
    void shouldWrapIOExceptionWhenOpeningStream() {
        LocalStorageProvider provider = newProvider();
        try (MockedStatic<Files> mocked = mockStatic(Files.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> Files.newInputStream(any(Path.class))).thenThrow(new IOException("disk error"));

            assertThatThrownBy(() -> provider.openStream("aegis/pms/x/y/image/foto.png"))
                    .isInstanceOf(AssetStorageException.class);
        }
    }

    @Test
    void shouldWrapIOExceptionWhenLoadingContent() {
        LocalStorageProvider provider = newProvider();
        String storageKey = provider.store(TENANT_ID, PRODUCT_ID, AssetCategory.IMAGE, "foto.png", "x".getBytes(StandardCharsets.UTF_8));
        try (MockedStatic<Files> mocked = mockStatic(Files.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> Files.newInputStream(any(Path.class))).thenReturn(new FailingInputStream());

            assertThatThrownBy(() -> provider.loadContent(storageKey))
                    .isInstanceOf(AssetStorageException.class);
        }
    }

    private LocalStorageProvider newProvider() {
        return new LocalStorageProvider(tempDir.toString());
    }

    private static final class FailingInputStream extends InputStream {

        @Override
        public int read() throws IOException {
            throw new IOException("disk error");
        }
    }
}
