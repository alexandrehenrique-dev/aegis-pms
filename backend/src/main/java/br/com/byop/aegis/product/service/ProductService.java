package br.com.byop.aegis.product.service;

import br.com.byop.aegis.product.api.ModuleKey;
import br.com.byop.aegis.product.command.CreateProductCommand;
import br.com.byop.aegis.product.api.AssetStorageStrategy;
import br.com.byop.aegis.product.api.ProductCreatedEvent;
import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.domain.ProductAssignment;
import br.com.byop.aegis.product.domain.ProductAssignmentRole;
import br.com.byop.aegis.product.domain.ProductAssignmentStatus;
import br.com.byop.aegis.product.domain.ProductModuleTemplateCatalog;
import br.com.byop.aegis.product.domain.ProductTypeKey;
import br.com.byop.aegis.product.dto.ProductDetail;
import br.com.byop.aegis.product.dto.ProductModuleSummary;
import br.com.byop.aegis.product.dto.ProductSummary;
import br.com.byop.aegis.product.exception.InvalidProductTypeException;
import br.com.byop.aegis.product.exception.ProductAlreadyExistsException;
import br.com.byop.aegis.product.exception.ProductNotFoundException;
import br.com.byop.aegis.product.mapper.ProductMapper;
import br.com.byop.aegis.product.mapper.ProductModuleMapper;
import br.com.byop.aegis.product.repository.ProductAssignmentRepository;
import br.com.byop.aegis.product.repository.ProductModuleRepository;
import br.com.byop.aegis.product.repository.ProductRepository;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.tenant.api.TenantAccessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class ProductService {

    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String ROLE_TENANT_ADMIN = "ROLE_TENANT_ADMIN";
    private static final String ROLE_PRODUCT_MANAGER = "ROLE_PRODUCT_MANAGER";
    private static final String ROLE_EDITOR = "ROLE_EDITOR";
    private static final String ROLE_VIEWER = "ROLE_VIEWER";
    private static final Map<String, ProductTypeKey> PRODUCT_TYPES = buildProductTypes();

    private final ProductRepository productRepository;
    private final TenantAccessService tenantAccessService;
    private final ProductAssignmentRepository assignmentRepository;
    private final ProductModuleRepository moduleRepository;
    private final ProductModuleMapper moduleMapper;
    private final ProductMapper productMapper;
    private final ProductModuleService productModuleService;
    private final ApplicationEventPublisher eventPublisher;

    public ProductService(ProductRepository productRepository, TenantAccessService tenantAccessService,
                          ProductAssignmentRepository assignmentRepository, ProductModuleRepository moduleRepository,
                          ProductModuleMapper moduleMapper, ProductMapper productMapper,
                          ProductModuleService productModuleService, ApplicationEventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.tenantAccessService = tenantAccessService;
        this.assignmentRepository = assignmentRepository;
        this.moduleRepository = moduleRepository;
        this.moduleMapper = moduleMapper;
        this.productMapper = productMapper;
        this.productModuleService = productModuleService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ProductSummary createProduct(AuthenticatedUser caller, CreateProductCommand command) {
        log.debug("createProduct: tenantId='{}', key='{}', type='{}'", command.tenantId(), command.key(), command.type());
        tenantAccessService.getRequiredReference(command.tenantId());
        ProductTypeKey type = parseProductType(command.type());

        if (productRepository.existsByTenantIdAndKey(command.tenantId(), command.key())) {
            log.warn("createProduct: key='{}' ja existe no tenantId='{}'", command.key(), command.tenantId());
            throw new ProductAlreadyExistsException(command.tenantId(), command.key());
        }

        AssetStorageStrategy storageStrategy = command.assetStorageStrategy() == null
                ? AssetStorageStrategy.LOCAL
                : command.assetStorageStrategy();
        Product product = productRepository.save(new Product(
                command.tenantId(),
                command.key(),
                command.name(),
                type,
                command.defaultLocale(),
                storageStrategy
        ));
        assignmentRepository.save(new ProductAssignment(
                product,
                caller.subject(),
                ProductAssignmentRole.PRODUCT_MANAGER
        ));
        enableRecommendedModules(product, type);
        eventPublisher.publishEvent(new ProductCreatedEvent(product.getTenantId(), product.getId(), storageStrategy,
                type.name(), product.getDefaultLocale()));
        log.info("createProduct: produto criado id='{}', key='{}', type='{}'", product.getId(), product.getKey(), type);

        return productMapper.toSummary(product);
    }

    /**
     * Habilita, na ordem do catalogo (dependencias primeiro — ex. {@code CONTENT}
     * antes de {@code KNOWLEDGE_GRAPH}), os modulos recomendados para {@code type}
     * (ADR-0017, Etapa 26). {@link ProductTypeKey#CUSTOM} nao recomenda nenhum
     * modulo, entao este metodo nao faz nada para ele.
     */
    private void enableRecommendedModules(Product product, ProductTypeKey type) {
        for (ModuleKey moduleKey : ProductModuleTemplateCatalog.recommendedModulesFor(type)) {
            productModuleService.enableModule(product.getId(), moduleKey.name());
        }
    }

    @Transactional(readOnly = true)
    public List<ProductSummary> listProducts(AuthenticatedUser caller) {
        log.debug("listProducts: caller='{}'", caller.subject());
        if (caller.authorities().contains(ROLE_SUPER_ADMIN)) {
            return productRepository.findAll()
                    .stream()
                    .map(productMapper::toSummary)
                    .toList();
        }

        if (caller.authorities().contains(ROLE_TENANT_ADMIN)) {
            return tenantAccessService.findActiveTenantAdminTenantIds(caller.subject())
                    .stream()
                    .flatMap(tenantId -> productRepository.findAllByTenantId(tenantId).stream())
                    .distinct()
                    .map(productMapper::toSummary)
                    .toList();
        }

        if (hasProductRole(caller)) {
            return assignmentRepository.findAllByUserSubjectAndStatus(caller.subject(), ProductAssignmentStatus.ASSIGNED)
                    .stream()
                    .map(ProductAssignment::getProduct)
                    .distinct()
                    .map(productMapper::toSummary)
                    .toList();
        }

        return List.of();
    }

    @Transactional(readOnly = true)
    public ProductSummary getProduct(AuthenticatedUser caller, UUID productId) {
        log.debug("getProduct: productId='{}'", productId);
        return productMapper.toSummary(resolveAccessibleProduct(caller, productId));
    }

    @Transactional(readOnly = true)
    public ProductDetail getProductDetail(AuthenticatedUser caller, UUID productId) {
        log.debug("getProductDetail: productId='{}'", productId);
        Product product = resolveAccessibleProduct(caller, productId);
        List<ProductModuleSummary> modules = moduleRepository.findAllByProductId(productId)
                .stream()
                .map(moduleMapper::toSummary)
                .toList();

        return productMapper.toDetail(product, modules);
    }

    private Product resolveAccessibleProduct(AuthenticatedUser caller, UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        if (caller.authorities().contains(ROLE_SUPER_ADMIN)) {
            return product;
        }

        if (caller.authorities().contains(ROLE_TENANT_ADMIN)
                && tenantAccessService.hasActiveMembership(product.getTenantId(), caller.subject())) {
            return product;
        }

        if (hasProductRole(caller)
                && assignmentRepository.existsByProductIdAndUserSubjectAndStatus(
                productId, caller.subject(), ProductAssignmentStatus.ASSIGNED)) {
            return product;
        }

        throw new ProductNotFoundException(productId);
    }

    private ProductTypeKey parseProductType(String type) {
        ProductTypeKey productType = PRODUCT_TYPES.get(normalizeProductType(type));
        if (productType == null) {
            throw new InvalidProductTypeException(type);
        }
        return productType;
    }

    private static Map<String, ProductTypeKey> buildProductTypes() {
        Map<String, ProductTypeKey> productTypes = new HashMap<>();
        Arrays.stream(ProductTypeKey.values()).forEach(productType -> {
            productTypes.put(normalizeProductType(productType.name()), productType);
            productTypes.put(normalizeProductType(productType.label()), productType);
        });
        return Map.copyOf(productTypes);
    }

    private static String normalizeProductType(String value) {
        return String.valueOf(value).trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private boolean hasProductRole(AuthenticatedUser caller) {
        return caller.authorities().contains(ROLE_PRODUCT_MANAGER)
                || caller.authorities().contains(ROLE_EDITOR)
                || caller.authorities().contains(ROLE_VIEWER);
    }
}
