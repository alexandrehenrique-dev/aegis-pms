package br.com.byop.aegis.product.export.service;

import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.domain.ProductAssignment;
import br.com.byop.aegis.product.domain.ProductAssignmentRole;
import br.com.byop.aegis.product.domain.ProductStatus;
import br.com.byop.aegis.product.domain.ProductTypeKey;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductDeleteServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PRODUCT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductAssignmentRepository assignmentRepository;

    @Mock
    private TenantAccessService tenantAccessService;

    @Mock
    private ExportAndDeleteService exportAndDeleteService;

    @Test
    void shouldStartProductExportAndDeleteForSuperAdmin() {
        Product product = product();
        AuthenticatedUser caller = user("super-subject", "ROLE_SUPER_ADMIN");
        DeleteProductRequest request = request("Maestro Beton");
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        ProductDeleteService service = service();

        DeleteAcceptedResponse result = service.deleteProduct(caller, PRODUCT_ID, request);

        assertThat(result.message()).contains("super-subject@byop.dev");
        assertThat(product.getStatus()).isEqualTo(ProductStatus.DELETING);
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        assertThat(productCaptor.getValue()).isEqualTo(product);
        verify(exportAndDeleteService).exportAndDelete(
                PRODUCT_ID,
                caller.subject(),
                caller.email(),
                caller.name(),
                false
        );
        verify(tenantAccessService, never()).hasActiveMembership(TENANT_ID, caller.subject());
    }

    @Test
    void shouldAllowTenantAdminWithActiveMembership() {
        Product product = product();
        AuthenticatedUser caller = user("tenant-admin", "ROLE_TENANT_ADMIN");
        DeleteProductRequest request = request("Maestro Beton");
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(tenantAccessService.hasActiveMembership(TENANT_ID, caller.subject())).thenReturn(true);

        service().deleteProduct(caller, PRODUCT_ID, request);

        verify(productRepository).save(product);
        verify(exportAndDeleteService).exportAndDelete(PRODUCT_ID, caller.subject(), caller.email(), caller.name(), false);
    }

    @Test
    void shouldAllowAssignedProductManager() {
        Product product = product();
        AuthenticatedUser caller = user("manager", "ROLE_USER");
        ProductAssignment assignment = new ProductAssignment(product, caller.subject(), ProductAssignmentRole.PRODUCT_MANAGER);
        DeleteProductRequest request = request("Maestro Beton");
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(assignmentRepository.findByProductIdAndUserSubject(PRODUCT_ID, caller.subject()))
                .thenReturn(Optional.of(assignment));

        service().deleteProduct(caller, PRODUCT_ID, request);

        verify(productRepository).save(product);
        verify(exportAndDeleteService).exportAndDelete(PRODUCT_ID, caller.subject(), caller.email(), caller.name(), false);
    }

    @Test
    void shouldRejectTenantAdminWithoutActiveMembership() {
        Product product = product();
        AuthenticatedUser caller = user("tenant-admin", "ROLE_TENANT_ADMIN");
        DeleteProductRequest request = request("Maestro Beton");
        ProductDeleteService service = service();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(tenantAccessService.hasActiveMembership(TENANT_ID, caller.subject())).thenReturn(false);
        when(assignmentRepository.findByProductIdAndUserSubject(PRODUCT_ID, caller.subject())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteProduct(caller, PRODUCT_ID, request))
                .isInstanceOf(ProductDeleteForbiddenException.class);

        verify(productRepository, never()).save(product);
    }

    @Test
    void shouldRejectAssignedNonManager() {
        Product product = product();
        AuthenticatedUser caller = user("editor", "ROLE_USER");
        ProductAssignment assignment = new ProductAssignment(product, caller.subject(), ProductAssignmentRole.EDITOR);
        DeleteProductRequest request = request("Maestro Beton");
        ProductDeleteService service = service();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(assignmentRepository.findByProductIdAndUserSubject(PRODUCT_ID, caller.subject()))
                .thenReturn(Optional.of(assignment));

        assertThatThrownBy(() -> service.deleteProduct(caller, PRODUCT_ID, request))
                .isInstanceOf(ProductDeleteForbiddenException.class);

        verify(productRepository, never()).save(product);
    }

    @Test
    void shouldRejectInactiveProductManagerAssignment() {
        Product product = product();
        AuthenticatedUser caller = user("manager", "ROLE_USER");
        ProductAssignment assignment = new ProductAssignment(product, caller.subject(), ProductAssignmentRole.PRODUCT_MANAGER);
        assignment.revoke();
        DeleteProductRequest request = request("Maestro Beton");
        ProductDeleteService service = service();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(assignmentRepository.findByProductIdAndUserSubject(PRODUCT_ID, caller.subject()))
                .thenReturn(Optional.of(assignment));

        assertThatThrownBy(() -> service.deleteProduct(caller, PRODUCT_ID, request))
                .isInstanceOf(ProductDeleteForbiddenException.class);

        verify(productRepository, never()).save(product);
    }

    @Test
    void shouldRejectWrongConfirmation() {
        Product product = product();
        AuthenticatedUser caller = user("super-subject", "ROLE_SUPER_ADMIN");
        DeleteProductRequest request = request("Wrong");
        ProductDeleteService service = service();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.deleteProduct(caller, PRODUCT_ID, request))
                .isInstanceOf(InvalidProductDeleteConfirmationException.class);

        verify(productRepository, never()).save(product);
        verify(exportAndDeleteService, never()).exportAndDelete(PRODUCT_ID, caller.subject(), caller.email(), caller.name(), false);
    }

    @Test
    void shouldRejectUnauthorizedCaller() {
        Product product = product();
        AuthenticatedUser caller = user("viewer", "ROLE_USER");
        DeleteProductRequest request = request("Maestro Beton");
        ProductDeleteService service = service();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(assignmentRepository.findByProductIdAndUserSubject(PRODUCT_ID, caller.subject())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteProduct(caller, PRODUCT_ID, request))
                .isInstanceOf(ProductDeleteForbiddenException.class);

        verify(productRepository, never()).save(product);
    }

    @Test
    void shouldRejectDeletingProduct() {
        Product product = product();
        product.markDeleting();
        AuthenticatedUser caller = user("super-subject", "ROLE_SUPER_ADMIN");
        DeleteProductRequest request = request("Maestro Beton");
        ProductDeleteService service = service();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.deleteProduct(caller, PRODUCT_ID, request))
                .isInstanceOf(ExportAlreadyInProgressException.class);

        verify(productRepository, never()).save(product);
    }

    @Test
    void shouldRejectExportFailedProduct() {
        Product product = product();
        product.markExportFailed();
        AuthenticatedUser caller = user("super-subject", "ROLE_SUPER_ADMIN");
        DeleteProductRequest request = request("Maestro Beton");
        ProductDeleteService service = service();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.deleteProduct(caller, PRODUCT_ID, request))
                .isInstanceOf(ExportAlreadyInProgressException.class);

        verify(productRepository, never()).save(product);
    }

    @Test
    void shouldRejectMissingProduct() {
        AuthenticatedUser caller = user("super-subject", "ROLE_SUPER_ADMIN");
        DeleteProductRequest request = request("Maestro Beton");
        ProductDeleteService service = service();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteProduct(caller, PRODUCT_ID, request))
                .isInstanceOf(ProductNotFoundException.class);
    }

    private ProductDeleteService service() {
        return new ProductDeleteService(productRepository, assignmentRepository, tenantAccessService, exportAndDeleteService);
    }

    private Product product() {
        Product product = new Product(TENANT_ID, "maestro-beton", "Maestro Beton", ProductTypeKey.SITE_INSTITUCIONAL, "pt-BR");
        ReflectionTestUtils.setField(product, "id", PRODUCT_ID);
        return product;
    }

    private DeleteProductRequest request(String confirmationText) {
        return new DeleteProductRequest(confirmationText);
    }

    private AuthenticatedUser user(String subject, String authority) {
        return new AuthenticatedUser(subject, subject + "@byop.dev", subject, subject, Set.of(authority));
    }
}
