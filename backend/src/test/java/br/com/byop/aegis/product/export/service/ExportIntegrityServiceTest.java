package br.com.byop.aegis.product.export.service;

import br.com.byop.aegis.product.api.AssetStorageStrategy;
import br.com.byop.aegis.product.export.domain.ExportToken;
import br.com.byop.aegis.product.export.dto.ExportAssetFile;
import br.com.byop.aegis.product.export.dto.ProductExportData;
import br.com.byop.aegis.product.export.dto.StoredExport;
import br.com.byop.aegis.product.export.exception.ProductExportException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportIntegrityServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PRODUCT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String ASSET_ID = "33333333-3333-3333-3333-333333333333";

    @Mock
    private ExportStorageService exportStorageService;

    @Test
    void shouldAcceptCompleteStoredZip() throws IOException {
        ProductExportData data = data(List.of(asset("hero.png")));
        ExportToken token = token();
        StoredExport storedExport = storedExport(1024L);
        when(exportStorageService.openStream(token)).thenReturn(new ByteArrayInputStream(zipBytes(requiredEntries("assets/files/images/hero.png"))));

        service().validateStoredZip(data, storedExport, token);
    }

    @Test
    void shouldAcceptAssetRenamedByCollisionSuffix() throws IOException {
        ProductExportData data = data(List.of(asset("hero.png")));
        ExportToken token = token();
        StoredExport storedExport = storedExport(1024L);
        when(exportStorageService.openStream(token)).thenReturn(new ByteArrayInputStream(zipBytes(requiredEntries("assets/files/images/hero-33333333.png"))));

        service().validateStoredZip(data, storedExport, token);
    }

    @Test
    void shouldRejectZipMissingRequiredJsonEntry() throws IOException {
        ProductExportData data = data(List.of());
        ExportToken token = token();
        StoredExport storedExport = storedExport(1024L);
        List<String> entries = requiredEntries().stream()
                .filter(entry -> !"manifest.json".equals(entry))
                .toList();
        ExportIntegrityService service = service();
        when(exportStorageService.openStream(token)).thenReturn(new ByteArrayInputStream(zipBytes(entries)));

        assertThatThrownBy(() -> service.validateStoredZip(data, storedExport, token))
                .isInstanceOf(ProductExportException.class)
                .hasMessage("Stored ZIP is missing required JSON entries");
    }

    @Test
    void shouldRejectZipMissingExpectedAsset() throws IOException {
        ProductExportData data = data(List.of(asset("hero.png")));
        ExportToken token = token();
        StoredExport storedExport = storedExport(1024L);
        ExportIntegrityService service = service();
        when(exportStorageService.openStream(token)).thenReturn(new ByteArrayInputStream(zipBytes(requiredEntries())));

        assertThatThrownBy(() -> service.validateStoredZip(data, storedExport, token))
                .isInstanceOf(ProductExportException.class)
                .hasMessage("Stored ZIP is missing asset file hero.png");
    }

    @Test
    void shouldRejectZipWhenAssetCategoryMatchesButFileDoesNot() throws IOException {
        ProductExportData data = data(List.of(asset("hero.png")));
        ExportToken token = token();
        StoredExport storedExport = storedExport(1024L);
        ExportIntegrityService service = service();
        when(exportStorageService.openStream(token))
                .thenReturn(new ByteArrayInputStream(zipBytes(requiredEntries("assets/files/images/banner.png"))));

        assertThatThrownBy(() -> service.validateStoredZip(data, storedExport, token))
                .isInstanceOf(ProductExportException.class)
                .hasMessage("Stored ZIP is missing asset file hero.png");
    }

    @Test
    void shouldRejectEmptyStoredZip() throws IOException {
        ProductExportData data = data(List.of());
        ExportToken token = token();
        StoredExport storedExport = storedExport(0L);
        ExportIntegrityService service = service();
        when(exportStorageService.openStream(token)).thenReturn(new ByteArrayInputStream(zipBytes(requiredEntries())));

        assertThatThrownBy(() -> service.validateStoredZip(data, storedExport, token))
                .isInstanceOf(ProductExportException.class)
                .hasMessage("Stored ZIP is empty");
    }

    @Test
    void shouldRejectUnreadableStoredZip() {
        ProductExportData data = data(List.of());
        ExportToken token = token();
        StoredExport storedExport = storedExport(1024L);
        ExportIntegrityService service = service();
        when(exportStorageService.openStream(token)).thenReturn(new FailingInputStream());

        assertThatThrownBy(() -> service.validateStoredZip(data, storedExport, token))
                .isInstanceOf(ProductExportException.class)
                .hasMessage("Unable to validate stored ZIP");
    }

    private ExportIntegrityService service() {
        return new ExportIntegrityService(exportStorageService);
    }

    private ProductExportData data(List<ExportAssetFile> assets) {
        return new ProductExportData(
                TENANT_ID,
                PRODUCT_ID,
                "maestro-beton",
                "Maestro Beton",
                AssetStorageStrategy.LOCAL,
                "aegis-export.zip",
                Map.of(),
                assets,
                Map.of(),
                Map.of()
        );
    }

    private ExportAssetFile asset(String name) {
        return new ExportAssetFile(ASSET_ID, name, "images", "LOCAL", "assets/hero.png", 128L);
    }

    private ExportToken token() {
        ExportToken token = new ExportToken(PRODUCT_ID, "maestro-beton", "exports/token/aegis-export.zip", "local", "admin@byop.dev");
        token.markAvailable("exports/token/aegis-export.zip");
        return token;
    }

    private StoredExport storedExport(long sizeBytes) {
        return new StoredExport("local", "exports/token/aegis-export.zip", "aegis-export.zip", sizeBytes, null);
    }

    private List<String> requiredEntries(String... additionalEntries) {
        List<String> entries = new java.util.ArrayList<>(List.of(
                "manifest.json",
                "product.json",
                "modules.json",
                "content/entries.json",
                "pages/pages.json",
                "forms/forms.json",
                "forms/submissions.json",
                "assets/metadata.json",
                "knowledge-graph/nodes.json",
                "knowledge-graph/edges.json",
                "users/assignments.json",
                "audit/events.json"
        ));
        entries.addAll(List.of(additionalEntries));
        return entries;
    }

    private byte[] zipBytes(List<String> entries) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            for (String entry : entries) {
                zipOutputStream.putNextEntry(new ZipEntry(entry));
                zipOutputStream.write("{}".getBytes(StandardCharsets.UTF_8));
                zipOutputStream.closeEntry();
            }
        }
        return outputStream.toByteArray();
    }

    private static final class FailingInputStream extends InputStream {

        @Override
        public int read() throws IOException {
            throw new IOException("broken");
        }
    }
}
