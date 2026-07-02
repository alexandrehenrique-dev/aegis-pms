package br.com.byop.aegis.pages.repository;

import br.com.byop.aegis.core.RepositoryTestSupport;
import br.com.byop.aegis.pages.domain.ProductGlobals;
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

class ProductGlobalsRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductGlobalsRepository globalsRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldSaveProductGlobals() {
        Product product = saveProduct("globals-save");

        ProductGlobals saved = globalsRepository.saveAndFlush(new ProductGlobals(
                product.getId(), "{\"links\":[]}", "{\"links\":[]}", "[]", null
        ));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getProductId()).isEqualTo(product.getId());
        assertThat(saved.getNavbarJson()).contains("links");
        assertThat(saved.getFloatingWhatsappJson()).isNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldApplyUpsertUpdate() {
        Product product = saveProduct("globals-apply");
        ProductGlobals globals = globalsRepository.saveAndFlush(new ProductGlobals(
                product.getId(), "{\"links\":[]}", "{\"links\":[]}", "[]", null
        ));

        globals.apply("{\"links\":[{\"label\":\"Home\",\"href\":\"/\"}]}", "{\"links\":[]}",
                "[{\"platform\":\"instagram\",\"href\":\"https://instagram.com/x\"}]",
                "{\"enabled\":true,\"number\":\"5511999999999\",\"message\":\"Oi\"}");
        globalsRepository.saveAndFlush(globals);

        ProductGlobals reloaded = globalsRepository.findById(globals.getId()).orElseThrow();
        assertThat(reloaded.getNavbarJson()).contains("Home");
        assertThat(reloaded.getSocialLinksJson()).contains("instagram");
        assertThat(reloaded.getFloatingWhatsappJson()).contains("5511999999999");
    }

    @Test
    void shouldRejectDuplicateProductGlobals() {
        Product product = saveProduct("globals-duplicate");
        globalsRepository.saveAndFlush(new ProductGlobals(product.getId(), "{}", "{}", "[]", null));
        ProductGlobals duplicate = new ProductGlobals(product.getId(), "{}", "{}", "[]", null);

        assertThatThrownBy(() -> globalsRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldReturnEmptyWhenProductHasNoGlobalsYet() {
        Product product = saveProduct("globals-missing");

        assertThat(globalsRepository.findByProductId(product.getId())).isEmpty();
    }

    @Test
    void shouldCascadeDeleteGlobalsWhenProductIsDeleted() {
        Product product = saveProduct("globals-cascade");
        ProductGlobals globals = globalsRepository.saveAndFlush(new ProductGlobals(product.getId(), "{}", "{}", "[]", null));

        jdbcTemplate.update("DELETE FROM products WHERE id = ?", product.getId());

        assertThat(countRows("product_globals", globals.getId())).isZero();
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
