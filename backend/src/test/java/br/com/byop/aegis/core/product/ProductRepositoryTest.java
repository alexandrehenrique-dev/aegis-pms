package br.com.byop.aegis.core.product;

import br.com.byop.aegis.core.RepositoryTestSupport;
import br.com.byop.aegis.core.tenant.Tenant;
import br.com.byop.aegis.core.tenant.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void shouldSaveProductLinkedToTenant() {
        Tenant tenant = tenantRepository.saveAndFlush(tenant("product-save"));

        Product saved = productRepository.saveAndFlush(product(tenant, "save"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTenant()).isEqualTo(tenant);
        assertThat(saved.getKey()).isEqualTo("product-save");
        assertThat(saved.getName()).isEqualTo("Product save");
        assertThat(saved.getType()).isEqualTo(ProductTypeKey.SITE_INSTITUCIONAL);
        assertThat(saved.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(saved.getDefaultLocale()).isEqualTo("pt-BR");
        assertThat(saved.getAssetStorageStrategy()).isEqualTo(AssetStorageStrategy.LOCAL);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldFindProductsByTenant() {
        Tenant tenant = tenantRepository.saveAndFlush(tenant("product-find"));
        Product saved = productRepository.saveAndFlush(product(tenant, "find"));

        assertThat(productRepository.findAllByTenantId(tenant.getId()))
                .containsExactly(saved);
        assertThat(productRepository.findByTenantIdAndKey(tenant.getId(), "product-find"))
                .contains(saved);
        assertThat(productRepository.existsByTenantIdAndKey(tenant.getId(), "product-find"))
                .isTrue();
    }

    @Test
    void shouldRejectDuplicateProductKeyWithinTenant() {
        Tenant tenant = tenantRepository.saveAndFlush(tenant("product-duplicate"));
        productRepository.saveAndFlush(product(tenant, "same-key"));
        Product duplicate = product(tenant, "same-key");

        assertThatThrownBy(() -> productRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldAllowSameProductKeyAcrossDifferentTenants() {
        Tenant firstTenant = tenantRepository.saveAndFlush(tenant("product-tenant-a"));
        Tenant secondTenant = tenantRepository.saveAndFlush(tenant("product-tenant-b"));

        Product first = productRepository.saveAndFlush(product(firstTenant, "shared-key"));
        Product second = productRepository.saveAndFlush(product(secondTenant, "shared-key"));

        assertThat(first.getKey()).isEqualTo(second.getKey());
        assertThat(first.getTenant()).isNotEqualTo(second.getTenant());
    }
}
