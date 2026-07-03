package br.com.byop.aegis.product.service;

import br.com.byop.aegis.audit.api.AuditRecordCommand;
import br.com.byop.aegis.audit.api.AuditService;
import br.com.byop.aegis.product.api.ModuleKey;
import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.domain.ProductModule;
import br.com.byop.aegis.product.dto.ProductModuleSummary;
import br.com.byop.aegis.product.exception.InvalidModuleKeyException;
import br.com.byop.aegis.product.exception.ModuleDependencyMissingException;
import br.com.byop.aegis.product.exception.ProductNotFoundException;
import br.com.byop.aegis.product.mapper.ProductModuleMapper;
import br.com.byop.aegis.product.repository.ProductModuleRepository;
import br.com.byop.aegis.product.repository.ProductRepository;
import br.com.byop.aegis.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

@Service
public class ProductModuleService {

    private static final Map<ModuleKey, ModuleKey> DEPENDENCIES = Map.copyOf(new EnumMap<>(Map.of(
            ModuleKey.KNOWLEDGE_GRAPH, ModuleKey.CONTENT
    )));
    private static final String MODULE_PRODUCT = "PRODUCT";
    private static final String TARGET_TYPE_PRODUCT_MODULE = "ProductModule";

    private final ProductRepository productRepository;
    private final ProductModuleRepository moduleRepository;
    private final ProductModuleMapper moduleMapper;
    private final AuditService auditService;

    public ProductModuleService(ProductRepository productRepository, ProductModuleRepository moduleRepository,
                                ProductModuleMapper moduleMapper, AuditService auditService) {
        this.productRepository = productRepository;
        this.moduleRepository = moduleRepository;
        this.moduleMapper = moduleMapper;
        this.auditService = auditService;
    }

    @Transactional
    public ProductModuleSummary enableModule(UUID productId, String moduleKeyValue) {
        return enableModuleInternal(productId, moduleKeyValue, null);
    }

    @Transactional
    public ProductModuleSummary enableModule(UUID productId, String moduleKeyValue, AuthenticatedUser caller) {
        return enableModuleInternal(productId, moduleKeyValue, caller);
    }

    private ProductModuleSummary enableModuleInternal(UUID productId, String moduleKeyValue, AuthenticatedUser caller) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        ModuleKey moduleKey = parseModuleKey(moduleKeyValue);
        ModuleKey dependency = DEPENDENCIES.get(moduleKey);

        if (dependency != null && !moduleRepository.existsByProductIdAndModuleKeyAndEnabledTrue(productId, dependency)) {
            throw new ModuleDependencyMissingException(moduleKey, dependency);
        }

        ProductModule module = moduleRepository.findByProductIdAndModuleKey(productId, moduleKey)
                .orElseGet(() -> new ProductModule(product, moduleKey));
        module.enable();

        ProductModule saved = moduleRepository.save(module);
        recordModuleAudit(product, moduleKey, true, caller);
        return moduleMapper.toSummary(saved);
    }

    @Transactional
    public ProductModuleSummary disableModule(UUID productId, String moduleKeyValue) {
        return disableModuleInternal(productId, moduleKeyValue, null);
    }

    @Transactional
    public ProductModuleSummary disableModule(UUID productId, String moduleKeyValue, AuthenticatedUser caller) {
        return disableModuleInternal(productId, moduleKeyValue, caller);
    }

    private ProductModuleSummary disableModuleInternal(UUID productId, String moduleKeyValue, AuthenticatedUser caller) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        ModuleKey moduleKey = parseModuleKey(moduleKeyValue);
        validateNoEnabledDependent(productId, moduleKey);

        ProductModule module = moduleRepository.findByProductIdAndModuleKey(productId, moduleKey)
                .orElseGet(() -> new ProductModule(product, moduleKey));
        module.disable();

        ProductModule saved = moduleRepository.save(module);
        recordModuleAudit(product, moduleKey, false, caller);
        return moduleMapper.toSummary(saved);
    }

    private void validateNoEnabledDependent(UUID productId, ModuleKey moduleKey) {
        DEPENDENCIES.entrySet()
                .stream()
                .filter(entry -> entry.getValue() == moduleKey)
                .filter(entry -> moduleRepository.existsByProductIdAndModuleKeyAndEnabledTrue(productId, entry.getKey()))
                .findFirst()
                .ifPresent(entry -> {
                    throw new ModuleDependencyMissingException(entry.getKey(), moduleKey);
                });
    }

    private ModuleKey parseModuleKey(String moduleKeyValue) {
        try {
            return ModuleKey.valueOf(moduleKeyValue);
        } catch (IllegalArgumentException | NullPointerException _) {
            throw new InvalidModuleKeyException(moduleKeyValue);
        }
    }

    private void recordModuleAudit(Product product, ModuleKey moduleKey, boolean enabled, AuthenticatedUser caller) {
        if (caller == null) {
            return;
        }
        String action = enabled ? "MODULE_ENABLED" : "MODULE_DISABLED";
        auditService.recordEvent(new AuditRecordCommand(
                product.getTenantId(),
                product.getId(),
                caller.subject(),
                action,
                TARGET_TYPE_PRODUCT_MODULE,
                moduleKey.name(),
                moduleKey.name(),
                MODULE_PRODUCT,
                null,
                Map.of("moduleKey", moduleKey.name(), "enabled", enabled)
        ));
    }
}
