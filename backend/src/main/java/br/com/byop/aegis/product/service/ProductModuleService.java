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
import br.com.byop.aegis.tenant.api.TenantAccessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class ProductModuleService {

    private static final Map<ModuleKey, List<ModuleKey>> DEPENDENCIES = buildDependencies();
    private static final String MODULE_PRODUCT = "PRODUCT";
    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String TARGET_TYPE_PRODUCT_MODULE = "ProductModule";

    private final ProductRepository productRepository;
    private final TenantAccessService tenantAccessService;
    private final ProductModuleRepository moduleRepository;
    private final ProductModuleMapper moduleMapper;
    private final AuditService auditService;

    private static Map<ModuleKey, List<ModuleKey>> buildDependencies() {
        Map<ModuleKey, List<ModuleKey>> dependencies = new EnumMap<>(ModuleKey.class);
        dependencies.put(ModuleKey.KNOWLEDGE_GRAPH, List.of(ModuleKey.CONTENT, ModuleKey.ASSETS));
        return Map.copyOf(dependencies);
    }

    public ProductModuleService(ProductRepository productRepository, TenantAccessService tenantAccessService,
                                ProductModuleRepository moduleRepository, ProductModuleMapper moduleMapper,
                                AuditService auditService) {
        this.productRepository = productRepository;
        this.tenantAccessService = tenantAccessService;
        this.moduleRepository = moduleRepository;
        this.moduleMapper = moduleMapper;
        this.auditService = auditService;
    }

    @Transactional
    public ProductModuleSummary enableModule(UUID productId, String moduleKeyValue) {
        log.debug("enableModule: productId='{}', moduleKey='{}'", productId, moduleKeyValue);
        return enableModuleInternal(productId, moduleKeyValue, null);
    }

    @Transactional
    public ProductModuleSummary enableModule(UUID productId, String moduleKeyValue, AuthenticatedUser caller) {
        log.debug("enableModule: productId='{}', moduleKey='{}', caller='{}'", productId, moduleKeyValue, caller.subject());
        return enableModuleInternal(productId, moduleKeyValue, caller);
    }

    private ProductModuleSummary enableModuleInternal(UUID productId, String moduleKeyValue, AuthenticatedUser caller) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        ModuleKey moduleKey = parseModuleKey(moduleKeyValue);
        assertModuleManager(product, caller);
        List<ModuleKey> dependencies = DEPENDENCIES.getOrDefault(moduleKey, List.of());
        for (ModuleKey dependency : dependencies) {
            if (!moduleRepository.existsByProductIdAndModuleKeyAndEnabledTrue(productId, dependency)) {
                log.warn("enableModule: dependencia ausente productId='{}', moduleKey='{}', dependency='{}'", productId, moduleKey, dependency);
                throw new ModuleDependencyMissingException(moduleKey, dependency);
            }
        }

        ProductModule module = moduleRepository.findByProductIdAndModuleKey(productId, moduleKey)
                .orElseGet(() -> new ProductModule(product, moduleKey));
        module.enable();

        ProductModule saved = moduleRepository.save(module);
        recordModuleAudit(product, moduleKey, true, caller);
        log.info("enableModule: modulo habilitado productId='{}', moduleKey='{}'", productId, moduleKey);
        return moduleMapper.toSummary(saved);
    }

    @Transactional
    public ProductModuleSummary disableModule(UUID productId, String moduleKeyValue) {
        log.debug("disableModule: productId='{}', moduleKey='{}'", productId, moduleKeyValue);
        return disableModuleInternal(productId, moduleKeyValue, null);
    }

    @Transactional
    public ProductModuleSummary disableModule(UUID productId, String moduleKeyValue, AuthenticatedUser caller) {
        log.debug("disableModule: productId='{}', moduleKey='{}', caller='{}'", productId, moduleKeyValue, caller.subject());
        return disableModuleInternal(productId, moduleKeyValue, caller);
    }

    private ProductModuleSummary disableModuleInternal(UUID productId, String moduleKeyValue, AuthenticatedUser caller) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        ModuleKey moduleKey = parseModuleKey(moduleKeyValue);
        assertModuleManager(product, caller);
        validateNoEnabledDependent(productId, moduleKey);

        ProductModule module = moduleRepository.findByProductIdAndModuleKey(productId, moduleKey)
                .orElseGet(() -> new ProductModule(product, moduleKey));
        module.disable();

        ProductModule saved = moduleRepository.save(module);
        recordModuleAudit(product, moduleKey, false, caller);
        log.info("disableModule: modulo desabilitado productId='{}', moduleKey='{}'", productId, moduleKey);
        return moduleMapper.toSummary(saved);
    }

    private void validateNoEnabledDependent(UUID productId, ModuleKey moduleKey) {
        DEPENDENCIES.entrySet()
                .stream()
                .filter(entry -> entry.getValue().contains(moduleKey))
                .filter(entry -> moduleRepository.existsByProductIdAndModuleKeyAndEnabledTrue(productId, entry.getKey()))
                .findFirst()
                .ifPresent(entry -> {
                    log.warn("disableModule: modulo dependente habilitado productId='{}', moduleKey='{}', dependent='{}'", productId, moduleKey, entry.getKey());
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

    private void assertModuleManager(Product product, AuthenticatedUser caller) {
        if (caller == null || caller.authorities().contains(ROLE_SUPER_ADMIN)) {
            return;
        }
        if (tenantAccessService.hasActiveTenantAdminMembership(product.getTenantId(), caller.subject())) {
            return;
        }
        throw new ProductNotFoundException(product.getId());
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
