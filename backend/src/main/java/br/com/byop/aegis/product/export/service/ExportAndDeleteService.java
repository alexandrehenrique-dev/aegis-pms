package br.com.byop.aegis.product.export.service;

import br.com.byop.aegis.audit.api.AuditRecordCommand;
import br.com.byop.aegis.audit.api.AuditService;
import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.export.config.ExportAsyncConfig;
import br.com.byop.aegis.product.export.domain.ExportToken;
import br.com.byop.aegis.product.export.dto.ProductExportData;
import br.com.byop.aegis.product.export.dto.StoredExport;
import br.com.byop.aegis.product.export.repository.ExportTokenRepository;
import br.com.byop.aegis.product.repository.ProductRepository;
import br.com.byop.aegis.tenant.api.TenantExportRemovalPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

@Service
public class ExportAndDeleteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExportAndDeleteService.class);
    private static final String ACTION_PRODUCT_DELETED = "USER_DATA_EXPORTED_AND_PRODUCT_DELETED";
    private static final String TARGET_TYPE_PRODUCT = "Product";

    private final ProductRepository productRepository;
    private final ProductExportSerializer productExportSerializer;
    private final ExportZipBuilder exportZipBuilder;
    private final ExportStorageService exportStorageService;
    private final ExportTokenRepository exportTokenRepository;
    private final ProductExportEmailService productExportEmailService;
    private final ProductExportDeletionService productExportDeletionService;
    private final ExportIntegrityService exportIntegrityService;
    private final TenantExportRemovalPort tenantExportRemovalPort;
    private final AuditService auditService;

    public ExportAndDeleteService(ProductRepository productRepository,
                                  ProductExportSerializer productExportSerializer,
                                  ExportZipBuilder exportZipBuilder,
                                  ExportStorageService exportStorageService,
                                  ExportTokenRepository exportTokenRepository,
                                  ProductExportEmailService productExportEmailService,
                                  ProductExportDeletionService productExportDeletionService,
                                  ExportIntegrityService exportIntegrityService,
                                  TenantExportRemovalPort tenantExportRemovalPort,
                                  AuditService auditService) {
        this.productRepository = productRepository;
        this.productExportSerializer = productExportSerializer;
        this.exportZipBuilder = exportZipBuilder;
        this.exportStorageService = exportStorageService;
        this.exportTokenRepository = exportTokenRepository;
        this.productExportEmailService = productExportEmailService;
        this.productExportDeletionService = productExportDeletionService;
        this.exportIntegrityService = exportIntegrityService;
        this.tenantExportRemovalPort = tenantExportRemovalPort;
        this.auditService = auditService;
    }

    @Async(ExportAsyncConfig.EXPORT_TASK_EXECUTOR)
    public void exportAndDelete(UUID productId, String callerSubject, String callerEmail, String callerName,
                                boolean deleteTenantWhenEmpty) {
        ProductExportData data;
        try {
            data = productExportSerializer.serialize(productId, callerSubject, callerEmail);
            Path zip = exportZipBuilder.build(data);
            ExportToken token = new ExportToken(productId, data.productKey(), "pending", data.assetStorageStrategy().name().toLowerCase(), callerEmail);
            StoredExport stored = exportStorageService.store(zip, token.getId(), data.filename(), data.assetStorageStrategy());
            token.markAvailable(stored.zipPath());
            exportIntegrityService.validateStoredZip(data, stored, token);
            exportTokenRepository.save(token);
            productExportEmailService.sendExportReady(data, stored, token.getId(), callerEmail, callerName);
        } catch (Exception exception) {
            handleExportFailure(productId, callerEmail, exception);
            return;
        }

        try {
            productExportDeletionService.deleteExportedProduct(data);
            recordDeletionAudit(data, callerSubject);
            deleteTenantIfReady(data, deleteTenantWhenEmpty);
        } catch (Exception exception) {
            handleDeleteFailure(productId, callerEmail, exception);
        }
    }

    private void handleExportFailure(UUID productId, String callerEmail, Exception exception) {
        LOGGER.error("Product export failed for product {}", productId, exception);
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return;
        }
        product.markExportFailed();
        productRepository.save(product);
        try {
            productExportEmailService.sendExportFailure(callerEmail, product.getName());
        } catch (Exception emailException) {
            LOGGER.warn("Unable to send export failure e-mail for product {}", productId, emailException);
        }
    }

    private void handleDeleteFailure(UUID productId, String callerEmail, Exception exception) {
        LOGGER.error("Product delete failed after export for product {}", productId, exception);
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return;
        }
        product.markDeleteFailed();
        productRepository.save(product);
        try {
            productExportEmailService.sendExportFailure(callerEmail, product.getName());
        } catch (Exception emailException) {
            LOGGER.warn("Unable to send delete failure e-mail for product {}", productId, emailException);
        }
    }

    private void recordDeletionAudit(ProductExportData data, String callerSubject) {
        auditService.recordEvent(new AuditRecordCommand(
                data.tenantId(),
                data.productId(),
                callerSubject,
                ACTION_PRODUCT_DELETED,
                TARGET_TYPE_PRODUCT,
                data.productId().toString(),
                data.productName(),
                null,
                null,
                Map.of("exportTokenCreated", true)
        ));
    }

    private void deleteTenantIfReady(ProductExportData data, boolean deleteTenantWhenEmpty) {
        if (deleteTenantWhenEmpty && productRepository.findAllByTenantId(data.tenantId()).isEmpty()) {
            tenantExportRemovalPort.deleteTenantAfterExports(data.tenantId());
        }
    }
}
