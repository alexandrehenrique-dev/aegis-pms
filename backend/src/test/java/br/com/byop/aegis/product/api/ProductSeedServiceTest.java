package br.com.byop.aegis.product.api;

import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.domain.ProductAssignment;
import br.com.byop.aegis.product.domain.ProductAssignmentRole;
import br.com.byop.aegis.product.domain.ProductAssignmentStatus;
import br.com.byop.aegis.product.domain.ProductModule;
import br.com.byop.aegis.product.domain.ProductStatus;
import br.com.byop.aegis.product.domain.ProductTypeKey;
import br.com.byop.aegis.product.repository.ProductAssignmentRepository;
import br.com.byop.aegis.product.repository.ProductModuleRepository;
import br.com.byop.aegis.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductSeedServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductModuleRepository moduleRepository;

    @Mock
    private ProductAssignmentRepository assignmentRepository;

    @InjectMocks
    private ProductSeedService service;

    @Test
    void shouldCreateActiveProductAndSynchronizeModules() {
        UUID tenantId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(productRepository.findByTenantIdAndKey(tenantId, "wikidev")).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            ReflectionTestUtils.setField(product, "id", productId);
            return product;
        });
        when(moduleRepository.findByProductIdAndModuleKey(eq(productId), any(ModuleKey.class))).thenReturn(Optional.empty());
        when(moduleRepository.save(any(ProductModule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductSeedReference reference = service.ensureProduct(new ProductSeedCommand(
                tenantId,
                "wikidev",
                "WikiDev",
                "Knowledge Base",
                "ACTIVE",
                Set.of(ModuleKey.CONTENT, ModuleKey.KNOWLEDGE_GRAPH)
        ));

        assertThat(reference.id()).isEqualTo(productId);
        assertThat(reference.key()).isEqualTo("wikidev");
        verify(moduleRepository, atLeastOnce()).save(any(ProductModule.class));
    }

    @Test
    void shouldUpdateSuspendedProductAndDisableModuleOutsideSeedSet() {
        UUID tenantId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Product existing = product(tenantId, "portal-norte", ProductTypeKey.PORTAL);
        ReflectionTestUtils.setField(existing, "id", productId);
        ProductModule analytics = new ProductModule(existing, ModuleKey.ANALYTICS);
        analytics.enable();
        when(productRepository.findByTenantIdAndKey(tenantId, "portal-norte")).thenReturn(Optional.of(existing));
        when(productRepository.save(existing)).thenReturn(existing);
        when(moduleRepository.findByProductIdAndModuleKey(eq(productId), any(ModuleKey.class)))
                .thenAnswer(invocation -> invocation.getArgument(1) == ModuleKey.ANALYTICS
                        ? Optional.of(analytics)
                        : Optional.empty());
        when(moduleRepository.save(any(ProductModule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.ensureProduct(new ProductSeedCommand(
                tenantId,
                "portal-norte",
                "Portal Norte",
                "Portal",
                "SUSPENDED",
                Set.of()
        ));

        assertThat(existing.getStatus()).isEqualTo(ProductStatus.SUSPENDED);
        assertThat(analytics.isEnabled()).isFalse();
    }

    @Test
    void shouldArchiveExistingProduct() {
        UUID tenantId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Product existing = product(tenantId, "aegis-docs", ProductTypeKey.KNOWLEDGE_BASE);
        ReflectionTestUtils.setField(existing, "id", productId);
        when(productRepository.findByTenantIdAndKey(tenantId, "aegis-docs")).thenReturn(Optional.of(existing));
        when(productRepository.save(existing)).thenReturn(existing);
        when(moduleRepository.findByProductIdAndModuleKey(eq(productId), any(ModuleKey.class))).thenReturn(Optional.empty());
        when(moduleRepository.save(any(ProductModule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.ensureProduct(new ProductSeedCommand(
                tenantId,
                "aegis-docs",
                "Aegis Docs",
                "Knowledge Base",
                "ARCHIVED",
                Set.of()
        ));

        assertThat(existing.getStatus()).isEqualTo(ProductStatus.ARCHIVED);
    }

    @Test
    void shouldCreateAssignmentWhenMissing() {
        UUID productId = UUID.randomUUID();
        Product product = product(UUID.randomUUID(), "maestro-beton", ProductTypeKey.SITE_INSTITUCIONAL);
        ReflectionTestUtils.setField(product, "id", productId);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(assignmentRepository.findByProductIdAndUserSubject(productId, "user-id")).thenReturn(Optional.empty());

        service.ensureAssignment(productId, "user-id", "EDITOR");

        verify(assignmentRepository).save(org.mockito.ArgumentMatchers.argThat(assignment ->
                assignment.getProduct().equals(product)
                        && "user-id".equals(assignment.getUserSubject())
                        && assignment.getRole() == ProductAssignmentRole.EDITOR
                        && assignment.getStatus() == ProductAssignmentStatus.ASSIGNED
        ));
    }

    @Test
    void shouldReactivateExistingAssignment() {
        UUID productId = UUID.randomUUID();
        Product product = product(UUID.randomUUID(), "maestro-beton", ProductTypeKey.SITE_INSTITUCIONAL);
        ProductAssignment assignment = new ProductAssignment(product, "user-id", ProductAssignmentRole.VIEWER);
        assignment.remove();
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(assignmentRepository.findByProductIdAndUserSubject(productId, "user-id"))
                .thenReturn(Optional.of(assignment));

        service.ensureAssignment(productId, "user-id", "PRODUCT_MANAGER");

        assertThat(assignment.getRole()).isEqualTo(ProductAssignmentRole.PRODUCT_MANAGER);
        assertThat(assignment.getStatus()).isEqualTo(ProductAssignmentStatus.ASSIGNED);
        verify(assignmentRepository).save(assignment);
    }

    @Test
    void shouldRejectAssignmentWhenProductDoesNotExist() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                service.ensureAssignment(productId, "user-id", "VIEWER"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found for seed");
    }

    @Test
    void shouldTreatNullModuleSetAsEmpty() {
        UUID tenantId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(productRepository.findByTenantIdAndKey(tenantId, "custom")).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            ReflectionTestUtils.setField(product, "id", productId);
            return product;
        });
        when(moduleRepository.findByProductIdAndModuleKey(eq(productId), any(ModuleKey.class))).thenReturn(Optional.empty());
        when(moduleRepository.save(any(ProductModule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.ensureProduct(new ProductSeedCommand(tenantId, "custom", "Custom", "Custom", "ACTIVE", null));

        verify(moduleRepository, atLeastOnce()).save(org.mockito.ArgumentMatchers.argThat(module -> !module.isEnabled()));
    }

    private Product product(UUID tenantId, String key, ProductTypeKey type) {
        return new Product(tenantId, key, key, type, "pt-BR");
    }
}
