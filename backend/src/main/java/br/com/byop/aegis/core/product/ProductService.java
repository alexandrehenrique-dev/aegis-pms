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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ProductService {

    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String ROLE_TENANT_ADMIN = "ROLE_TENANT_ADMIN";
    private static final String ROLE_PRODUCT_MANAGER = "ROLE_PRODUCT_MANAGER";
    private static final String ROLE_EDITOR = "ROLE_EDITOR";
    private static final String ROLE_VIEWER = "ROLE_VIEWER";
    private static final String TENANT_ADMIN = "TENANT_ADMIN";
    private static final Map<String, ProductTypeKey> PRODUCT_TYPES = buildProductTypes();

    private final ProductRepository productRepository;
    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;
    private final ProductAssignmentRepository assignmentRepository;
    private final ProductModuleRepository moduleRepository;
    private final ProductModuleMapper moduleMapper;
    private final ProductMapper productMapper;

    public ProductService(ProductRepository productRepository, TenantRepository tenantRepository,
                          TenantMembershipRepository membershipRepository,
                          ProductAssignmentRepository assignmentRepository, ProductModuleRepository moduleRepository,
                          ProductModuleMapper moduleMapper, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
        this.assignmentRepository = assignmentRepository;
        this.moduleRepository = moduleRepository;
        this.moduleMapper = moduleMapper;
        this.productMapper = productMapper;
    }

    @Transactional
    public ProductSummary createProduct(AuthenticatedUser caller, CreateProductCommand command) {
        Tenant tenant = tenantRepository.findById(command.tenantId())
                .orElseThrow(() -> new TenantNotFoundException(command.tenantId()));
        ProductTypeKey type = parseProductType(command.type());

        if (productRepository.existsByTenantIdAndKey(command.tenantId(), command.key())) {
            throw new ProductAlreadyExistsException(command.tenantId(), command.key());
        }

        AssetStorageStrategy storageStrategy = command.assetStorageStrategy() == null
                ? AssetStorageStrategy.LOCAL
                : command.assetStorageStrategy();
        Product product = productRepository.save(new Product(
                tenant,
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

        return productMapper.toSummary(product);
    }

    @Transactional(readOnly = true)
    public List<ProductSummary> listProducts(AuthenticatedUser caller) {
        if (caller.authorities().contains(ROLE_SUPER_ADMIN)) {
            return productRepository.findAll()
                    .stream()
                    .map(productMapper::toSummary)
                    .toList();
        }

        if (caller.authorities().contains(ROLE_TENANT_ADMIN)) {
            return membershipRepository.findAllByUserSubject(caller.subject())
                    .stream()
                    .filter(this::isActiveTenantAdminMembership)
                    .flatMap(membership -> productRepository.findAllByTenantId(membership.getTenant().getId()).stream())
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
        return productMapper.toSummary(resolveAccessibleProduct(caller, productId));
    }

    @Transactional(readOnly = true)
    public ProductDetail getProductDetail(AuthenticatedUser caller, UUID productId) {
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
                && membershipRepository.existsByTenantIdAndUserSubjectAndStatus(
                product.getTenant().getId(), caller.subject(), TenantMembershipStatus.ACTIVE)) {
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

    private boolean isActiveTenantAdminMembership(TenantMembership membership) {
        return membership.getStatus() == TenantMembershipStatus.ACTIVE && TENANT_ADMIN.equals(membership.getRole());
    }

    private boolean hasProductRole(AuthenticatedUser caller) {
        return caller.authorities().contains(ROLE_PRODUCT_MANAGER)
                || caller.authorities().contains(ROLE_EDITOR)
                || caller.authorities().contains(ROLE_VIEWER);
    }
}
