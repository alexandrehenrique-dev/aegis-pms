package br.com.byop.aegis.product.export.service;

import br.com.byop.aegis.audit.api.AuditService;
import br.com.byop.aegis.product.api.AssetStorageStrategy;
import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.domain.ProductStatus;
import br.com.byop.aegis.product.domain.ProductTypeKey;
import br.com.byop.aegis.product.export.domain.ExportToken;
import br.com.byop.aegis.product.export.dto.ExportRecipient;
import br.com.byop.aegis.product.export.dto.ProductExportData;
import br.com.byop.aegis.product.export.dto.StoredExport;
import br.com.byop.aegis.product.export.repository.ExportTokenRepository;
import br.com.byop.aegis.product.repository.ProductRepository;
import br.com.byop.aegis.tenant.api.TenantExportRemovalPort;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportAndDeleteServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PRODUCT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String CALLER_SUBJECT = "admin-subject";
    private static final String CALLER_EMAIL = "admin@byop.dev";
    private static final String CALLER_NAME = "Admin";

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductExportSerializer productExportSerializer;

    @Mock
    private ExportZipBuilder exportZipBuilder;

    @Mock
    private ExportStorageService exportStorageService;

    @Mock
    private ExportTokenRepository exportTokenRepository;

    @Mock
    private ProductExportEmailService productExportEmailService;

    @Mock
    private ProductExportDeletionService productExportDeletionService;

    @Mock
    private ExportIntegrityService exportIntegrityService;

    @Mock
    private TenantExportRemovalPort tenantExportRemovalPort;

    @Mock
    private AuditService auditService;

    @Test
    void shouldExportStoreValidateNotifyDeleteAuditAndRemoveTenantWhenLastProduct() {
        ProductExportData data = data();
        Path zip = Path.of("target/aegis-export.zip");
        StoredExport storedExport = storedExport();
        when(productExportSerializer.serialize(PRODUCT_ID, CALLER_SUBJECT, CALLER_EMAIL)).thenReturn(data);
        when(exportZipBuilder.build(data)).thenReturn(zip);
        when(exportStorageService.store(eq(zip), any(UUID.class), eq(data.filename()), eq(AssetStorageStrategy.LOCAL)))
                .thenReturn(storedExport);
        when(productRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of());

        service().exportAndDelete(PRODUCT_ID, CALLER_SUBJECT, CALLER_EMAIL, List.of(new ExportRecipient(CALLER_EMAIL, CALLER_NAME)), true);

        ArgumentCaptor<ExportToken> tokenCaptor = ArgumentCaptor.forClass(ExportToken.class);
        InOrder order = inOrder(exportIntegrityService, exportTokenRepository, productExportEmailService, productExportDeletionService);
        order.verify(exportIntegrityService).validateStoredZip(eq(data), eq(storedExport), tokenCaptor.capture());
        order.verify(exportTokenRepository).save(tokenCaptor.getValue());
        order.verify(productExportEmailService).sendExportReady(data, storedExport, tokenCaptor.getValue().getId(), CALLER_EMAIL, CALLER_NAME);
        order.verify(productExportDeletionService).deleteExportedProduct(data);
        assertThat(tokenCaptor.getValue().getStatus().name()).isEqualTo("AVAILABLE");
        assertThat(tokenCaptor.getValue().getZipPath()).isEqualTo(storedExport.zipPath());
        verify(auditService).recordEvent(any());
        verify(tenantExportRemovalPort).deleteTenantAfterExports(TENANT_ID);
    }

    @Test
    void shouldNotRemoveTenantWhenProductsRemain() {
        ProductExportData data = data();
        Path zip = Path.of("target/aegis-export.zip");
        Product remainingProduct = product();
        when(productExportSerializer.serialize(PRODUCT_ID, CALLER_SUBJECT, CALLER_EMAIL)).thenReturn(data);
        when(exportZipBuilder.build(data)).thenReturn(zip);
        when(exportStorageService.store(eq(zip), any(UUID.class), eq(data.filename()), eq(AssetStorageStrategy.LOCAL)))
                .thenReturn(storedExport());
        when(productRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(remainingProduct));

        service().exportAndDelete(PRODUCT_ID, CALLER_SUBJECT, CALLER_EMAIL, List.of(new ExportRecipient(CALLER_EMAIL, CALLER_NAME)), true);

        verify(tenantExportRemovalPort, never()).deleteTenantAfterExports(TENANT_ID);
    }

    @Test
    void shouldNotCheckTenantRemovalWhenTenantDeleteWasNotRequested() {
        ProductExportData data = data();
        Path zip = Path.of("target/aegis-export.zip");
        when(productExportSerializer.serialize(PRODUCT_ID, CALLER_SUBJECT, CALLER_EMAIL)).thenReturn(data);
        when(exportZipBuilder.build(data)).thenReturn(zip);
        when(exportStorageService.store(eq(zip), any(UUID.class), eq(data.filename()), eq(AssetStorageStrategy.LOCAL)))
                .thenReturn(storedExport());

        service().exportAndDelete(PRODUCT_ID, CALLER_SUBJECT, CALLER_EMAIL, List.of(new ExportRecipient(CALLER_EMAIL, CALLER_NAME)), false);

        verify(productRepository, never()).findAllByTenantId(TENANT_ID);
        verify(tenantExportRemovalPort, never()).deleteTenantAfterExports(TENANT_ID);
    }

    @Test
    void shouldMarkProductAsExportFailedWhenExportFails() {
        Product product = product();
        when(productExportSerializer.serialize(PRODUCT_ID, CALLER_SUBJECT, CALLER_EMAIL))
                .thenThrow(new IllegalStateException("serialize failed"));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        service().exportAndDelete(PRODUCT_ID, CALLER_SUBJECT, CALLER_EMAIL, List.of(new ExportRecipient(CALLER_EMAIL, CALLER_NAME)), false);

        assertThat(product.getStatus()).isEqualTo(ProductStatus.EXPORT_FAILED);
        verify(productRepository).save(product);
        verify(productExportEmailService).sendExportFailure(CALLER_EMAIL, product.getName());
        verify(productExportDeletionService, never()).deleteExportedProduct(any(ProductExportData.class));
    }

    @Test
    void shouldSwallowFailureEmailErrorAfterExportFailure() {
        Product product = product();
        when(productExportSerializer.serialize(PRODUCT_ID, CALLER_SUBJECT, CALLER_EMAIL))
                .thenThrow(new IllegalStateException("serialize failed"));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        doThrow(new IllegalStateException("mail failed"))
                .when(productExportEmailService)
                .sendExportFailure(CALLER_EMAIL, product.getName());

        service().exportAndDelete(PRODUCT_ID, CALLER_SUBJECT, CALLER_EMAIL, List.of(new ExportRecipient(CALLER_EMAIL, CALLER_NAME)), false);

        assertThat(product.getStatus()).isEqualTo(ProductStatus.EXPORT_FAILED);
        verify(productRepository).save(product);
    }

    @Test
    void shouldIgnoreExportFailureWhenProductNoLongerExists() {
        when(productExportSerializer.serialize(PRODUCT_ID, CALLER_SUBJECT, CALLER_EMAIL))
                .thenThrow(new IllegalStateException("serialize failed"));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        service().exportAndDelete(PRODUCT_ID, CALLER_SUBJECT, CALLER_EMAIL, List.of(new ExportRecipient(CALLER_EMAIL, CALLER_NAME)), false);

        verify(productRepository, never()).save(any(Product.class));
        verify(productExportEmailService, never()).sendExportFailure(eq(CALLER_EMAIL), any(String.class));
    }

    @Test
    void shouldMarkProductAsDeleteFailedWhenDeletionFails() {
        Product product = product();
        ProductExportData data = data();
        Path zip = Path.of("target/aegis-export.zip");
        when(productExportSerializer.serialize(PRODUCT_ID, CALLER_SUBJECT, CALLER_EMAIL)).thenReturn(data);
        when(exportZipBuilder.build(data)).thenReturn(zip);
        when(exportStorageService.store(eq(zip), any(UUID.class), eq(data.filename()), eq(AssetStorageStrategy.LOCAL)))
                .thenReturn(storedExport());
        doThrow(new IllegalStateException("delete failed")).when(productExportDeletionService).deleteExportedProduct(data);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        service().exportAndDelete(PRODUCT_ID, CALLER_SUBJECT, CALLER_EMAIL, List.of(new ExportRecipient(CALLER_EMAIL, CALLER_NAME)), false);

        assertThat(product.getStatus()).isEqualTo(ProductStatus.DELETE_FAILED);
        verify(productRepository).save(product);
        verify(productExportEmailService).sendExportFailure(CALLER_EMAIL, product.getName());
        verify(auditService, never()).recordEvent(any());
    }

    @Test
    void shouldSwallowFailureEmailErrorAfterDeleteFailure() {
        Product product = product();
        ProductExportData data = data();
        Path zip = Path.of("target/aegis-export.zip");
        when(productExportSerializer.serialize(PRODUCT_ID, CALLER_SUBJECT, CALLER_EMAIL)).thenReturn(data);
        when(exportZipBuilder.build(data)).thenReturn(zip);
        when(exportStorageService.store(eq(zip), any(UUID.class), eq(data.filename()), eq(AssetStorageStrategy.LOCAL)))
                .thenReturn(storedExport());
        doThrow(new IllegalStateException("delete failed")).when(productExportDeletionService).deleteExportedProduct(data);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        doThrow(new IllegalStateException("mail failed"))
                .when(productExportEmailService)
                .sendExportFailure(CALLER_EMAIL, product.getName());

        service().exportAndDelete(PRODUCT_ID, CALLER_SUBJECT, CALLER_EMAIL, List.of(new ExportRecipient(CALLER_EMAIL, CALLER_NAME)), false);

        assertThat(product.getStatus()).isEqualTo(ProductStatus.DELETE_FAILED);
        verify(productRepository).save(product);
    }

    @Test
    void shouldIgnoreDeleteFailureWhenProductNoLongerExists() {
        ProductExportData data = data();
        Path zip = Path.of("target/aegis-export.zip");
        when(productExportSerializer.serialize(PRODUCT_ID, CALLER_SUBJECT, CALLER_EMAIL)).thenReturn(data);
        when(exportZipBuilder.build(data)).thenReturn(zip);
        when(exportStorageService.store(eq(zip), any(UUID.class), eq(data.filename()), eq(AssetStorageStrategy.LOCAL)))
                .thenReturn(storedExport());
        doThrow(new IllegalStateException("delete failed")).when(productExportDeletionService).deleteExportedProduct(data);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        service().exportAndDelete(PRODUCT_ID, CALLER_SUBJECT, CALLER_EMAIL, List.of(new ExportRecipient(CALLER_EMAIL, CALLER_NAME)), false);

        verify(productRepository, never()).save(any(Product.class));
        verify(productExportEmailService, never()).sendExportFailure(eq(CALLER_EMAIL), any(String.class));
    }

    private ExportAndDeleteService service() {
        return new ExportAndDeleteService(
                productRepository,
                productExportSerializer,
                exportZipBuilder,
                exportStorageService,
                exportTokenRepository,
                productExportEmailService,
                productExportDeletionService,
                exportIntegrityService,
                tenantExportRemovalPort,
                auditService
        );
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
                List.of(),
                Map.of(),
                Map.of()
        );
    }

    private StoredExport storedExport() {
        return new StoredExport("local", "exports/token/aegis-export.zip", "aegis-export.zip", 1024L, Path.of("exports/token/aegis-export.zip"));
    }

    private Product product() {
        Product product = new Product(TENANT_ID, "maestro-beton", "Maestro Beton", ProductTypeKey.SITE_INSTITUCIONAL, "pt-BR");
        org.springframework.test.util.ReflectionTestUtils.setField(product, "id", PRODUCT_ID);
        return product;
    }
}
