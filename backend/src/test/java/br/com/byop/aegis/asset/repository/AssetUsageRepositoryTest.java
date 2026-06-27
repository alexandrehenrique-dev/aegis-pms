package br.com.byop.aegis.asset.repository;

import br.com.byop.aegis.asset.domain.Asset;
import br.com.byop.aegis.asset.domain.AssetCategory;
import br.com.byop.aegis.asset.domain.AssetUsage;
import br.com.byop.aegis.core.RepositoryTestSupport;
import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.repository.ProductRepository;
import br.com.byop.aegis.tenant.domain.Tenant;
import br.com.byop.aegis.tenant.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AssetUsageRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private AssetUsageRepository assetUsageRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldSaveUsage() {
        Product product = saveProduct("usage-save");
        Asset asset = saveAsset(product, "foto.png");

        AssetUsage saved = assetUsageRepository.saveAndFlush(
                new AssetUsage(asset.getId(), "CONTENT", "content-123", "Artigo Home")
        );

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAssetId()).isEqualTo(asset.getId());
        assertThat(saved.getUsedInType()).isEqualTo("CONTENT");
        assertThat(saved.getUsedInRefId()).isEqualTo("content-123");
        assertThat(saved.getUsedInLabel()).isEqualTo("Artigo Home");
    }

    @Test
    void shouldFindUsagesByAsset() {
        Product product = saveProduct("usage-find");
        Asset asset = saveAsset(product, "foto.png");
        Asset otherAsset = saveAsset(product, "outra.png");
        AssetUsage usage = assetUsageRepository.saveAndFlush(new AssetUsage(asset.getId(), "CONTENT", "content-1", "Artigo"));
        assetUsageRepository.saveAndFlush(new AssetUsage(otherAsset.getId(), "SEO", "seo-1", "Meta description"));

        assertThat(assetUsageRepository.findAllByAssetId(asset.getId())).containsExactly(usage);
    }

    @Test
    void shouldDetectAssetWithUsage() {
        Product product = saveProduct("usage-exists");
        Asset asset = saveAsset(product, "foto.png");
        Asset unusedAsset = saveAsset(product, "sem-uso.png");
        assetUsageRepository.saveAndFlush(new AssetUsage(asset.getId(), "CONTENT", "content-1", "Artigo"));

        assertThat(assetUsageRepository.existsByAssetId(asset.getId())).isTrue();
        assertThat(assetUsageRepository.existsByAssetId(unusedAsset.getId())).isFalse();
    }

    @Test
    void shouldCascadeDeleteUsageWhenAssetIsDeleted() {
        Product product = saveProduct("usage-cascade");
        Asset asset = saveAsset(product, "foto.png");
        AssetUsage usage = assetUsageRepository.saveAndFlush(new AssetUsage(asset.getId(), "CONTENT", "content-1", "Artigo"));

        jdbcTemplate.update("DELETE FROM assets WHERE id = ?", asset.getId());

        assertThat(countRows(usage.getId())).isZero();
    }

    private Asset saveAsset(Product product, String name) {
        return assetRepository.saveAndFlush(new Asset(new Asset.Creation(product.getTenantId(), product.getId(), name, "image/png",
                AssetCategory.IMAGE, 1024L, product.getAssetStorageStrategy(), "aegis/pms/x/y/image/" + name, "subject-1")));
    }

    private Product saveProduct(String suffix) {
        Tenant tenant = tenantRepository.saveAndFlush(tenant(suffix));
        return productRepository.saveAndFlush(product(tenant, suffix));
    }

    private Long countRows(UUID id) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM asset_usages WHERE id = ?",
                Long.class,
                id
        );
    }
}
