package br.com.byop.aegis.product.service;

import br.com.byop.aegis.product.api.ModuleKey;
import br.com.byop.aegis.product.command.CreateProductCommand;
import br.com.byop.aegis.product.command.UpdateProductCommand;
import br.com.byop.aegis.product.api.AssetStorageStrategy;
import br.com.byop.aegis.product.api.ProductCreatedEvent;
import br.com.byop.aegis.product.domain.*;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductService {

    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String ROLE_TENANT_ADMIN = "ROLE_TENANT_ADMIN";
    private static final Map<String, ProductTypeKey> PRODUCT_TYPES = buildProductTypes();
    private static final Map<String, ProductStatus> PRODUCT_STATUSES = buildProductStatuses();
    private static final Map<String, ModuleKey> PRODUCT_MODULE_KEYS = buildProductModuleKeys();

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

        return toSummaryWithModuleCount(product, ProductAssignmentRole.PRODUCT_MANAGER);
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
            Map<UUID, ProductAssignmentRole> callerRoles = callerRolesByProductId(caller.subject());
            return productRepository.findAll()
                    .stream()
                    .map(product -> toSummaryWithModuleCount(product, callerRoles.get(product.getId())))
                    .toList();
        }

        List<UUID> tenantAdminTenantIds = tenantAccessService.findActiveTenantAdminTenantIds(caller.subject());
        if (caller.authorities().contains(ROLE_TENANT_ADMIN) || !tenantAdminTenantIds.isEmpty()) {
            Map<UUID, ProductAssignmentRole> callerRoles = callerRolesByProductId(caller.subject());
            return tenantAdminTenantIds.stream()
                    .flatMap(tenantId -> productRepository.findAllByTenantId(tenantId).stream())
                    .distinct()
                    .map(product -> toSummaryWithModuleCount(product, callerRoles.get(product.getId())))
                    .toList();
        }

        return assignmentRepository.findAllByUserSubjectAndStatus(caller.subject(), ProductAssignmentStatus.ASSIGNED)
                .stream()
                .map(assignment -> toSummaryWithModuleCount(assignment.getProduct(), assignment.getRole()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductSummary getProduct(AuthenticatedUser caller, UUID productId) {
        log.debug("getProduct: productId='{}'", productId);
        Product product = resolveAccessibleProduct(caller, productId);
        return toSummaryWithModuleCount(product, callerRoleForProduct(caller.subject(), productId));
    }

    @Transactional
    public ProductSummary updateProduct(AuthenticatedUser caller, UUID productId, UpdateProductCommand command) {
        log.debug("updateProduct: productId='{}', status='{}'", productId, command.status());
        Product product = resolveManageableProduct(caller, productId);
        product.rename(command.name());
        product.changeType(parseProductType(command.type()));
        product.changeStatus(parseProductStatus(command.status()));
        Set<ModuleKey> syncedModuleKeys = syncModules(product, command.modules());
        log.info("updateProduct: produto atualizado id='{}', status='{}'", productId, product.getStatus());
        if (!syncedModuleKeys.isEmpty()) {
            return toSummaryWithModules(product, syncedModuleKeys, callerRoleForProduct(caller.subject(), productId));
        }
        return toSummaryWithModuleCount(product, callerRoleForProduct(caller.subject(), productId));
    }

    private Set<ModuleKey> syncModules(Product product, List<String> moduleValues) {
        if (moduleValues == null) {
            return Set.of();
        }

        Set<ModuleKey> enabledModuleKeys = expandRequiredModules(moduleValues.stream()
                .map(ProductService::parseProductModuleKey)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        List<ProductModule> configuredModules = moduleRepository.findAllByProductId(product.getId());
        Map<ModuleKey, ProductModule> modulesByKey = configuredModules.stream()
                .collect(Collectors.toMap(ProductModule::getModuleKey, Function.identity()));

        for (ProductModule module : configuredModules) {
            if (enabledModuleKeys.contains(module.getModuleKey())) {
                module.enable();
            } else {
                module.disable();
            }
            moduleRepository.save(module);
        }

        enabledModuleKeys.stream()
                .filter(moduleKey -> !modulesByKey.containsKey(moduleKey))
                .map(moduleKey -> enabledModule(product, moduleKey))
                .forEach(moduleRepository::save);
        moduleRepository.flush();
        return enabledModuleKeys;
    }

    private static ProductModule enabledModule(Product product, ModuleKey moduleKey) {
        ProductModule module = new ProductModule(product, moduleKey);
        module.enable();
        return module;
    }

    private static Set<ModuleKey> expandRequiredModules(Set<ModuleKey> moduleKeys) {
        LinkedHashSet<ModuleKey> expandedModuleKeys = new LinkedHashSet<>(moduleKeys);
        if (moduleKeys.contains(ModuleKey.KNOWLEDGE_GRAPH)) {
            expandedModuleKeys.add(ModuleKey.CONTENT);
            expandedModuleKeys.add(ModuleKey.ASSETS);
        }
        return expandedModuleKeys;
    }

    /**
     * O papel de plataforma do caller (Keycloak realm role, ex. ROLE_SUPER_ADMIN)
     * e o papel dele NUM PRODUTO especifico ({@link ProductAssignmentRole}) sao
     * independentes — um Super Admin pode alem disso ser Editor de um produto
     * pontual. Este campo carrega o segundo, para o frontend mesclar a
     * navegacao da sidebar quando os dois coexistem.
     */
    private Map<UUID, ProductAssignmentRole> callerRolesByProductId(String subject) {
        return assignmentRepository.findAllByUserSubjectAndStatus(subject, ProductAssignmentStatus.ASSIGNED)
                .stream()
                .collect(java.util.stream.Collectors.toMap(ProductAssignment::getProductId, ProductAssignment::getRole));
    }

    private ProductAssignmentRole callerRoleForProduct(String subject, UUID productId) {
        return assignmentRepository.findByProductIdAndUserSubject(productId, subject)
                .filter(assignment -> assignment.getStatus() == ProductAssignmentStatus.ASSIGNED)
                .map(ProductAssignment::getRole)
                .orElse(null);
    }

    private ProductSummary toSummaryWithModuleCount(Product product, ProductAssignmentRole callerRole) {
        List<String> enabledModules = moduleRepository.findAllByProductId(product.getId())
                .stream()
                .filter(ProductModule::isEnabled)
                .map(module -> module.getModuleKey().name())
                .toList();
        int enabledModuleCount = enabledModules.size();
        String callerRoleName = callerRole == null ? null : callerRole.name();
        return productMapper.toSummary(product, enabledModuleCount, enabledModules, callerRoleName);
    }

    private ProductSummary toSummaryWithModules(Product product, Set<ModuleKey> enabledModuleKeys,
                                                ProductAssignmentRole callerRole) {
        List<String> enabledModules = enabledModuleKeys.stream()
                .map(ModuleKey::name)
                .toList();
        String callerRoleName = callerRole == null ? null : callerRole.name();
        return productMapper.toSummary(product, enabledModules.size(), enabledModules, callerRoleName);
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

        if ((caller.authorities().contains(ROLE_TENANT_ADMIN)
                && tenantAccessService.hasActiveMembership(product.getTenantId(), caller.subject()))
                || tenantAccessService.hasActiveTenantAdminMembership(product.getTenantId(), caller.subject())) {
            return product;
        }

        if (assignmentRepository.existsByProductIdAndUserSubjectAndStatus(
                productId, caller.subject(), ProductAssignmentStatus.ASSIGNED)) {
            return product;
        }

        throw new ProductNotFoundException(productId);
    }

    private Product resolveManageableProduct(AuthenticatedUser caller, UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        if (caller.authorities().contains(ROLE_SUPER_ADMIN)
                || tenantAccessService.hasActiveTenantAdminMembership(product.getTenantId(), caller.subject())) {
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

    private ProductStatus parseProductStatus(String status) {
        ProductStatus productStatus = PRODUCT_STATUSES.get(normalizeProductType(status));
        if (productStatus == null) {
            throw new InvalidProductStatusException(status);
        }
        return productStatus;
    }

    private static ModuleKey parseProductModuleKey(String moduleKeyValue) {
        ModuleKey moduleKey = PRODUCT_MODULE_KEYS.get(normalizeModuleKey(moduleKeyValue));
        if (moduleKey == null) {
            throw new IllegalArgumentException("Invalid product module: " + moduleKeyValue);
        }
        return moduleKey;
    }

    private static Map<String, ProductTypeKey> buildProductTypes() {
        Map<String, ProductTypeKey> productTypes = new HashMap<>();
        Arrays.stream(ProductTypeKey.values()).forEach(productType -> {
            productTypes.put(normalizeProductType(productType.name()), productType);
            productTypes.put(normalizeProductType(productType.label()), productType);
        });
        return Map.copyOf(productTypes);
    }

    private static Map<String, ProductStatus> buildProductStatuses() {
        Map<String, ProductStatus> productStatuses = new HashMap<>();
        productStatuses.put("ACTIVE", ProductStatus.ACTIVE);
        productStatuses.put("ATIVO", ProductStatus.ACTIVE);
        productStatuses.put("ARCHIVED", ProductStatus.ARCHIVED);
        productStatuses.put("ARQUIVADO", ProductStatus.ARCHIVED);
        productStatuses.put("SUSPENDED", ProductStatus.SUSPENDED);
        productStatuses.put("SUSPENSO", ProductStatus.SUSPENDED);
        productStatuses.put("PENDING", ProductStatus.SUSPENDED);
        productStatuses.put("INACTIVE", ProductStatus.SUSPENDED);
        return Map.copyOf(productStatuses);
    }

    private static Map<String, ModuleKey> buildProductModuleKeys() {
        Map<String, ModuleKey> moduleKeys = new HashMap<>();
        Arrays.stream(ModuleKey.values()).forEach(moduleKey -> moduleKeys.put(normalizeModuleKey(moduleKey.name()), moduleKey));
        moduleKeys.put(normalizeModuleKey("Páginas"), ModuleKey.PAGES);
        moduleKeys.put(normalizeModuleKey("Conteúdo"), ModuleKey.CONTENT);
        moduleKeys.put(normalizeModuleKey("Formulários"), ModuleKey.FORMS);
        moduleKeys.put(normalizeModuleKey("Grafo de conhecimento"), ModuleKey.KNOWLEDGE_GRAPH);
        moduleKeys.put(normalizeModuleKey("Knowledge Graph"), ModuleKey.KNOWLEDGE_GRAPH);
        return Map.copyOf(moduleKeys);
    }

    private static String normalizeProductType(String value) {
        return String.valueOf(value).trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private static String normalizeModuleKey(String value) {
        String normalizedValue = Normalizer.normalize(String.valueOf(value).trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalizedValue.toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }
}
