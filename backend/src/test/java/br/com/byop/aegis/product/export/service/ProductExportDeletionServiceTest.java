package br.com.byop.aegis.product.export.service;

import br.com.byop.aegis.product.api.AssetStorageStrategy;
import br.com.byop.aegis.product.api.ProductExportStoragePort;
import br.com.byop.aegis.product.export.dto.ExportAssetFile;
import br.com.byop.aegis.product.export.dto.ProductExportData;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductExportDeletionServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PRODUCT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private JdbcClient jdbcClient;

    @Mock
    private JdbcClient.StatementSpec statementSpec;

    @Mock
    private ProductExportStoragePort storagePort;

    @Test
    void shouldDeleteAssetsAndProductRows() {
        ProductExportData data = data();
        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param("productId", PRODUCT_ID)).thenReturn(statementSpec);
        when(statementSpec.update()).thenReturn(1);

        service().deleteExportedProduct(data);

        verify(storagePort).deleteAsset("local", "local/hero.png");
        verify(storagePort).deleteAsset("s3", "s3/banner.png");
        verify(jdbcClient, times(19)).sql(anyString());
        verify(statementSpec, times(19)).param("productId", PRODUCT_ID);
        verify(statementSpec, times(19)).update();
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcClient, times(19)).sql(sqlCaptor.capture());
        assertThat(sqlCaptor.getAllValues())
                .noneMatch(sql -> sql.contains("tenant_memberships"))
                .noneMatch(sql -> sql.contains("user_entity"))
                .noneMatch(sql -> sql.matches("(?i).*delete\\s+from\\s+users.*"));
    }

    @Test
    void shouldDeleteProductRowsWithoutTouchingAssetFiles() {
        ProductExportData data = data();
        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param("productId", PRODUCT_ID)).thenReturn(statementSpec);
        when(statementSpec.update()).thenReturn(1);

        service().deleteExportedProductSkippingAssetFiles(data);

        verify(storagePort, org.mockito.Mockito.never()).deleteAsset(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
        verify(jdbcClient, times(19)).sql(anyString());
        verify(statementSpec, times(19)).param("productId", PRODUCT_ID);
        verify(statementSpec, times(19)).update();
    }

    private ProductExportDeletionService service() {
        return new ProductExportDeletionService(jdbcClient, storagePort);
    }

    private ProductExportData data() {
        return new ProductExportData(
                TENANT_ID,
                PRODUCT_ID,
                "maestro-beton",
                "Maestro Beton",
                AssetStorageStrategy.LOCAL,
                "aegis-export.zip",
                Map.of(),
                List.of(
                        new ExportAssetFile("asset-local", "hero.png", "images", "local", "local/hero.png", 100L),
                        new ExportAssetFile("asset-s3", "banner.png", "images", "s3", "s3/banner.png", 100L)
                ),
                Map.of(),
                Map.of()
        );
    }
}
