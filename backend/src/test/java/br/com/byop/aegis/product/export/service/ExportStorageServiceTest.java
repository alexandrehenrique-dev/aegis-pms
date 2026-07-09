package br.com.byop.aegis.product.export.service;

import br.com.byop.aegis.product.api.AssetStorageStrategy;
import br.com.byop.aegis.product.api.ProductExportStoragePort;
import br.com.byop.aegis.product.api.ProductExportStoredFile;
import br.com.byop.aegis.product.export.domain.ExportToken;
import br.com.byop.aegis.product.export.dto.StoredExport;
import br.com.byop.aegis.product.export.exception.ExportStorageException;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExportStorageServiceTest {

    private static final UUID TOKEN_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID PRODUCT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Test
    void shouldStoreUsingPublicStoragePort() {
        ProductExportStoragePort storagePort = mock(ProductExportStoragePort.class);
        Path zip = Path.of("target/export.zip");
        ProductExportStoredFile storedFile = new ProductExportStoredFile("local", "exports/token/export.zip", "export.zip", 3L, zip);
        when(storagePort.storeExport(zip, TOKEN_ID, "export.zip", AssetStorageStrategy.LOCAL)).thenReturn(storedFile);

        StoredExport stored = new ExportStorageService(storagePort).store(zip, TOKEN_ID, "export.zip", AssetStorageStrategy.LOCAL);

        assertThat(stored.storageProvider()).isEqualTo("local");
        assertThat(stored.zipPath()).isEqualTo("exports/token/export.zip");
        assertThat(stored.filename()).isEqualTo("export.zip");
        assertThat(stored.sizeBytes()).isEqualTo(3L);
        assertThat(stored.localPath()).isEqualTo(zip);
    }

    @Test
    void shouldWrapStoreFailures() {
        ProductExportStoragePort storagePort = mock(ProductExportStoragePort.class);
        Path zip = Path.of("target/export.zip");
        when(storagePort.storeExport(zip, TOKEN_ID, "export.zip", AssetStorageStrategy.LOCAL))
                .thenThrow(new IllegalStateException("broken"));
        ExportStorageService service = new ExportStorageService(storagePort);

        assertThatThrownBy(() -> service.store(zip, TOKEN_ID, "export.zip", AssetStorageStrategy.LOCAL))
                .isInstanceOf(ExportStorageException.class)
                .hasMessage("Unable to store export ZIP");
    }

    @Test
    void shouldOpenDeleteAndResolveFilenameThroughPublicStoragePort() {
        ProductExportStoragePort storagePort = mock(ProductExportStoragePort.class);
        ExportToken token = token("exports/token/export.zip", "local");
        ByteArrayInputStream stream = new ByteArrayInputStream("zip".getBytes());
        when(storagePort.openExport("local", token.getZipPath())).thenReturn(stream);
        when(storagePort.filename(token.getZipPath())).thenReturn("export.zip");
        ExportStorageService service = new ExportStorageService(storagePort);

        assertThat(service.openStream(token)).isSameAs(stream);
        service.delete(token);

        assertThat(service.filename(token.getZipPath())).isEqualTo("export.zip");
        verify(storagePort).deleteExport("local", token.getZipPath());
    }

    @Test
    void shouldWrapOpenAndDeleteFailures() {
        ProductExportStoragePort storagePort = mock(ProductExportStoragePort.class);
        ExportToken token = token("exports/token/export.zip", "local");
        when(storagePort.openExport("local", token.getZipPath())).thenThrow(new IllegalStateException("broken"));
        ExportStorageService service = new ExportStorageService(storagePort);

        assertThatThrownBy(() -> service.openStream(token))
                .isInstanceOf(ExportStorageException.class)
                .hasMessage("Unable to open export ZIP");

        doThrow(new IllegalStateException("broken"))
                .when(storagePort)
                .deleteExport("local", token.getZipPath());

        assertThatThrownBy(() -> service.delete(token))
                .isInstanceOf(ExportStorageException.class)
                .hasMessage("Unable to delete export ZIP");
    }

    private ExportToken token(String zipPath, String storageProvider) {
        return new ExportToken(PRODUCT_ID, "maestro-beton", zipPath, storageProvider, "owner@byop.dev");
    }
}
