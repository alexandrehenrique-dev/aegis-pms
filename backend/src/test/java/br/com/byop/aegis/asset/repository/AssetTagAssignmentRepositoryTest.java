package br.com.byop.aegis.asset.repository;

import br.com.byop.aegis.asset.domain.Asset;
import br.com.byop.aegis.asset.domain.AssetCategory;
import br.com.byop.aegis.asset.domain.AssetTag;
import br.com.byop.aegis.asset.domain.AssetTagAssignment;
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

class AssetTagAssignmentRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private AssetTagRepository assetTagRepository;

    @Autowired
    private AssetTagAssignmentRepository assignmentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldAssignTagToAsset() {
        Product product = saveProduct("assignment-save");
        Asset asset = saveAsset(product, "foto.png");
        AssetTag tag = assetTagRepository.saveAndFlush(new AssetTag(product.getId(), "blog"));

        AssetTagAssignment saved = assignmentRepository.saveAndFlush(new AssetTagAssignment(asset.getId(), tag.getId()));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAssetId()).isEqualTo(asset.getId());
        assertThat(saved.getAssetTagId()).isEqualTo(tag.getId());
    }

    @Test
    void shouldFindAssignmentsByAsset() {
        Product product = saveProduct("assignment-find");
        Asset asset = saveAsset(product, "foto.png");
        Asset otherAsset = saveAsset(product, "outra.png");
        AssetTag tag = assetTagRepository.saveAndFlush(new AssetTag(product.getId(), "blog"));
        AssetTagAssignment assignment = assignmentRepository.saveAndFlush(new AssetTagAssignment(asset.getId(), tag.getId()));
        assignmentRepository.saveAndFlush(new AssetTagAssignment(otherAsset.getId(), tag.getId()));

        assertThat(assignmentRepository.findAllByAssetId(asset.getId())).containsExactly(assignment);
    }

    @Test
    void shouldDetectExistingAssignment() {
        Product product = saveProduct("assignment-exists");
        Asset asset = saveAsset(product, "foto.png");
        AssetTag tag = assetTagRepository.saveAndFlush(new AssetTag(product.getId(), "blog"));
        assignmentRepository.saveAndFlush(new AssetTagAssignment(asset.getId(), tag.getId()));

        assertThat(assignmentRepository.existsByAssetIdAndAssetTagId(asset.getId(), tag.getId())).isTrue();
        assertThat(assignmentRepository.existsByAssetIdAndAssetTagId(asset.getId(), UUID.randomUUID())).isFalse();
    }

    @Test
    void shouldDeleteAssignmentByAssetAndTag() {
        Product product = saveProduct("assignment-delete");
        Asset asset = saveAsset(product, "foto.png");
        AssetTag tag = assetTagRepository.saveAndFlush(new AssetTag(product.getId(), "blog"));
        assignmentRepository.saveAndFlush(new AssetTagAssignment(asset.getId(), tag.getId()));

        assignmentRepository.deleteByAssetIdAndAssetTagId(asset.getId(), tag.getId());
        assignmentRepository.flush();

        assertThat(assignmentRepository.existsByAssetIdAndAssetTagId(asset.getId(), tag.getId())).isFalse();
    }

    @Test
    void shouldCascadeDeleteAssignmentWhenAssetIsDeleted() {
        Product product = saveProduct("assignment-cascade-asset");
        Asset asset = saveAsset(product, "foto.png");
        AssetTag tag = assetTagRepository.saveAndFlush(new AssetTag(product.getId(), "blog"));
        AssetTagAssignment assignment = assignmentRepository.saveAndFlush(new AssetTagAssignment(asset.getId(), tag.getId()));

        jdbcTemplate.update("DELETE FROM assets WHERE id = ?", asset.getId());

        assertThat(countRows(assignment.getId())).isZero();
    }

    @Test
    void shouldCascadeDeleteAssignmentWhenTagIsDeleted() {
        Product product = saveProduct("assignment-cascade-tag");
        Asset asset = saveAsset(product, "foto.png");
        AssetTag tag = assetTagRepository.saveAndFlush(new AssetTag(product.getId(), "blog"));
        AssetTagAssignment assignment = assignmentRepository.saveAndFlush(new AssetTagAssignment(asset.getId(), tag.getId()));

        jdbcTemplate.update("DELETE FROM asset_tags WHERE id = ?", tag.getId());

        assertThat(countRows(assignment.getId())).isZero();
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
                "SELECT COUNT(*) FROM asset_tag_assignments WHERE id = ?",
                Long.class,
                id
        );
    }
}
