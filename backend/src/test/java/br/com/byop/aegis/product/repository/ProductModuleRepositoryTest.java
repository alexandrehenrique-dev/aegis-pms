package br.com.byop.aegis.product.repository;

import br.com.byop.aegis.core.RepositoryTestSupport;
import br.com.byop.aegis.product.api.ModuleKey;
import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.domain.ProductModule;
import br.com.byop.aegis.tenant.domain.Tenant;
import br.com.byop.aegis.tenant.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductModuleRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductModuleRepository moduleRepository;

    @Test
    void shouldSaveProductModule() {
        Product product = saveProduct("module-save");

        ProductModule saved = moduleRepository.saveAndFlush(new ProductModule(product, ModuleKey.CONTENT));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getProduct()).isEqualTo(product);
        assertThat(saved.getModuleKey()).isEqualTo(ModuleKey.CONTENT);
        assertThat(saved.isEnabled()).isFalse();
        assertThat(saved.getSettingsJson()).isEqualTo("{}");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldFindModulesByProductId() {
        Product product = saveProduct("module-find");
        ProductModule saved = moduleRepository.saveAndFlush(new ProductModule(product, ModuleKey.ASSETS));

        assertThat(moduleRepository.findAllByProductId(product.getId()))
                .containsExactly(saved);
        assertThat(moduleRepository.findByProductIdAndModuleKey(product.getId(), ModuleKey.ASSETS))
                .contains(saved);
    }

    @Test
    void shouldRejectDuplicateProductAndModuleKey() {
        Product product = saveProduct("module-duplicate");
        moduleRepository.saveAndFlush(new ProductModule(product, ModuleKey.CONTENT));
        ProductModule duplicate = new ProductModule(product, ModuleKey.CONTENT);

        assertThatThrownBy(() -> moduleRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldCheckEnabledModuleExists() {
        Product product = saveProduct("module-enabled");
        ProductModule module = new ProductModule(product, ModuleKey.KNOWLEDGE_GRAPH);
        module.enable();
        moduleRepository.saveAndFlush(module);

        assertThat(moduleRepository.existsByProductIdAndModuleKeyAndEnabledTrue(
                product.getId(), ModuleKey.KNOWLEDGE_GRAPH
        )).isTrue();
        assertThat(moduleRepository.existsByProductIdAndModuleKeyAndEnabledTrue(
                product.getId(), ModuleKey.CONTENT
        )).isFalse();
    }

    private Product saveProduct(String suffix) {
        Tenant tenant = tenantRepository.saveAndFlush(tenant(suffix));
        return productRepository.saveAndFlush(product(tenant, suffix));
    }
}
