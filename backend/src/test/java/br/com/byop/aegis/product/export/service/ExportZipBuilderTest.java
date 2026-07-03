package br.com.byop.aegis.product.export.service;

import br.com.byop.aegis.product.api.ProductExportStoragePort;
import br.com.byop.aegis.product.export.dto.ExportAssetFile;
import br.com.byop.aegis.product.export.dto.ProductExportData;
import br.com.byop.aegis.product.api.AssetStorageStrategy;
import br.com.byop.aegis.product.export.exception.ProductExportException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExportZipBuilderTest {

    private static final UUID TENANT_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID PRODUCT_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");

    @TempDir
    private Path tempDir;

    @Test
    void shouldBuildZipWithJsonEntriesAndStreamingAssets() throws Exception {
        ProductExportStoragePort storagePort = mock(ProductExportStoragePort.class);
        when(storagePort.openAsset("local", "local-key")).thenReturn(new ByteArrayInputStream("one".getBytes()));
        when(storagePort.openAsset("s3", "s3-key")).thenReturn(new ByteArrayInputStream("two".getBytes()));
        ExportZipBuilder builder = new ExportZipBuilder(storagePort, tempDir);

        Path zip = builder.build(data());

        assertThat(Files.exists(zip)).isTrue();
        assertThat(zipEntries(zip)).contains(
                "manifest.json",
                "product.json",
                "assets/files/image/logo.png",
                "assets/files/image/logo-bbbbbbbb.png",
                "assets/files/document/readme",
                "assets/files/document/readme-dddddddd"
        );
    }

    @Test
    void shouldWrapAssetStreamFailure() {
        ProductExportStoragePort storagePort = mock(ProductExportStoragePort.class);
        when(storagePort.openAsset("local", "local-key")).thenReturn(new FailingInputStream());
        ExportZipBuilder builder = new ExportZipBuilder(storagePort, tempDir);
        ProductExportData data = failingData();

        assertThatThrownBy(() -> builder.build(data))
                .isInstanceOf(ProductExportException.class)
                .hasMessage("Unable to build export ZIP");
    }

    private ProductExportData data() {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("manifest.json", "{}".getBytes());
        entries.put("product.json", "{}".getBytes());
        return new ProductExportData(
                TENANT_ID,
                PRODUCT_ID,
                "maestro-beton",
                "Maestro Beton",
                AssetStorageStrategy.LOCAL,
                "aegis-export-maestro.zip",
                entries,
                List.of(
                        new ExportAssetFile("aaaaaaaa-0000-0000-0000-000000000000", "logo.png", "image", "local", "local-key", 3),
                        new ExportAssetFile("bbbbbbbb-0000-0000-0000-000000000000", "logo.png", "image", "s3", "s3-key", 3),
                        new ExportAssetFile("cccccccc-0000-0000-0000-000000000000", "readme", "document", "local", "local-key", 3),
                        new ExportAssetFile("dddddddd-0000-0000-0000-000000000000", "readme", "document", "s3", "s3-key", 3)
                ),
                Map.of(),
                Map.of()
        );
    }

    private ProductExportData failingData() {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("manifest.json", "{}".getBytes());
        return new ProductExportData(
                TENANT_ID,
                PRODUCT_ID,
                "maestro-beton",
                "Maestro Beton",
                AssetStorageStrategy.LOCAL,
                "aegis-export-maestro.zip",
                entries,
                List.of(new ExportAssetFile("aaaaaaaa-0000-0000-0000-000000000000", "logo.png", "image", "local", "local-key", 3)),
                Map.of(),
                Map.of()
        );
    }

    private List<String> zipEntries(Path zip) throws Exception {
        List<String> entries = new ArrayList<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zip))) {
            java.util.zip.ZipEntry entry = zipInputStream.getNextEntry();
            while (entry != null) {
                entries.add(entry.getName());
                entry = zipInputStream.getNextEntry();
            }
        }
        return entries;
    }

    private static final class FailingInputStream extends InputStream {

        @Override
        public int read() throws IOException {
            throw new IOException("broken");
        }
    }
}
