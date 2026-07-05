package br.com.byop.aegis.product.export.service;

import br.com.byop.aegis.audit.api.AuditRecordCommand;
import br.com.byop.aegis.audit.api.AuditService;
import br.com.byop.aegis.product.api.ProductExportStoragePort;
import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.export.config.ExportAsyncConfig;
import br.com.byop.aegis.product.export.domain.ExportToken;
import br.com.byop.aegis.product.export.dto.ExportRecipient;
import br.com.byop.aegis.product.export.dto.ProductExportData;
import br.com.byop.aegis.product.export.dto.StoredExport;
import br.com.byop.aegis.product.export.repository.ExportTokenRepository;
import br.com.byop.aegis.product.repository.ProductRepository;
import br.com.byop.aegis.tenant.api.TenantExportRemovalPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class ExportAndDeleteService {

    private static final String ACTION_PRODUCT_DELETED = "USER_DATA_EXPORTED_AND_PRODUCT_DELETED";
    private static final String ACTION_PRODUCT_DELETED_WITHOUT_BACKUP = "USER_DATA_DELETED_WITHOUT_EXPORT_STORAGE_NOT_CONFIGURED";
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
    private final ProductExportStoragePort storagePort;
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
                                  ProductExportStoragePort storagePort,
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
        this.storagePort = storagePort;
        this.auditService = auditService;
    }

    /**
     * Exporta e exclui um produto de forma assíncrona.
     *
     * @param callerSubject subject do usuário que disparou a ação (para auditoria e manifesto)
     * @param callerEmail   e-mail do usuário que disparou a ação (para manifesto e ExportToken)
     * @param recipients    lista de destinatários que receberão o e-mail de conclusão/falha;
     *                      um e-mail é enviado por destinatário
     */
    @Async(ExportAsyncConfig.EXPORT_TASK_EXECUTOR)
    public void exportAndDelete(UUID productId, String callerSubject, String callerEmail,
                                List<ExportRecipient> recipients,
                                boolean deleteTenantWhenEmpty) {
        log.debug("exportAndDelete: productId='{}', destinatarios='{}', deleteTenantWhenEmpty={}",
                productId, recipients.size(), deleteTenantWhenEmpty);
        ProductExportData data;
        try {
            data = productExportSerializer.serialize(productId, callerSubject, callerEmail);
        } catch (Exception exception) {
            handleExportFailure(productId, recipients, exception);
            return;
        }

        if (!storagePort.isStorageConfigured(data.assetStorageStrategy())) {
            handleDeleteWithoutBackup(data, recipients, callerSubject, deleteTenantWhenEmpty);
            return;
        }

        try {
            Path zip = exportZipBuilder.build(data);
            ExportToken token = new ExportToken(productId, data.productKey(), "pending", data.assetStorageStrategy().name().toLowerCase(), callerEmail);
            StoredExport stored = exportStorageService.store(zip, token.getId(), data.filename(), data.assetStorageStrategy());
            token.markAvailable(stored.zipPath());
            exportIntegrityService.validateStoredZip(data, stored, token);
            exportTokenRepository.save(token);
            recipients.forEach(r -> productExportEmailService.sendExportReady(data, stored, token.getId(), r.email(), r.name()));
            log.info("exportAndDelete: export concluido productId='{}', exportTokenId='{}', destinatarios='{}'",
                    productId, token.getId(), recipients.size());
        } catch (Exception exception) {
            handleExportFailure(productId, recipients, exception);
            return;
        }

        try {
            productExportDeletionService.deleteExportedProduct(data);
            recordDeletionAudit(data, callerSubject, ACTION_PRODUCT_DELETED);
            deleteTenantIfReady(data, deleteTenantWhenEmpty);
            log.info("exportAndDelete: produto excluido apos export productId='{}'", productId);
        } catch (Exception exception) {
            handleDeleteFailure(productId, recipients, exception);
        }
    }

    /**
     * Exclui o produto sem tentar exportar quando o backend de storage da sua
     * estratégia não está configurado (ex.: S3 sem bucket neste ambiente) — sem
     * isto, {@code exportZipBuilder.build()} falharia ao ler os assets e o produto
     * ficaria preso em EXPORT_FAILED para sempre, sem nunca ser excluído.
     */
    private void handleDeleteWithoutBackup(ProductExportData data, List<ExportRecipient> recipients,
                                           String callerSubject, boolean deleteTenantWhenEmpty) {
        log.warn("exportAndDelete: armazenamento da estrategia '{}' nao configurado — excluindo productId='{}' sem backup de assets",
                data.assetStorageStrategy(), data.productId());
        try {
            productExportDeletionService.deleteExportedProductSkippingAssetFiles(data);
            recordDeletionAudit(data, callerSubject, ACTION_PRODUCT_DELETED_WITHOUT_BACKUP);
            deleteTenantIfReady(data, deleteTenantWhenEmpty);
            log.info("exportAndDelete: produto excluido sem backup productId='{}'", data.productId());
        } catch (Exception exception) {
            handleDeleteFailure(data.productId(), recipients, exception);
            return;
        }
        recipients.forEach(r -> {
            try {
                productExportEmailService.sendProductDeletedWithoutBackup(r.email(), r.name(), data.productName());
            } catch (Exception _) {
                log.warn("Unable to send no-backup warning e-mail for product {} to {}", data.productId(), r.email());
            }
        });
    }

    private void handleExportFailure(UUID productId, List<ExportRecipient> recipients, Exception exception) {
        log.error("Product export failed for product {}", productId, exception);
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return;
        }
        product.markExportFailed();
        productRepository.save(product);
        recipients.forEach(r -> {
            try {
                productExportEmailService.sendExportFailure(r.email(), product.getName());
            } catch (Exception _) {
                log.warn("Unable to send export failure e-mail for product {} to {}", productId, r.email());
            }
        });
    }

    private void handleDeleteFailure(UUID productId, List<ExportRecipient> recipients, Exception exception) {
        log.error("Product delete failed after export for product {}", productId, exception);
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return;
        }
        product.markDeleteFailed();
        productRepository.save(product);
        recipients.forEach(r -> {
            try {
                productExportEmailService.sendExportFailure(r.email(), product.getName());
            } catch (Exception _) {
                log.warn("Unable to send delete failure e-mail for product {} to {}", productId, r.email());
            }
        });
    }

    private void recordDeletionAudit(ProductExportData data, String callerSubject, String action) {
        auditService.recordEvent(new AuditRecordCommand(
                data.tenantId(),
                data.productId(),
                callerSubject,
                action,
                TARGET_TYPE_PRODUCT,
                data.productId().toString(),
                data.productName(),
                null,
                null,
                Map.of("exportTokenCreated", action.equals(ACTION_PRODUCT_DELETED))
        ));
    }

    private void deleteTenantIfReady(ProductExportData data, boolean deleteTenantWhenEmpty) {
        if (deleteTenantWhenEmpty && productRepository.findAllByTenantId(data.tenantId()).isEmpty()) {
            tenantExportRemovalPort.deleteTenantAfterExports(data.tenantId());
        }
    }
}
