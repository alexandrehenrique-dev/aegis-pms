package br.com.byop.aegis.core.product;

import br.com.byop.aegis.core.product.command.CreateProductCommand;
import br.com.byop.aegis.core.product.dto.ProductDetail;
import br.com.byop.aegis.core.product.dto.ProductModuleSummary;
import br.com.byop.aegis.core.product.dto.ProductSummary;
import br.com.byop.aegis.core.product.exception.InvalidProductTypeException;
import br.com.byop.aegis.core.product.exception.ProductAlreadyExistsException;
import br.com.byop.aegis.core.product.exception.ProductNotFoundException;
import br.com.byop.aegis.core.tenant.Tenant;
import br.com.byop.aegis.core.tenant.TenantMembership;
import br.com.byop.aegis.core.tenant.TenantMembershipRepository;
import br.com.byop.aegis.core.tenant.TenantMembershipStatus;
import br.com.byop.aegis.core.tenant.TenantRepository;
import br.com.byop.aegis.core.tenant.exception.TenantNotFoundException;
import br.com.byop.aegis.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private TenantMembershipRepository membershipRepository;

    @Mock
    private ProductAssignmentRepository assignmentRepository;

    @Mock
    private ProductModuleRepository moduleRepository;

    @Mock
    private ProductModuleMapper moduleMapper;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    @Test
    void shouldCreateProductAndAutomaticProductManagerAssignmentWithDefaultLocalStorage() {
        UUID tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Tenant tenant = tenant(tenantId, "byop");
        Product savedProduct = product(tenant, "maestro-beton");
        ProductSummary summary = productSummary(tenantId, savedProductId(), "maestro-beton");
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(productRepository.existsByTenantIdAndKey(tenantId, "maestro-beton")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        when(productMapper.toSummary(savedProduct)).thenReturn(summary);

        ProductSummary result = productService.createProduct(
                user("creator-subject", "ROLE_TENANT_ADMIN"),
                new CreateProductCommand(
                        tenantId,
                        "maestro-beton",
                        "Maestro Beton",
                        "Site Institucional",
                        "pt-BR",
                        null
                )
        );

        assertThat(result).isEqualTo(summary);
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        assertThat(productCaptor.getValue().getAssetStorageStrategy()).isEqualTo(AssetStorageStrategy.LOCAL);
        assertThat(productCaptor.getValue().getType()).isEqualTo(ProductTypeKey.SITE_INSTITUCIONAL);

        ArgumentCaptor<ProductAssignment> assignmentCaptor = ArgumentCaptor.forClass(ProductAssignment.class);
        verify(assignmentRepository).save(assignmentCaptor.capture());
        ProductAssignment assignment = assignmentCaptor.getValue();
        assertThat(assignment.getProduct()).isEqualTo(savedProduct);
        assertThat(assignment.getUserSubject()).isEqualTo("creator-subject");
        assertThat(assignment.getRole()).isEqualTo(ProductAssignmentRole.PRODUCT_MANAGER);
        assertThat(assignment.getStatus()).isEqualTo(ProductAssignmentStatus.ASSIGNED);
    }

    @Test
    void shouldCreateProductWithExplicitAssetStorageStrategy() {
        UUID tenantId = UUID.fromString("12121212-1212-1212-1212-121212121212");
        Tenant tenant = tenant(tenantId, "explicit-storage");
        Product savedProduct = product(tenant, "explicit-storage-product");
        ProductSummary summary = productSummary(tenantId, savedProductId(), "explicit-storage-product");
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(productRepository.existsByTenantIdAndKey(tenantId, "explicit-storage-product")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        when(productMapper.toSummary(savedProduct)).thenReturn(summary);

        ProductSummary result = productService.createProduct(
                user("creator-subject", "ROLE_TENANT_ADMIN"),
                createCommand(tenantId, "explicit-storage-product", ProductTypeKey.CUSTOM.name())
        );

        assertThat(result).isEqualTo(summary);
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        assertThat(productCaptor.getValue().getAssetStorageStrategy()).isEqualTo(AssetStorageStrategy.S3);
    }

    @Test
    void shouldRejectProductWhenTenantDoesNotExist() {
        UUID tenantId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());
        AuthenticatedUser caller = user("creator-subject", "ROLE_TENANT_ADMIN");
        CreateProductCommand command = createCommand(tenantId, "missing-tenant", "CUSTOM");

        assertThatThrownBy(() -> productService.createProduct(caller, command))
                .isInstanceOf(TenantNotFoundException.class)
                .hasMessage("Tenant not found: " + tenantId);

        verify(productRepository, never()).save(any(Product.class));
        verify(assignmentRepository, never()).save(any(ProductAssignment.class));
    }

    @Test
    void shouldRejectDuplicateProductKeyByTenant() {
        UUID tenantId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        Tenant tenant = tenant(tenantId, "byop");
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(productRepository.existsByTenantIdAndKey(tenantId, "duplicate")).thenReturn(true);
        AuthenticatedUser caller = user("creator-subject", "ROLE_TENANT_ADMIN");
        CreateProductCommand command = createCommand(tenantId, "duplicate", ProductTypeKey.CUSTOM.name());

        assertThatThrownBy(() -> productService.createProduct(caller, command))
                .isInstanceOf(ProductAlreadyExistsException.class)
                .hasMessage("Product already exists with key 'duplicate' for tenant: " + tenantId);
    }

    @Test
    void shouldRejectInvalidProductType() {
        UUID tenantId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        Tenant tenant = tenant(tenantId, "byop");
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        AuthenticatedUser caller = user("creator-subject", "ROLE_TENANT_ADMIN");
        CreateProductCommand command = createCommand(tenantId, "invalid", "Unknown");

        assertThatThrownBy(() -> productService.createProduct(caller, command))
                .isInstanceOf(InvalidProductTypeException.class)
                .hasMessage("Invalid product type: Unknown");
    }

    @Test
    void shouldListAllProductsForSuperAdmin() {
        Tenant tenant = tenant(UUID.fromString("55555555-5555-5555-5555-555555555555"), "byop");
        Product product = product(tenant, "all-products");
        ProductSummary summary = productSummary(tenant.getId(), savedProductId(), "all-products");
        when(productRepository.findAll()).thenReturn(List.of(product));
        when(productMapper.toSummary(product)).thenReturn(summary);

        List<ProductSummary> result = productService.listProducts(user("super-subject", "ROLE_SUPER_ADMIN"));

        assertThat(result).containsExactly(summary);
        verify(membershipRepository, never()).findAllByUserSubject("super-subject");
        verify(assignmentRepository, never()).findAllByUserSubjectAndStatus("super-subject", ProductAssignmentStatus.ASSIGNED);
    }

    @Test
    void shouldListProductsByActiveTenantAdminMembership() {
        Tenant activeTenant = tenant(UUID.fromString("66666666-6666-6666-6666-666666666666"), "active");
        Tenant inactiveTenant = tenant(UUID.fromString("77777777-7777-7777-7777-777777777777"), "inactive");
        Product product = product(activeTenant, "tenant-product");
        ProductSummary summary = productSummary(activeTenant.getId(), savedProductId(), "tenant-product");
        TenantMembership activeMembership = new TenantMembership(activeTenant, "tenant-admin-subject", "TENANT_ADMIN");
        TenantMembership suspendedMembership = new TenantMembership(inactiveTenant, "tenant-admin-subject", "TENANT_ADMIN");
        suspendedMembership.suspend();
        when(membershipRepository.findAllByUserSubject("tenant-admin-subject"))
                .thenReturn(List.of(activeMembership, suspendedMembership));
        when(productRepository.findAllByTenantId(activeTenant.getId())).thenReturn(List.of(product));
        when(productMapper.toSummary(product)).thenReturn(summary);

        List<ProductSummary> result = productService.listProducts(user("tenant-admin-subject", "ROLE_TENANT_ADMIN"));

        assertThat(result).containsExactly(summary);
        verify(productRepository, never()).findAllByTenantId(inactiveTenant.getId());
    }

    @Test
    void shouldIgnoreNonTenantAdminMembershipWhenListingForTenantAdminAuthority() {
        Tenant tenant = tenant(UUID.fromString("88888888-8888-8888-8888-888888888888"), "viewer-tenant");
        TenantMembership viewerMembership = new TenantMembership(tenant, "tenant-admin-subject", "VIEWER");
        when(membershipRepository.findAllByUserSubject("tenant-admin-subject")).thenReturn(List.of(viewerMembership));

        List<ProductSummary> result = productService.listProducts(user("tenant-admin-subject", "ROLE_TENANT_ADMIN"));

        assertThat(result).isEmpty();
        verify(productRepository, never()).findAllByTenantId(tenant.getId());
    }

    @Test
    void shouldListProductsByAssignmentForProductManagerEditorAndViewer() {
        assertThat(listProductsByAssignmentFor("ROLE_PRODUCT_MANAGER")).hasSize(1);
        assertThat(listProductsByAssignmentFor("ROLE_EDITOR")).hasSize(1);
        assertThat(listProductsByAssignmentFor("ROLE_VIEWER")).hasSize(1);
    }

    @Test
    void shouldReturnEmptyListWhenCallerHasNoKnownProductScope() {
        List<ProductSummary> result = productService.listProducts(user("unknown-subject", "ROLE_UNKNOWN"));

        assertThat(result).isEmpty();
    }

    @Test
    void shouldGetProductForSuperAdminTenantAdminAndProductRole() {
        Tenant tenant = tenant(UUID.fromString("99999999-9999-9999-9999-999999999999"), "byop");
        Product product = product(tenant, "visible-product");
        UUID productId = product.getId();
        ProductSummary summary = productSummary(tenant.getId(), productId, "visible-product");
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productMapper.toSummary(product)).thenReturn(summary);

        assertThat(productService.getProduct(user("super-subject", "ROLE_SUPER_ADMIN"), productId)).isEqualTo(summary);

        when(membershipRepository.existsByTenantIdAndUserSubjectAndStatus(
                tenant.getId(), "tenant-subject", TenantMembershipStatus.ACTIVE
        )).thenReturn(true);
        assertThat(productService.getProduct(user("tenant-subject", "ROLE_TENANT_ADMIN"), productId)).isEqualTo(summary);

        when(assignmentRepository.existsByProductIdAndUserSubjectAndStatus(
                productId, "editor-subject", ProductAssignmentStatus.ASSIGNED
        )).thenReturn(true);
        assertThat(productService.getProduct(user("editor-subject", "ROLE_EDITOR"), productId)).isEqualTo(summary);
    }

    @Test
    void shouldGetProductDetailWithModulesWhenCallerHasAccess() {
        Tenant tenant = tenant(UUID.fromString("9a9a9a9a-9a9a-9a9a-9a9a-9a9a9a9a9a9a"), "detail");
        Product product = product(tenant, "detail-product");
        ProductModule module = new ProductModule(product, ModuleKey.CONTENT);
        ProductModuleSummary moduleSummary = new ProductModuleSummary(
                UUID.fromString("9b9b9b9b-9b9b-9b9b-9b9b-9b9b9b9b9b9b"),
                product.getId(),
                ModuleKey.CONTENT,
                true,
                "{}",
                OffsetDateTime.parse("2026-06-25T10:00:00-03:00"),
                OffsetDateTime.parse("2026-06-25T10:10:00-03:00")
        );
        ProductDetail detail = new ProductDetail(
                product.getId(),
                tenant.getId(),
                product.getKey(),
                product.getName(),
                product.getType(),
                product.getStatus(),
                product.getDefaultLocale(),
                product.getAssetStorageStrategy(),
                OffsetDateTime.parse("2026-06-25T10:00:00-03:00"),
                OffsetDateTime.parse("2026-06-25T10:10:00-03:00"),
                List.of(moduleSummary)
        );
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(moduleRepository.findAllByProductId(product.getId())).thenReturn(List.of(module));
        when(moduleMapper.toSummary(module)).thenReturn(moduleSummary);
        when(productMapper.toDetail(product, List.of(moduleSummary))).thenReturn(detail);

        ProductDetail result = productService.getProductDetail(user("super-subject", "ROLE_SUPER_ADMIN"), product.getId());

        assertThat(result).isEqualTo(detail);
        assertThat(result.modules()).containsExactly(moduleSummary);
    }

    @Test
    void shouldHideProductOutsideCallerScope() {
        Tenant tenant = tenant(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"), "outside");
        Product product = product(tenant, "outside-product");
        UUID productId = product.getId();
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(assignmentRepository.existsByProductIdAndUserSubjectAndStatus(
                productId, "editor-subject", ProductAssignmentStatus.ASSIGNED
        )).thenReturn(false);
        AuthenticatedUser caller = user("editor-subject", "ROLE_EDITOR");

        assertThatThrownBy(() -> productService.getProduct(caller, productId))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product not found: " + productId);
    }

    @Test
    void shouldHideProductWhenTenantAdminHasNoActiveMembership() {
        Tenant tenant = tenant(UUID.fromString("abababab-abab-abab-abab-abababababab"), "tenant-admin-outside");
        Product product = product(tenant, "tenant-admin-outside-product");
        UUID productId = product.getId();
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(membershipRepository.existsByTenantIdAndUserSubjectAndStatus(
                tenant.getId(), "tenant-subject", TenantMembershipStatus.ACTIVE
        )).thenReturn(false);
        AuthenticatedUser caller = user("tenant-subject", "ROLE_TENANT_ADMIN");

        assertThatThrownBy(() -> productService.getProduct(caller, productId))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product not found: " + productId);
    }

    @Test
    void shouldHideProductWhenCallerHasNoKnownProductScope() {
        Tenant tenant = tenant(UUID.fromString("acacacac-acac-acac-acac-acacacacacac"), "unknown-scope");
        Product product = product(tenant, "unknown-scope-product");
        UUID productId = product.getId();
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        AuthenticatedUser caller = user("unknown-subject", "ROLE_UNKNOWN");

        assertThatThrownBy(() -> productService.getProduct(caller, productId))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product not found: " + productId);
    }

    @Test
    void shouldThrowProductNotFoundWhenProductDoesNotExist() {
        UUID productId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        when(productRepository.findById(productId)).thenReturn(Optional.empty());
        AuthenticatedUser caller = user("viewer-subject", "ROLE_VIEWER");

        assertThatThrownBy(() -> productService.getProduct(caller, productId))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product not found: " + productId);
    }

    private List<ProductSummary> listProductsByAssignmentFor(String authority) {
        Tenant tenant = tenant(UUID.nameUUIDFromBytes(authority.getBytes()), "assigned-" + authority);
        Product product = product(tenant, "assigned-product-" + authority);
        ProductAssignment assignment = new ProductAssignment(product, "assigned-subject", ProductAssignmentRole.EDITOR);
        ProductSummary summary = productSummary(tenant.getId(), product.getId(), product.getKey());
        when(assignmentRepository.findAllByUserSubjectAndStatus("assigned-subject", ProductAssignmentStatus.ASSIGNED))
                .thenReturn(List.of(assignment));
        when(productMapper.toSummary(product)).thenReturn(summary);

        return productService.listProducts(user("assigned-subject", authority));
    }

    private CreateProductCommand createCommand(UUID tenantId, String key, String type) {
        return new CreateProductCommand(tenantId, key, "Product " + key, type, "pt-BR", AssetStorageStrategy.S3);
    }

    private Tenant tenant(UUID tenantId, String key) {
        Tenant tenant = new Tenant(key, "Tenant " + key);
        ReflectionTestUtils.setField(tenant, "id", tenantId);
        return tenant;
    }

    private Product product(Tenant tenant, String key) {
        Product product = new Product(
                tenant,
                key,
                "Product " + key,
                ProductTypeKey.SITE_INSTITUCIONAL,
                "pt-BR",
                AssetStorageStrategy.S3
        );
        ReflectionTestUtils.setField(product, "id", savedProductId());
        return product;
    }

    private ProductSummary productSummary(UUID tenantId, UUID productId, String key) {
        return new ProductSummary(
                productId,
                tenantId,
                key,
                "Product " + key,
                ProductTypeKey.SITE_INSTITUCIONAL,
                ProductStatus.ACTIVE,
                "pt-BR",
                AssetStorageStrategy.S3,
                OffsetDateTime.parse("2026-06-25T10:00:00-03:00"),
                OffsetDateTime.parse("2026-06-25T10:10:00-03:00")
        );
    }

    private UUID savedProductId() {
        return UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    }

    private AuthenticatedUser user(String subject, String authority) {
        return new AuthenticatedUser(subject, subject + "@byop.dev", subject, subject, Set.of(authority));
    }
}
