package br.com.byop.aegis.product.api;

import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.domain.ProductAssignment;
import br.com.byop.aegis.product.domain.ProductAssignmentRole;
import br.com.byop.aegis.product.domain.ProductModule;
import br.com.byop.aegis.product.domain.ProductStatus;
import br.com.byop.aegis.product.domain.ProductTypeKey;
import br.com.byop.aegis.product.repository.ProductAssignmentRepository;
import br.com.byop.aegis.product.repository.ProductModuleRepository;
import br.com.byop.aegis.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Porta publica do modulo product para seeds locais idempotentes.
 */
@Service
public class ProductSeedService {

    private static final String DEFAULT_LOCALE = "pt-BR";
    private static final Map<String, ProductTypeKey> PRODUCT_TYPES = buildProductTypes();

    private final ProductRepository productRepository;
    private final ProductModuleRepository moduleRepository;
    private final ProductAssignmentRepository assignmentRepository;

    public ProductSeedService(ProductRepository productRepository, ProductModuleRepository moduleRepository,
                              ProductAssignmentRepository assignmentRepository) {
        this.productRepository = productRepository;
        this.moduleRepository = moduleRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional
    public ProductSeedReference ensureProduct(ProductSeedCommand command) {
        ProductTypeKey productType = parseProductType(command.type());
        Product product = productRepository.findByTenantIdAndKey(command.tenantId(), command.key())
                .orElseGet(() -> new Product(command.tenantId(), command.key(), command.name(), productType, DEFAULT_LOCALE));
        product.rename(command.name());
        applyProductStatus(product, command.status());
        Product saved = productRepository.save(product);
        syncModules(saved, command.enabledModules());
        return new ProductSeedReference(saved.getId(), saved.getTenantId(), saved.getKey(), saved.getName());
    }

    @Transactional
    public void ensureAssignment(UUID productId, String userSubject, String role) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found for seed: " + productId));
        ProductAssignmentRole assignmentRole = ProductAssignmentRole.valueOf(normalize(role));
        ProductAssignment assignment = assignmentRepository.findByProductIdAndUserSubject(productId, userSubject)
                .orElseGet(() -> new ProductAssignment(product, userSubject, assignmentRole));
        assignment.changeRole(assignmentRole);
        assignment.assign();
        assignmentRepository.save(assignment);
    }

    private void syncModules(Product product, Set<ModuleKey> enabledModules) {
        Set<ModuleKey> modules = enabledModules == null ? Set.of() : Set.copyOf(enabledModules);
        Arrays.stream(ModuleKey.values()).forEach(moduleKey -> {
            ProductModule module = moduleRepository.findByProductIdAndModuleKey(product.getId(), moduleKey)
                    .orElseGet(() -> new ProductModule(product, moduleKey));
            if (modules.contains(moduleKey)) {
                module.enable();
            } else {
                module.disable();
            }
            moduleRepository.save(module);
        });
    }

    private void applyProductStatus(Product product, String status) {
        ProductStatus parsedStatus = ProductStatus.valueOf(normalize(status));
        if (parsedStatus == ProductStatus.ACTIVE) {
            product.activate();
            return;
        }
        if (parsedStatus == ProductStatus.SUSPENDED) {
            product.suspend();
            return;
        }
        product.archive();
    }

    private ProductTypeKey parseProductType(String type) {
        return PRODUCT_TYPES.get(normalize(type));
    }

    private static Map<String, ProductTypeKey> buildProductTypes() {
        return Arrays.stream(ProductTypeKey.values())
                .flatMap(productType -> java.util.stream.Stream.of(
                        Map.entry(normalize(productType.name()), productType),
                        Map.entry(normalize(productType.label()), productType)
                ))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue, (left, _) -> left));
    }

    private static String normalize(String value) {
        return Objects.toString(value, "").trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }
}
