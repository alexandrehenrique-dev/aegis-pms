package br.com.byop.aegis.product.service;

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

    private final ProductRepository productRepository;
    private final ProductModuleRepository moduleRepository;
    private final ProductModuleMapper moduleMapper;

    public ProductModuleService(ProductRepository productRepository, ProductModuleRepository moduleRepository,
                                ProductModuleMapper moduleMapper) {
        this.productRepository = productRepository;
        this.moduleRepository = moduleRepository;
        this.moduleMapper = moduleMapper;
    }

    @Transactional
    public ProductModuleSummary enableModule(UUID productId, String moduleKeyValue) {
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

        return moduleMapper.toSummary(moduleRepository.save(module));
    }

    @Transactional
    public ProductModuleSummary disableModule(UUID productId, String moduleKeyValue) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        ModuleKey moduleKey = parseModuleKey(moduleKeyValue);
        validateNoEnabledDependent(productId, moduleKey);

        ProductModule module = moduleRepository.findByProductIdAndModuleKey(productId, moduleKey)
                .orElseGet(() -> new ProductModule(product, moduleKey));
        module.disable();

        return moduleMapper.toSummary(moduleRepository.save(module));
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
}
