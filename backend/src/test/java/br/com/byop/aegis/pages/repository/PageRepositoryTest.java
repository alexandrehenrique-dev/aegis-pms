package br.com.byop.aegis.pages.repository;

import br.com.byop.aegis.core.RepositoryTestSupport;
import br.com.byop.aegis.pages.domain.Page;
import br.com.byop.aegis.pages.domain.PageStatus;
import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.repository.ProductRepository;
import br.com.byop.aegis.tenant.domain.Tenant;
import br.com.byop.aegis.tenant.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldSavePage() {
        Product product = saveProduct("page-save");

        Page saved = pageRepository.saveAndFlush(new Page(product.getTenantId(), product.getId(), "home", "Home", "pt-BR"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTenantId()).isEqualTo(product.getTenantId());
        assertThat(saved.getProductId()).isEqualTo(product.getId());
        assertThat(saved.getSlug()).isEqualTo("home");
        assertThat(saved.getTitle()).isEqualTo("Home");
        assertThat(saved.getLocale()).isEqualTo("pt-BR");
        assertThat(saved.getStatus()).isEqualTo(PageStatus.DRAFT);
        assertThat(saved.getVersion()).isEqualTo(1);
        assertThat(saved.isSeoNoIndex()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldApplyEditAndBumpVersion() {
        Product product = saveProduct("page-edit");
        Page page = pageRepository.saveAndFlush(new Page(product.getTenantId(), product.getId(), "sobre", "Sobre", "pt-BR"));
        UUID ogImageAssetId = UUID.randomUUID();

        page.applyEdit(new Page.Edit("sobre-nos", "Sobre nos", "en-US", PageStatus.PUBLISHED, "Sobre | Aegis",
                "Descricao SEO", "https://example.com/sobre", ogImageAssetId, true));
        pageRepository.saveAndFlush(page);

        Page reloaded = pageRepository.findById(page.getId()).orElseThrow();
        assertThat(reloaded.getSlug()).isEqualTo("sobre-nos");
        assertThat(reloaded.getTitle()).isEqualTo("Sobre nos");
        assertThat(reloaded.getLocale()).isEqualTo("en-US");
        assertThat(reloaded.getStatus()).isEqualTo(PageStatus.PUBLISHED);
        assertThat(reloaded.getVersion()).isEqualTo(2);
        assertThat(reloaded.getSeoTitle()).isEqualTo("Sobre | Aegis");
        assertThat(reloaded.getSeoDescription()).isEqualTo("Descricao SEO");
        assertThat(reloaded.getSeoCanonical()).isEqualTo("https://example.com/sobre");
        assertThat(reloaded.getSeoOgImageAssetId()).isEqualTo(ogImageAssetId);
        assertThat(reloaded.isSeoNoIndex()).isTrue();
    }

    @Test
    void shouldRejectDuplicateSlugWithinSameProduct() {
        Product product = saveProduct("page-duplicate-slug");
        pageRepository.saveAndFlush(new Page(product.getTenantId(), product.getId(), "home", "Home", "pt-BR"));
        Page duplicate = new Page(product.getTenantId(), product.getId(), "home", "Home 2", "pt-BR");

        assertThatThrownBy(() -> pageRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldAllowSameSlugAcrossDifferentProducts() {
        Product product = saveProduct("page-slug-a");
        Product otherProduct = saveProduct("page-slug-b");
        pageRepository.saveAndFlush(new Page(product.getTenantId(), product.getId(), "home", "Home", "pt-BR"));

        Page otherPage = pageRepository.saveAndFlush(new Page(otherProduct.getTenantId(), otherProduct.getId(), "home", "Home", "pt-BR"));

        assertThat(otherPage.getId()).isNotNull();
    }

    @Test
    void shouldFindPageScopedByProduct() {
        Product product = saveProduct("page-find");
        Product otherProduct = saveProduct("page-find-other");
        Page page = pageRepository.saveAndFlush(new Page(product.getTenantId(), product.getId(), "home", "Home", "pt-BR"));
        pageRepository.saveAndFlush(new Page(otherProduct.getTenantId(), otherProduct.getId(), "outra", "Outra", "pt-BR"));

        assertThat(pageRepository.findAllByProductId(product.getId())).containsExactly(page);
        assertThat(pageRepository.findByProductIdAndId(product.getId(), page.getId())).contains(page);
        assertThat(pageRepository.findByProductIdAndId(otherProduct.getId(), page.getId())).isEmpty();
        assertThat(pageRepository.findByProductIdAndSlug(product.getId(), "home")).contains(page);
        assertThat(pageRepository.findByProductIdAndSlug(otherProduct.getId(), "home")).isEmpty();
    }

    @Test
    void shouldSupportPageStatusContractValues() {
        assertThat(PageStatus.fromContractValue("published")).isEqualTo(PageStatus.PUBLISHED);
        assertThat(PageStatus.PUBLISHED.contractValue()).isEqualTo("published");
        assertThatThrownBy(() -> PageStatus.fromContractValue("unknown"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldCascadeDeletePageWhenProductIsDeleted() {
        Product product = saveProduct("page-cascade");
        Page page = pageRepository.saveAndFlush(new Page(product.getTenantId(), product.getId(), "home", "Home", "pt-BR"));

        jdbcTemplate.update("DELETE FROM products WHERE id = ?", product.getId());

        assertThat(countRows("pages", page.getId())).isZero();
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
