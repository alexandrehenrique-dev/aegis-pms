package br.com.byop.aegis.content.repository;

import br.com.byop.aegis.content.domain.Content;
import br.com.byop.aegis.content.domain.ContentVersion;
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

class ContentVersionRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private ContentVersionRepository versionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldSaveContentVersion() {
        Content content = saveContent("version-save");

        ContentVersion saved = versionRepository.saveAndFlush(
                new ContentVersion(content.getId(), "v1", "{\"title\":\"Artigo\"}", "subject-1")
        );

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getContentId()).isEqualTo(content.getId());
        assertThat(saved.getVersionLabel()).isEqualTo("v1");
        assertThat(saved.getSnapshotJson()).isEqualTo("{\"title\":\"Artigo\"}");
        assertThat(saved.getCreatedBySubject()).isEqualTo("subject-1");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldListVersionsInChronologicalOrder() {
        Content content = saveContent("version-order");

        ContentVersion first = versionRepository.saveAndFlush(
                new ContentVersion(content.getId(), "v1", "{}", "subject-1")
        );
        ContentVersion second = versionRepository.saveAndFlush(
                new ContentVersion(content.getId(), "v2", "{}", "subject-1")
        );

        assertThat(versionRepository.findAllByContentIdOrderByCreatedAtAsc(content.getId()))
                .containsExactly(first, second);
    }

    @Test
    void shouldCascadeDeleteVersionsWhenContentIsDeleted() {
        Content content = saveContent("version-cascade");
        ContentVersion version = versionRepository.saveAndFlush(
                new ContentVersion(content.getId(), "v1", "{}", "subject-1")
        );

        jdbcTemplate.update("DELETE FROM contents WHERE id = ?", content.getId());

        assertThat(countRows("content_versions", version.getId())).isZero();
    }

    private Content saveContent(String suffix) {
        Tenant tenant = tenantRepository.saveAndFlush(tenant(suffix));
        Product product = productRepository.saveAndFlush(product(tenant, suffix));
        return contentRepository.saveAndFlush(
                new Content(product.getTenantId(), product.getId(), "Artigo", "article", "pt-BR", "subject-1")
        );
    }

    private Long countRows(String tableName, UUID id) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName + " WHERE id = ?",
                Long.class,
                id
        );
    }
}
