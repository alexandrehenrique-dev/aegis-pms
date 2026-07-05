package br.com.byop.aegis.product.export.service;

import br.com.byop.aegis.identity.api.IdentityUser;
import br.com.byop.aegis.identity.api.IdentityUserDirectory;
import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.domain.ProductAssignmentRole;
import br.com.byop.aegis.product.domain.ProductAssignmentStatus;
import br.com.byop.aegis.product.domain.ProductStatus;
import br.com.byop.aegis.product.export.dto.ExportRecipient;
import br.com.byop.aegis.product.repository.ProductAssignmentRepository;
import br.com.byop.aegis.product.repository.ProductRepository;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.tenant.api.TenantExportRemovalPort;
import br.com.byop.aegis.tenant.api.TenantProductExportPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ProductExportCoordinatorService implements TenantProductExportPort {

    private final ProductRepository productRepository;
    private final ProductAssignmentRepository assignmentRepository;
    private final IdentityUserDirectory identityUserDirectory;
    private final ExportAndDeleteService exportAndDeleteService;
    private final TenantExportRemovalPort tenantExportRemovalPort;

    public ProductExportCoordinatorService(ProductRepository productRepository,
                                           ProductAssignmentRepository assignmentRepository,
                                           IdentityUserDirectory identityUserDirectory,
                                           ExportAndDeleteService exportAndDeleteService,
                                           TenantExportRemovalPort tenantExportRemovalPort) {
        this.productRepository = productRepository;
        this.assignmentRepository = assignmentRepository;
        this.identityUserDirectory = identityUserDirectory;
        this.exportAndDeleteService = exportAndDeleteService;
        this.tenantExportRemovalPort = tenantExportRemovalPort;
    }

    @Override
    @Transactional
    public void startTenantProductExports(UUID tenantId, AuthenticatedUser caller) {
        log.debug("startTenantProductExports: tenantId='{}'", tenantId);
        List<Product> products = productRepository.findAllByTenantId(tenantId).stream()
                .filter(product -> product.getStatus() != ProductStatus.DELETED)
                .toList();
        if (products.isEmpty()) {
            // Sem produtos para exportar, o loop abaixo nunca executaria — e e
            // exatamente a conclusao de "todos os produtos exportados" (verificada
            // em ExportAndDeleteService.deleteTenantIfReady) que remove o tenant.
            // Sem este atalho, um tenant sem produtos ficava com a exclusao
            // "aceita" (202) mas nunca de fato removido.
            tenantExportRemovalPort.deleteTenantAfterExports(tenantId);
            log.info("startTenantProductExports: tenantId='{}' sem produtos — removido imediatamente", tenantId);
            return;
        }
        products.forEach(product -> startProductExport(product, caller));
        log.info("startTenantProductExports: tenantId='{}', produtosProcessados='{}'", tenantId, products.size());
    }

    private void startProductExport(Product product, AuthenticatedUser caller) {
        product.markDeleting();
        productRepository.save(product);
        List<ExportRecipient> recipients = resolveRecipients(product.getId(), caller);
        exportAndDeleteService.exportAndDelete(product.getId(), caller.subject(), caller.email(),
                recipients, true);
        log.info("startProductExport: exportacao iniciada productId='{}', destinatarios='{}'",
                product.getId(), recipients.size());
    }

    /**
     * Resolve todos os destinatários do e-mail de exportação para um produto.
     *
     * <p>Todos os PRODUCT_MANAGERs com status ASSIGNED recebem o e-mail.
     * Se nenhum for encontrado, o caller (super admin) recebe como fallback.
     *
     * @param productId identificador do produto
     * @param caller    usuário que disparou a exclusão do tenant
     * @return lista de destinatários; nunca vazia
     */
    private List<ExportRecipient> resolveRecipients(UUID productId, AuthenticatedUser caller) {
        List<ExportRecipient> recipients = assignmentRepository.findAllByProductId(productId).stream()
                .filter(a -> a.getRole() == ProductAssignmentRole.PRODUCT_MANAGER
                        && a.getStatus() == ProductAssignmentStatus.ASSIGNED)
                .map(a -> {
                    try {
                        IdentityUser user = identityUserDirectory.getRequiredUser(a.getUserSubject());
                        return new ExportRecipient(user.email(), user.displayName());
                    } catch (Exception _) {
                        log.warn("resolveRecipients: nao foi possivel resolver usuario productId='{}', subject='{}' — ignorando",
                                productId, a.getUserSubject());
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .toList();

        if (recipients.isEmpty()) {
            log.debug("resolveRecipients: sem PRODUCT_MANAGER ativo para productId='{}' — usando caller", productId);
            return List.of(new ExportRecipient(caller.email(), caller.name()));
        }
        return recipients;
    }
}
