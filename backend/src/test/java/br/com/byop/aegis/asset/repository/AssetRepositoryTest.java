package br.com.byop.aegis.asset.repository;

import br.com.byop.aegis.asset.domain.Asset;
import br.com.byop.aegis.asset.domain.AssetCategory;
import br.com.byop.aegis.core.RepositoryTestSupport;
import br.com.byop.aegis.product.api.AssetStorageStrategy;
import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.repository.ProductRepository;
import br.com.byop.aegis.tenant.domain.Tenant;
import br.com.byop.aegis.tenant.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AssetRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldSaveAsset() {
        Product product = saveProduct("asset-save");

        Asset saved = assetRepository.saveAndFlush(new Asset(new Asset.Creation(
                product.getTenantId(), product.getId(), "curriculo.pdf", "application/pdf", AssetCategory.PDF,
                2048L, AssetStorageStrategy.LOCAL, "aegis/pms/tenant/product/pdf/curriculo.pdf", "subject-1"
        )));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTenantId()).isEqualTo(product.getTenantId());
        assertThat(saved.getProductId()).isEqualTo(product.getId());
        assertThat(saved.getName()).isEqualTo("curriculo.pdf");
        assertThat(saved.getFriendlyName()).isEqualTo("curriculo.pdf");
        assertThat(saved.getMimeType()).isEqualTo("application/pdf");
        assertThat(saved.getCategory()).isEqualTo(AssetCategory.PDF);
        assertThat(saved.getSizeBytes()).isEqualTo(2048L);
        assertThat(saved.getStorageProvider()).isEqualTo(AssetStorageStrategy.LOCAL);
        assertThat(saved.getStorageKey()).isEqualTo("aegis/pms/tenant/product/pdf/curriculo.pdf");
        assertThat(saved.getUploadedBySubject()).isEqualTo("subject-1");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldApplyMetadata() {
        Product product = saveProduct("asset-metadata");
        Asset asset = assetRepository.saveAndFlush(newAsset(product, "foto.png"));

        asset.applyMetadata(new Asset.Metadata("Foto da equipe", "Equipe sorrindo", "Foto institucional", "Foto: Joao"));
        assetRepository.saveAndFlush(asset);

        Asset reloaded = assetRepository.findById(asset.getId()).orElseThrow();
        assertThat(reloaded.getFriendlyName()).isEqualTo("Foto da equipe");
        assertThat(reloaded.getAltText()).isEqualTo("Equipe sorrindo");
        assertThat(reloaded.getCaption()).isEqualTo("Foto institucional");
        assertThat(reloaded.getCredit()).isEqualTo("Foto: Joao");
    }

    @Test
    void shouldKeepFriendlyNameWhenMetadataOmitsIt() {
        Product product = saveProduct("asset-metadata-keep");
        Asset asset = assetRepository.saveAndFlush(newAsset(product, "foto.png"));

        asset.applyMetadata(new Asset.Metadata(null, "Alt", "Caption", "Credit"));

        assertThat(asset.getFriendlyName()).isEqualTo("foto.png");
    }

    @Test
    void shouldFindAssetScopedByProduct() {
        Product product = saveProduct("asset-find");
        Product otherProduct = saveProduct("asset-find-other");
        Asset asset = assetRepository.saveAndFlush(newAsset(product, "foto.png"));
        assetRepository.saveAndFlush(newAsset(otherProduct, "outro.png"));

        assertThat(assetRepository.findAllByProductId(product.getId())).containsExactly(asset);
        assertThat(assetRepository.findByProductIdAndId(product.getId(), asset.getId())).contains(asset);
        assertThat(assetRepository.findByProductIdAndId(otherProduct.getId(), asset.getId())).isEmpty();
    }

    @Test
    void shouldCascadeDeleteAssetWhenProductIsDeleted() {
        Product product = saveProduct("asset-cascade");
        Asset asset = assetRepository.saveAndFlush(newAsset(product, "foto.png"));

        jdbcTemplate.update("DELETE FROM products WHERE id = ?", product.getId());

        assertThat(countRows("assets", asset.getId())).isZero();
    }

    private Asset newAsset(Product product, String name) {
        return new Asset(new Asset.Creation(product.getTenantId(), product.getId(), name, "image/png", AssetCategory.IMAGE,
                1024L, product.getAssetStorageStrategy(), "aegis/pms/x/y/image/" + name, "subject-1"));
    }

    private Product saveProduct(String suffix) {
        Tenant tenant = tenantRepository.saveAndFlush(tenant(suffix));
        return productRepository.saveAndFlush(product(tenant, suffix));
    }

    private Long countRows(String tableName, UUID id) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName + " WHERE id = ?",
                Long.class,
                id
        );
    }
}
