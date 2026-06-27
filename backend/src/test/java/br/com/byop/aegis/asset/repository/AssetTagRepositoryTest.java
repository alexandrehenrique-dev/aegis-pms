package br.com.byop.aegis.asset.repository;

import br.com.byop.aegis.asset.domain.AssetTag;
import br.com.byop.aegis.core.RepositoryTestSupport;
import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.repository.ProductRepository;
import br.com.byop.aegis.tenant.domain.Tenant;
import br.com.byop.aegis.tenant.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssetTagRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AssetTagRepository assetTagRepository;

    @Test
    void shouldSaveTag() {
        Product product = saveProduct("tag-save");

        AssetTag saved = assetTagRepository.saveAndFlush(new AssetTag(product.getId(), "institucional"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getProductId()).isEqualTo(product.getId());
        assertThat(saved.getName()).isEqualTo("institucional");
    }

    @Test
    void shouldFindTagsScopedByProduct() {
        Product product = saveProduct("tag-find");
        Product otherProduct = saveProduct("tag-find-other");
        AssetTag tag = assetTagRepository.saveAndFlush(new AssetTag(product.getId(), "blog"));
        assetTagRepository.saveAndFlush(new AssetTag(otherProduct.getId(), "blog"));

        assertThat(assetTagRepository.findAllByProductId(product.getId())).containsExactly(tag);
        assertThat(assetTagRepository.findByProductIdAndName(product.getId(), "blog")).contains(tag);
        assertThat(assetTagRepository.findByProductIdAndName(otherProduct.getId(), "blog")).isPresent();
    }

    @Test
    void shouldDetectExistingTagByName() {
        Product product = saveProduct("tag-exists");
        assetTagRepository.saveAndFlush(new AssetTag(product.getId(), "blog"));

        assertThat(assetTagRepository.existsByProductIdAndName(product.getId(), "blog")).isTrue();
        assertThat(assetTagRepository.existsByProductIdAndName(product.getId(), "outra")).isFalse();
    }

    @Test
    void shouldFindTagsByIds() {
        Product product = saveProduct("tag-find-ids");
        AssetTag blog = assetTagRepository.saveAndFlush(new AssetTag(product.getId(), "blog"));
        AssetTag institucional = assetTagRepository.saveAndFlush(new AssetTag(product.getId(), "institucional"));
        assetTagRepository.saveAndFlush(new AssetTag(product.getId(), "nao-buscada"));

        assertThat(assetTagRepository.findAllByIdIn(List.of(blog.getId(), institucional.getId())))
                .containsExactlyInAnyOrder(blog, institucional);
    }

    @Test
    void shouldRejectDuplicateTagNameInSameProduct() {
        Product product = saveProduct("tag-duplicate");
        assetTagRepository.saveAndFlush(new AssetTag(product.getId(), "blog"));
        AssetTag duplicateTag = new AssetTag(product.getId(), "blog");

        assertThatThrownBy(() -> assetTagRepository.saveAndFlush(duplicateTag))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Product saveProduct(String suffix) {
        Tenant tenant = tenantRepository.saveAndFlush(tenant(suffix));
        return productRepository.saveAndFlush(product(tenant, suffix));
    }
}
