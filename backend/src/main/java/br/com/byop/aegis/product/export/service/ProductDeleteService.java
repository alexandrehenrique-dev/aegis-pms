package br.com.byop.aegis.product.export.service;

import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.domain.ProductAssignment;
import br.com.byop.aegis.product.domain.ProductAssignmentRole;
import br.com.byop.aegis.product.domain.ProductAssignmentStatus;
import br.com.byop.aegis.product.domain.ProductStatus;
import br.com.byop.aegis.product.exception.ProductNotFoundException;
import br.com.byop.aegis.product.export.contract.DeleteProductRequest;
import br.com.byop.aegis.product.export.dto.DeleteAcceptedResponse;
import br.com.byop.aegis.product.export.exception.ExportAlreadyInProgressException;
import br.com.byop.aegis.product.export.exception.InvalidProductDeleteConfirmationException;
import br.com.byop.aegis.product.export.exception.ProductDeleteForbiddenException;
import br.com.byop.aegis.product.repository.ProductAssignmentRepository;
import br.com.byop.aegis.product.repository.ProductRepository;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.tenant.api.TenantAccessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
public class ProductDeleteService {

    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String ROLE_TENANT_ADMIN = "ROLE_TENANT_ADMIN";

    private final ProductRepository productRepository;
    private final ProductAssignmentRepository assignmentRepository;
    private final TenantAccessService tenantAccessService;
    private final ExportAndDeleteService exportAndDeleteService;

    public ProductDeleteService(ProductRepository productRepository, ProductAssignmentRepository assignmentRepository,
                                TenantAccessService tenantAccessService, ExportAndDeleteService exportAndDeleteService) {
        this.productRepository = productRepository;
        this.assignmentRepository = assignmentRepository;
        this.tenantAccessService = tenantAccessService;
        this.exportAndDeleteService = exportAndDeleteService;
    }

    @Transactional
    public DeleteAcceptedResponse deleteProduct(AuthenticatedUser caller, UUID productId, DeleteProductRequest request) {
        log.debug("deleteProduct: productId='{}'", productId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        if (!product.getName().equals(request.confirmationText())) {
            log.warn("deleteProduct: confirmacao invalida para productId='{}'", productId);
            throw new InvalidProductDeleteConfirmationException();
        }
        assertAuthorized(caller, product);
        assertDeletionCanStart(product);
        product.markDeleting();
        productRepository.save(product);
        exportAndDeleteService.exportAndDelete(productId, caller.subject(), caller.email(), caller.name(), false);
        log.info("deleteProduct: exclusao iniciada productId='{}'", productId);
        return new DeleteAcceptedResponse("Exportação iniciada. Um link de download será enviado para %s em instantes."
                .formatted(caller.email()));
    }

    private void assertAuthorized(AuthenticatedUser caller, Product product) {
        if (caller.authorities().contains(ROLE_SUPER_ADMIN)) {
            return;
        }
        if (caller.authorities().contains(ROLE_TENANT_ADMIN)
                && tenantAccessService.hasActiveMembership(product.getTenantId(), caller.subject())) {
            return;
        }
        boolean productManager = assignmentRepository.findByProductIdAndUserSubject(product.getId(), caller.subject())
                .filter(this::isAssignedProductManager)
                .isPresent();
        if (!productManager) {
            log.warn("assertAuthorized: acesso negado para exclusao productId='{}'", product.getId());
            throw new ProductDeleteForbiddenException();
        }
    }

    private boolean isAssignedProductManager(ProductAssignment assignment) {
        return assignment.getStatus() == ProductAssignmentStatus.ASSIGNED
                && assignment.getRole() == ProductAssignmentRole.PRODUCT_MANAGER;
    }

    private void assertDeletionCanStart(Product product) {
        if (product.getStatus() == ProductStatus.DELETING || product.getStatus() == ProductStatus.EXPORT_FAILED) {
            log.warn("assertDeletionCanStart: exclusao ja em andamento productId='{}'", product.getId());
            throw new ExportAlreadyInProgressException();
        }
    }
}
