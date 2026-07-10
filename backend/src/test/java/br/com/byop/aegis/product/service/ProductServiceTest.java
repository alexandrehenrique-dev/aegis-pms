package br.com.byop.aegis.product.service;

import br.com.byop.aegis.product.api.ModuleKey;
import br.com.byop.aegis.product.api.ProductCreatedEvent;
import br.com.byop.aegis.product.command.CreateProductCommand;
import br.com.byop.aegis.product.command.UpdateProductCommand;
import br.com.byop.aegis.product.api.AssetStorageStrategy;
import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.domain.ProductAssignment;
import br.com.byop.aegis.product.domain.ProductAssignmentRole;
import br.com.byop.aegis.product.domain.ProductAssignmentStatus;
import br.com.byop.aegis.product.domain.ProductModule;
import br.com.byop.aegis.product.domain.ProductStatus;
import br.com.byop.aegis.product.domain.ProductTypeKey;
import br.com.byop.aegis.product.dto.ProductDetail;
import br.com.byop.aegis.product.dto.ProductModuleSummary;
import br.com.byop.aegis.product.dto.ProductSummary;
import br.com.byop.aegis.product.exception.InvalidProductTypeException;
import br.com.byop.aegis.product.exception.InvalidProductStatusException;
import br.com.byop.aegis.product.exception.ProductAlreadyExistsException;
import br.com.byop.aegis.product.exception.ProductNotFoundException;
import br.com.byop.aegis.product.mapper.ProductMapper;
import br.com.byop.aegis.product.mapper.ProductModuleMapper;
import br.com.byop.aegis.product.repository.ProductAssignmentRepository;
import br.com.byop.aegis.product.repository.ProductModuleRepository;
import br.com.byop.aegis.product.repository.ProductRepository;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.tenant.api.TenantAccessService;
import br.com.byop.aegis.tenant.api.TenantReference;
import br.com.byop.aegis.tenant.domain.Tenant;
import br.com.byop.aegis.tenant.exception.TenantNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private TenantAccessService tenantAccessService;

    @Mock
    private ProductAssignmentRepository assignmentRepository;

    @Mock
    private ProductModuleRepository moduleRepository;

    @Mock
    private ProductModuleMapper moduleMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductModuleService productModuleService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ProductService productService;

    @Test
    void shouldCreateProductAndAutomaticProductManagerAssignmentWithDefaultLocalStorage() {
        UUID tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Tenant tenant = tenant(tenantId, "byop");
        Product savedProduct = product(tenant, "maestro-beton");
        ProductSummary summary = productSummary(tenantId, savedProductId(), "maestro-beton");
        when(tenantAccessService.getRequiredReference(tenantId)).thenReturn(new TenantReference(tenantId, "Tenant Aegis"));
        when(productRepository.existsByTenantIdAndKey(tenantId, "maestro-beton")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        when(productMapper.toSummary(savedProduct, 0, List.of(), "PRODUCT_MANAGER")).thenReturn(summary);

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

        ArgumentCaptor<ProductCreatedEvent> eventCaptor = ArgumentCaptor.forClass(ProductCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().tenantId()).isEqualTo(savedProduct.getTenantId());
        assertThat(eventCaptor.getValue().productId()).isEqualTo(savedProduct.getId());
        assertThat(eventCaptor.getValue().assetStorageStrategy()).isEqualTo(AssetStorageStrategy.LOCAL);
        assertThat(eventCaptor.getValue().productType()).isEqualTo("SITE_INSTITUCIONAL");
        assertThat(eventCaptor.getValue().defaultLocale()).isEqualTo("pt-BR");

        var moduleOrder = org.mockito.Mockito.inOrder(productModuleService);
        moduleOrder.verify(productModuleService).enableModule(savedProductId(), "PAGES");
        moduleOrder.verify(productModuleService).enableModule(savedProductId(), "CONTENT");
        moduleOrder.verify(productModuleService).enableModule(savedProductId(), "ASSETS");
        moduleOrder.verify(productModuleService).enableModule(savedProductId(), "FORMS");
        moduleOrder.verify(productModuleService).enableModule(savedProductId(), "SEO");
        moduleOrder.verify(productModuleService).enableModule(savedProductId(), "ANALYTICS");
    }

    @Test
    void shouldEnableContentBeforeKnowledgeGraphForKnowledgeBase() {
        UUID tenantId = UUID.fromString("13131313-1313-1313-1313-131313131313");
        Product savedProduct = product(tenant(tenantId, "kb"), "kb-product");
        when(tenantAccessService.getRequiredReference(tenantId)).thenReturn(new TenantReference(tenantId, "Tenant Aegis"));
        when(productRepository.existsByTenantIdAndKey(tenantId, "kb-product")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        when(productMapper.toSummary(savedProduct, 0, List.of(), "PRODUCT_MANAGER")).thenReturn(productSummary(tenantId, savedProductId(), "kb-product"));

        productService.createProduct(user("creator-subject", "ROLE_TENANT_ADMIN"),
                createCommand(tenantId, "kb-product", "Knowledge Base"));

        var moduleOrder = org.mockito.Mockito.inOrder(productModuleService);
        moduleOrder.verify(productModuleService).enableModule(savedProductId(), "CONTENT");
        moduleOrder.verify(productModuleService).enableModule(savedProductId(), "KNOWLEDGE_GRAPH");
    }

    @Test
    void shouldNotEnableAnyModuleForCustomProduct() {
        UUID tenantId = UUID.fromString("14141414-1414-1414-1414-141414141414");
        Product savedProduct = product(tenant(tenantId, "custom"), "custom-product");
        when(tenantAccessService.getRequiredReference(tenantId)).thenReturn(new TenantReference(tenantId, "Tenant Aegis"));
        when(productRepository.existsByTenantIdAndKey(tenantId, "custom-product")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        when(productMapper.toSummary(savedProduct, 0, List.of(), "PRODUCT_MANAGER")).thenReturn(productSummary(tenantId, savedProductId(), "custom-product"));

        productService.createProduct(user("creator-subject", "ROLE_TENANT_ADMIN"),
                createCommand(tenantId, "custom-product", ProductTypeKey.CUSTOM.name()));

        verify(productModuleService, never()).enableModule(any(), any());
    }

    @Test
    void shouldPropagateFailureFromModuleEnablingWithoutPublishingEvent() {
        UUID tenantId = UUID.fromString("15151515-1515-1515-1515-151515151515");
        Product savedProduct = product(tenant(tenantId, "broken"), "broken-product");
        when(tenantAccessService.getRequiredReference(tenantId)).thenReturn(new TenantReference(tenantId, "Tenant Aegis"));
        when(productRepository.existsByTenantIdAndKey(tenantId, "broken-product")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        doThrow(new RuntimeException("boom"))
                .when(productModuleService).enableModule(savedProductId(), "PAGES");
        AuthenticatedUser caller = user("creator-subject", "ROLE_TENANT_ADMIN");
        CreateProductCommand command = createCommand(tenantId, "broken-product", "Site Institucional");

        assertThatThrownBy(() -> productService.createProduct(caller, command))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("boom");

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldCreateProductWithExplicitAssetStorageStrategy() {
        UUID tenantId = UUID.fromString("12121212-1212-1212-1212-121212121212");
        Tenant tenant = tenant(tenantId, "explicit-storage");
        Product savedProduct = product(tenant, "explicit-storage-product");
        ProductSummary summary = productSummary(tenantId, savedProductId(), "explicit-storage-product");
        when(tenantAccessService.getRequiredReference(tenantId)).thenReturn(new TenantReference(tenantId, "Tenant Aegis"));
        when(productRepository.existsByTenantIdAndKey(tenantId, "explicit-storage-product")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        when(productMapper.toSummary(savedProduct, 0, List.of(), "PRODUCT_MANAGER")).thenReturn(summary);

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
        when(tenantAccessService.getRequiredReference(tenantId)).thenThrow(new TenantNotFoundException(tenantId));
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
        when(tenantAccessService.getRequiredReference(tenantId)).thenReturn(new TenantReference(tenantId, "Tenant Aegis"));
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
        when(tenantAccessService.getRequiredReference(tenantId)).thenReturn(new TenantReference(tenantId, "Tenant Aegis"));
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
        when(assignmentRepository.findAllByUserSubjectAndStatus("super-subject", ProductAssignmentStatus.ASSIGNED))
                .thenReturn(List.of());
        when(productMapper.toSummary(product, 0, List.of(), null)).thenReturn(summary);

        List<ProductSummary> result = productService.listProducts(user("super-subject", "ROLE_SUPER_ADMIN"));

        assertThat(result).containsExactly(summary);
        verify(tenantAccessService, never()).findActiveTenantAdminTenantIds("super-subject");
        // Super Admin agora TAMBEM consulta as proprias ProductAssignment (nao mais "never") —
        // necessario para saber, por produto, se ele acumula um papel de produto especifico
        // (ex.: Editor de um produto pontual) a mesclar na navegacao do frontend.
        verify(assignmentRepository).findAllByUserSubjectAndStatus("super-subject", ProductAssignmentStatus.ASSIGNED);
    }

    @Test
    void shouldListProductsByActiveTenantAdminMembership() {
        Tenant activeTenant = tenant(UUID.fromString("66666666-6666-6666-6666-666666666666"), "active");
        Tenant inactiveTenant = tenant(UUID.fromString("77777777-7777-7777-7777-777777777777"), "inactive");
        Product product = product(activeTenant, "tenant-product");
        ProductSummary summary = productSummary(activeTenant.getId(), savedProductId(), "tenant-product");
        when(tenantAccessService.findActiveTenantAdminTenantIds("tenant-admin-subject"))
                .thenReturn(List.of(activeTenant.getId()));
        when(productRepository.findAllByTenantId(activeTenant.getId())).thenReturn(List.of(product));
        when(productMapper.toSummary(product, 0, List.of(), null)).thenReturn(summary);

        List<ProductSummary> result = productService.listProducts(user("tenant-admin-subject", "ROLE_TENANT_ADMIN"));

        assertThat(result).containsExactly(summary);
        verify(productRepository, never()).findAllByTenantId(inactiveTenant.getId());
    }

    @Test
    void shouldListProductsByTenantAdminMembershipEvenWhenTokenHasNoTenantAdminRealmRoleYet() {
        Tenant tenant = tenant(UUID.fromString("66666666-6666-6666-6666-666666666667"), "membership-only-admin");
        Product product = product(tenant, "membership-only-product");
        ProductSummary summary = productSummary(tenant.getId(), savedProductId(), "membership-only-product");
        when(tenantAccessService.findActiveTenantAdminTenantIds("tenant-admin-subject"))
                .thenReturn(List.of(tenant.getId()));
        when(productRepository.findAllByTenantId(tenant.getId())).thenReturn(List.of(product));
        when(productMapper.toSummary(product, 0, List.of(), null)).thenReturn(summary);

        List<ProductSummary> result = productService.listProducts(user("tenant-admin-subject", "ROLE_UNKNOWN"));

        assertThat(result).containsExactly(summary);
    }

    @Test
    void shouldIgnoreNonTenantAdminMembershipWhenListingForTenantAdminAuthority() {
        Tenant tenant = tenant(UUID.fromString("88888888-8888-8888-8888-888888888888"), "viewer-tenant");
        when(tenantAccessService.findActiveTenantAdminTenantIds("tenant-admin-subject")).thenReturn(List.of());

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
    void shouldListAssignedProductsEvenWhenTokenHasNoProductRealmRoleYet() {
        Tenant tenant = tenant(UUID.fromString("99999999-9999-9999-9999-999999999998"), "legacy-role");
        Product product = product(tenant, "legacy-role-product");
        ProductAssignment assignment = new ProductAssignment(product, "unknown-subject", ProductAssignmentRole.PRODUCT_MANAGER);
        ProductSummary summary = productSummary(tenant.getId(), product.getId(), product.getKey());
        when(assignmentRepository.findAllByUserSubjectAndStatus("unknown-subject", ProductAssignmentStatus.ASSIGNED))
                .thenReturn(List.of(assignment));
        when(productMapper.toSummary(product, 0, List.of(), "PRODUCT_MANAGER")).thenReturn(summary);

        List<ProductSummary> result = productService.listProducts(user("unknown-subject", "ROLE_UNKNOWN"));

        assertThat(result).containsExactly(summary);
    }

    @Test
    void shouldGetProductForSuperAdminTenantAdminAndProductRole() {
        Tenant tenant = tenant(UUID.fromString("99999999-9999-9999-9999-999999999999"), "byop");
        Product product = product(tenant, "visible-product");
        UUID productId = product.getId();
        ProductSummary summary = productSummary(tenant.getId(), productId, "visible-product");
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productMapper.toSummary(product, 0, List.of(), null)).thenReturn(summary);

        assertThat(productService.getProduct(user("super-subject", "ROLE_SUPER_ADMIN"), productId)).isEqualTo(summary);

        when(tenantAccessService.hasActiveMembership(tenant.getId(), "tenant-subject")).thenReturn(true);
        assertThat(productService.getProduct(user("tenant-subject", "ROLE_TENANT_ADMIN"), productId)).isEqualTo(summary);

        when(assignmentRepository.existsByProductIdAndUserSubjectAndStatus(
                productId, "editor-subject", ProductAssignmentStatus.ASSIGNED
        )).thenReturn(true);
        assertThat(productService.getProduct(user("editor-subject", "ROLE_EDITOR"), productId)).isEqualTo(summary);
    }

    @Test
    void shouldUpdateProductForSuperAdminAndPersistActiveStatus() {
        Tenant tenant = tenant(UUID.fromString("b3b3b3b3-b3b3-b3b3-b3b3-b3b3b3b3b3b3"), "update-super");
        Product product = product(tenant, "archived-product");
        product.archive();
        UUID productId = product.getId();
        ProductSummary summary = productSummary(tenant.getId(), productId, "archived-product");
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productMapper.toSummary(product, 0, List.of(), null)).thenReturn(summary);
        AuthenticatedUser caller = user("super-subject", "ROLE_SUPER_ADMIN");
        UpdateProductCommand command = new UpdateProductCommand("Produto Ativo", "PORTAL", "ACTIVE");

        ProductSummary result = productService.updateProduct(caller, productId, command);

        assertThat(result).isEqualTo(summary);
        assertThat(product.getName()).isEqualTo("Produto Ativo");
        assertThat(product.getType()).isEqualTo(ProductTypeKey.PORTAL);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
    }

    @Test
    void shouldSyncProductModulesWhenUpdatingProduct() {
        Tenant tenant = tenant(UUID.fromString("b9b9b9b9-b9b9-b9b9-b9b9-b9b9b9b9b9b9"), "update-modules");
        Product product = product(tenant, "loki");
        UUID productId = product.getId();
        ProductModule contentModule = enabledProductModule(product, ModuleKey.CONTENT);
        ProductModule assetsModule = enabledProductModule(product, ModuleKey.ASSETS);
        ProductModule pagesModule = new ProductModule(product, ModuleKey.PAGES);
        List<ProductModule> configuredModules = List.of(contentModule, assetsModule, pagesModule);
        ProductAssignment assignment = new ProductAssignment(product, "super-subject", ProductAssignmentRole.EDITOR);
        ProductSummary summary = productSummary(tenant.getId(), productId, "loki");
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(moduleRepository.findAllByProductId(productId)).thenReturn(configuredModules);
        when(assignmentRepository.findByProductIdAndUserSubject(productId, "super-subject"))
                .thenReturn(Optional.of(assignment));
        when(productMapper.toSummary(product, 2, List.of("CONTENT", "PAGES"), "EDITOR")).thenReturn(summary);
        AuthenticatedUser caller = user("super-subject", "ROLE_SUPER_ADMIN");
        UpdateProductCommand command = new UpdateProductCommand(
                "Loki",
                "Library/Books/Music",
                "Ativo",
                List.of("Conteúdo", "Páginas")
        );

        ProductSummary result = productService.updateProduct(caller, productId, command);

        assertThat(result).isEqualTo(summary);
        assertThat(contentModule.isEnabled()).isTrue();
        assertThat(pagesModule.isEnabled()).isTrue();
        assertThat(assetsModule.isEnabled()).isFalse();
        verify(moduleRepository).save(contentModule);
        verify(moduleRepository).save(assetsModule);
        verify(moduleRepository).save(pagesModule);
        verify(moduleRepository).flush();
    }

    @Test
    void shouldCreateMissingRequiredModulesWhenUpdatingProductWithKnowledgeGraph() {
        Tenant tenant = tenant(UUID.fromString("b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b112"), "update-kg-modules");
        Product product = product(tenant, "kg-product");
        UUID productId = product.getId();
        ProductModule knowledgeGraphModule = enabledProductModule(product, ModuleKey.KNOWLEDGE_GRAPH);
        ProductSummary summary = productSummary(tenant.getId(), productId, "kg-product");

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(moduleRepository.findAllByProductId(productId)).thenReturn(List.of(knowledgeGraphModule));
        when(productMapper.toSummary(
                product,
                3,
                List.of(ModuleKey.KNOWLEDGE_GRAPH.name(), ModuleKey.CONTENT.name(), ModuleKey.ASSETS.name()),
                null))
                .thenReturn(summary);
        AuthenticatedUser caller = user("super-subject", "ROLE_SUPER_ADMIN");
        UpdateProductCommand command =
                new UpdateProductCommand("KG Product", "Knowledge Base", "Ativo", List.of("Knowledge Graph"));

        ProductSummary result = productService.updateProduct(caller, productId, command);

        assertThat(result).isEqualTo(summary);
        ArgumentCaptor<ProductModule> moduleCaptor = ArgumentCaptor.forClass(ProductModule.class);
        verify(moduleRepository, times(3)).save(moduleCaptor.capture());
        assertThat(moduleCaptor.getAllValues())
                .extracting(ProductModule::getModuleKey)
                .containsExactly(ModuleKey.KNOWLEDGE_GRAPH, ModuleKey.CONTENT, ModuleKey.ASSETS);
        assertThat(moduleCaptor.getAllValues())
                .extracting(ProductModule::isEnabled)
                .containsOnly(true);
        verify(moduleRepository).flush();
    }

    @Test
    void shouldRejectInvalidProductModuleWhenUpdatingProduct() {
        Tenant tenant = tenant(UUID.fromString("b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b113"), "invalid-module-tenant");
        Product product = product(tenant, "invalid-module-product");
        UUID productId = product.getId();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        AuthenticatedUser caller = user("super-subject", "ROLE_SUPER_ADMIN");
        UpdateProductCommand command =
                new UpdateProductCommand("Invalid Module Product", "CUSTOM", "ACTIVE", List.of("Modulo inexistente"));

        assertThatThrownBy(() -> productService.updateProduct(caller, productId, command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid product module: Modulo inexistente");

        verify(moduleRepository, never()).findAllByProductId(productId);
    }

    @Test
    void shouldUpdateProductForTenantAdminMembershipWithPortugueseArchivedStatus() {
        Tenant tenant = tenant(UUID.fromString("b4b4b4b4-b4b4-b4b4-b4b4-b4b4b4b4b4b4"), "update-tenant");
        Product product = product(tenant, "tenant-product");
        UUID productId = product.getId();
        ProductSummary summary = productSummary(tenant.getId(), productId, "tenant-product");
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(tenantAccessService.hasActiveTenantAdminMembership(tenant.getId(), "tenant-subject")).thenReturn(true);
        when(productMapper.toSummary(product, 0, List.of(), null)).thenReturn(summary);
        AuthenticatedUser caller = user("tenant-subject", "ROLE_VIEWER");
        UpdateProductCommand command = new UpdateProductCommand("Produto Arquivado", "Knowledge Base", "Arquivado");

        ProductSummary result = productService.updateProduct(caller, productId, command);

        assertThat(result).isEqualTo(summary);
        assertThat(product.getType()).isEqualTo(ProductTypeKey.KNOWLEDGE_BASE);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ARCHIVED);
    }

    @Test
    void shouldMapLegacyPendingStatusToSuspendedWhenUpdatingProduct() {
        Tenant tenant = tenant(UUID.fromString("b5b5b5b5-b5b5-b5b5-b5b5-b5b5b5b5b5b5"), "update-pending");
        Product product = product(tenant, "pending-product");
        UUID productId = product.getId();
        ProductSummary summary = productSummary(tenant.getId(), productId, "pending-product");
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productMapper.toSummary(product, 0, List.of(), null)).thenReturn(summary);
        AuthenticatedUser caller = user("super-subject", "ROLE_SUPER_ADMIN");
        UpdateProductCommand command = new UpdateProductCommand("Produto Pendente", "CUSTOM", "PENDING");

        productService.updateProduct(caller, productId, command);

        assertThat(product.getStatus()).isEqualTo(ProductStatus.SUSPENDED);
    }

    @Test
    void shouldRejectInvalidProductStatusWhenUpdatingProduct() {
        Tenant tenant = tenant(UUID.fromString("b6b6b6b6-b6b6-b6b6-b6b6-b6b6b6b6b6b6"), "invalid-status");
        Product product = product(tenant, "invalid-status-product");
        UUID productId = product.getId();
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        AuthenticatedUser caller = user("super-subject", "ROLE_SUPER_ADMIN");
        UpdateProductCommand command = new UpdateProductCommand("Produto", "CUSTOM", "BROKEN");

        assertThatThrownBy(() -> productService.updateProduct(caller, productId, command))
                .isInstanceOf(InvalidProductStatusException.class)
                .hasMessage("Invalid product status: BROKEN");
    }

    @Test
    void shouldRejectProductRoleTryingToUpdateProductStatus() {
        Tenant tenant = tenant(UUID.fromString("b7b7b7b7-b7b7-b7b7-b7b7-b7b7b7b7b7b7"), "update-denied");
        Product product = product(tenant, "denied-product");
        UUID productId = product.getId();
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(tenantAccessService.hasActiveTenantAdminMembership(tenant.getId(), "pm-subject")).thenReturn(false);
        AuthenticatedUser caller = user("pm-subject", "ROLE_PRODUCT_MANAGER");
        UpdateProductCommand command = new UpdateProductCommand("Produto", "CUSTOM", "ACTIVE");

        assertThatThrownBy(() -> productService.updateProduct(caller, productId, command))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product not found: " + productId);
    }

    @Test
    void shouldRejectUpdateWhenProductDoesNotExist() {
        UUID productId = UUID.fromString("b8b8b8b8-b8b8-b8b8-b8b8-b8b8b8b8b8b8");
        when(productRepository.findById(productId)).thenReturn(Optional.empty());
        AuthenticatedUser caller = user("super-subject", "ROLE_SUPER_ADMIN");
        UpdateProductCommand command = new UpdateProductCommand("Produto", "CUSTOM", "ACTIVE");

        assertThatThrownBy(() -> productService.updateProduct(caller, productId, command))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product not found: " + productId);
    }

    @Test
    void shouldReportCallerAssignedRoleWhenAssignmentIsActive() {
        Tenant tenant = tenant(UUID.fromString("b1b1b1b1-b1b1-b1b1-b1b1-b1b1b1b1b1b1"), "combo");
        Product product = product(tenant, "combo-product");
        UUID productId = product.getId();
        ProductAssignment assignment = new ProductAssignment(product, "super-editor-subject", ProductAssignmentRole.EDITOR);
        ProductSummary summary = productSummary(tenant.getId(), productId, "combo-product");
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(assignmentRepository.findByProductIdAndUserSubject(productId, "super-editor-subject"))
                .thenReturn(Optional.of(assignment));
        when(productMapper.toSummary(product, 0, List.of(), "EDITOR")).thenReturn(summary);

        assertThat(productService.getProduct(user("super-editor-subject", "ROLE_SUPER_ADMIN"), productId))
                .isEqualTo(summary);
    }

    @Test
    void shouldNotReportCallerAssignedRoleWhenAssignmentIsNotActive() {
        Tenant tenant = tenant(UUID.fromString("b2b2b2b2-b2b2-b2b2-b2b2-b2b2b2b2b2b2"), "revoked");
        Product product = product(tenant, "revoked-product");
        UUID productId = product.getId();
        ProductAssignment assignment = new ProductAssignment(product, "super-subject", ProductAssignmentRole.EDITOR);
        assignment.revoke();
        ProductSummary summary = productSummary(tenant.getId(), productId, "revoked-product");
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(assignmentRepository.findByProductIdAndUserSubject(productId, "super-subject"))
                .thenReturn(Optional.of(assignment));
        when(productMapper.toSummary(product, 0, List.of(), null)).thenReturn(summary);

        assertThat(productService.getProduct(user("super-subject", "ROLE_SUPER_ADMIN"), productId))
                .isEqualTo(summary);
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
        when(tenantAccessService.hasActiveMembership(tenant.getId(), "tenant-subject")).thenReturn(false);
        AuthenticatedUser caller = user("tenant-subject", "ROLE_TENANT_ADMIN");

        assertThatThrownBy(() -> productService.getProduct(caller, productId))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product not found: " + productId);
    }

    @Test
    void shouldGetProductByTenantAdminMembershipEvenWhenTokenHasNoTenantAdminRealmRoleYet() {
        Tenant tenant = tenant(UUID.fromString("adadadad-adad-adad-adad-adadadadadad"), "membership-admin");
        Product product = product(tenant, "membership-admin-product");
        UUID productId = product.getId();
        ProductSummary summary = productSummary(tenant.getId(), productId, "membership-admin-product");
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(tenantAccessService.hasActiveTenantAdminMembership(tenant.getId(), "tenant-subject")).thenReturn(true);
        when(productMapper.toSummary(product, 0, List.of(), null)).thenReturn(summary);

        ProductSummary result = productService.getProduct(user("tenant-subject", "ROLE_VIEWER"), productId);

        assertThat(result).isEqualTo(summary);
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
        when(productMapper.toSummary(product, 0, List.of(), "EDITOR")).thenReturn(summary);

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
                tenant.getId(),
                key,
                "Product " + key,
                ProductTypeKey.SITE_INSTITUCIONAL,
                "pt-BR",
                AssetStorageStrategy.S3
        );
        ReflectionTestUtils.setField(product, "id", savedProductId());
        return product;
    }

    private ProductModule enabledProductModule(Product product, ModuleKey moduleKey) {
        ProductModule module = new ProductModule(product, moduleKey);
        module.enable();
        return module;
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
                OffsetDateTime.parse("2026-06-25T10:10:00-03:00"),
                0,
                List.of(),
                null
        );
    }

    private UUID savedProductId() {
        return UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    }

    private AuthenticatedUser user(String subject, String authority) {
        return new AuthenticatedUser(subject, subject + "@byop.dev", subject, subject, Set.of(authority));
    }
}
