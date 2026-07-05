package br.com.byop.aegis.product.export.service;

import br.com.byop.aegis.identity.api.IdentityUser;
import br.com.byop.aegis.identity.api.IdentityUserDirectory;
import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.domain.ProductAssignment;
import br.com.byop.aegis.product.domain.ProductAssignmentRole;
import br.com.byop.aegis.product.domain.ProductStatus;
import br.com.byop.aegis.product.domain.ProductTypeKey;
import br.com.byop.aegis.product.export.dto.ExportRecipient;
import br.com.byop.aegis.product.repository.ProductAssignmentRepository;
import br.com.byop.aegis.product.repository.ProductRepository;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.tenant.api.TenantExportRemovalPort;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductExportCoordinatorServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ACTIVE_PRODUCT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID DELETED_PRODUCT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductAssignmentRepository assignmentRepository;

    @Mock
    private IdentityUserDirectory identityUserDirectory;

    @Mock
    private ExportAndDeleteService exportAndDeleteService;

    @Mock
    private TenantExportRemovalPort tenantExportRemovalPort;

    @Test
    void shouldRemoveTenantImmediatelyWhenThereAreNoProductsToExport() {
        AuthenticatedUser caller = caller();
        when(productRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of());

        service().startTenantProductExports(TENANT_ID, caller);

        verify(tenantExportRemovalPort).deleteTenantAfterExports(TENANT_ID);
        verify(exportAndDeleteService, never()).exportAndDelete(any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void shouldStartExportsForTenantProductsThatAreNotDeleted() {
        Product activeProduct = product(ACTIVE_PRODUCT_ID, "active");
        Product deletedProduct = product(DELETED_PRODUCT_ID, "deleted");
        deletedProduct.markDeleted();
        AuthenticatedUser caller = caller();
        when(productRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(activeProduct, deletedProduct));
        // assignmentRepository returns empty by default (no PRODUCT_MANAGER) → recipient falls back to caller

        service().startTenantProductExports(TENANT_ID, caller);

        assertThat(activeProduct.getStatus()).isEqualTo(ProductStatus.DELETING);
        verify(productRepository).save(activeProduct);
        verify(productRepository, never()).save(deletedProduct);
        // Sem PRODUCT_MANAGER atribuído, recipients = [caller] (fallback)
        verify(exportAndDeleteService).exportAndDelete(
                ACTIVE_PRODUCT_ID,
                caller.subject(),
                caller.email(),
                List.of(new ExportRecipient(caller.email(), caller.name())),
                true
        );
        // Produto já deletado não deve disparar exportação
        verify(exportAndDeleteService, never()).exportAndDelete(
                eq(DELETED_PRODUCT_ID), any(), any(), any(), anyBoolean()
        );
    }

    @Test
    void shouldUseAssignedProductManagerAsRecipient() {
        Product product = product(ACTIVE_PRODUCT_ID, "active");
        AuthenticatedUser caller = caller();
        ProductAssignment manager = new ProductAssignment(product, "manager-subject", ProductAssignmentRole.PRODUCT_MANAGER);
        IdentityUser managerUser = new IdentityUser("manager-subject", "manager", "manager@byop.dev", "Manager", "User");
        when(productRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(product));
        when(assignmentRepository.findAllByProductId(ACTIVE_PRODUCT_ID)).thenReturn(List.of(manager));
        when(identityUserDirectory.getRequiredUser("manager-subject")).thenReturn(managerUser);

        service().startTenantProductExports(TENANT_ID, caller);

        verify(exportAndDeleteService).exportAndDelete(
                ACTIVE_PRODUCT_ID,
                caller.subject(),
                caller.email(),
                List.of(new ExportRecipient(managerUser.email(), managerUser.displayName())),
                true
        );
    }

    @Test
    void shouldFallBackToCallerWhenProductManagerCannotBeResolved() {
        Product product = product(ACTIVE_PRODUCT_ID, "active");
        AuthenticatedUser caller = caller();
        ProductAssignment manager = new ProductAssignment(product, "manager-subject", ProductAssignmentRole.PRODUCT_MANAGER);
        ProductAssignment editor = new ProductAssignment(product, "editor-subject", ProductAssignmentRole.EDITOR);
        ProductAssignment revokedManager = new ProductAssignment(product, "revoked-manager-subject", ProductAssignmentRole.PRODUCT_MANAGER);
        revokedManager.revoke();
        when(productRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(product));
        when(assignmentRepository.findAllByProductId(ACTIVE_PRODUCT_ID))
                .thenReturn(List.of(manager, editor, revokedManager));
        when(identityUserDirectory.getRequiredUser("manager-subject")).thenThrow(new RuntimeException("not found"));

        service().startTenantProductExports(TENANT_ID, caller);

        verify(exportAndDeleteService).exportAndDelete(
                ACTIVE_PRODUCT_ID,
                caller.subject(),
                caller.email(),
                List.of(new ExportRecipient(caller.email(), caller.name())),
                true
        );
    }

    private ProductExportCoordinatorService service() {
        return new ProductExportCoordinatorService(productRepository, assignmentRepository, identityUserDirectory,
                exportAndDeleteService, tenantExportRemovalPort);
    }

    private Product product(UUID productId, String key) {
        Product product = new Product(TENANT_ID, key, "Product " + key, ProductTypeKey.SITE_INSTITUCIONAL, "pt-BR");
        ReflectionTestUtils.setField(product, "id", productId);
        return product;
    }

    private AuthenticatedUser caller() {
        return new AuthenticatedUser("subject", "subject@byop.dev", "Subject", "subject", Set.of("ROLE_SUPER_ADMIN"));
    }
}
