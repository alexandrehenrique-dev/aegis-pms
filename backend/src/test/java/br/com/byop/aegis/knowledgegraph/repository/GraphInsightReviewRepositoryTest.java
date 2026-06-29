package br.com.byop.aegis.knowledgegraph.repository;

import br.com.byop.aegis.core.RepositoryTestSupport;
import br.com.byop.aegis.knowledgegraph.domain.GraphInsightReview;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GraphInsightReviewRepositoryTest extends RepositoryTestSupport {

    private static final OffsetDateTime FIXED_TIME = OffsetDateTime.parse("2026-01-01T00:00:00Z");
    private static final String TEXT_HASH = "hash-1";
    private static final String REVIEW_TEXT = "Conectar conteudos relacionados";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private GraphInsightReviewRepository repository;

    @Test
    void shouldFindReviewByProductAndTextHash() {
        ProductIds product = persistedProduct("insight-review");
        GraphInsightReview review = repository.saveAndFlush(new GraphInsightReview(
                product.tenantId(),
                product.productId(),
                TEXT_HASH,
                REVIEW_TEXT
        ));

        assertThat(repository.findByProductIdAndTextHash(product.productId(), TEXT_HASH)).contains(review);
        assertThat(repository.findByProductIdAndTextHash(product.productId(), "missing")).isEmpty();
        assertThat(review.getTenantId()).isEqualTo(product.tenantId());
        assertThat(review.getProductId()).isEqualTo(product.productId());
        assertThat(review.getTextHash()).isEqualTo(TEXT_HASH);
        assertThat(review.getText()).isEqualTo(REVIEW_TEXT);
        assertThat(review.isReviewed()).isTrue();
        assertThat(review.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldRejectDuplicateReviewForSameProductAndHash() {
        ProductIds product = persistedProduct("insight-review-duplicate");
        repository.saveAndFlush(new GraphInsightReview(product.tenantId(), product.productId(), TEXT_HASH, "Texto"));

        GraphInsightReview duplicate = new GraphInsightReview(product.tenantId(), product.productId(), TEXT_HASH, "Texto");

        assertThatThrownBy(() -> repository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private ProductIds persistedProduct(String suffix) {
        UUID tenantId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into tenants (id, key, name, status, created_at, updated_at) values (?, ?, ?, ?, ?, ?)",
                tenantId,
                "tenant-" + suffix,
                "Tenant " + suffix,
                "ACTIVE",
                FIXED_TIME,
                FIXED_TIME
        );
        jdbcTemplate.update(
                """
                insert into products
                (id, tenant_id, key, name, type, status, default_locale, asset_storage_strategy, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                productId,
                tenantId,
                "product-" + suffix,
                "Product " + suffix,
                "SITE_INSTITUCIONAL",
                "ACTIVE",
                "pt-BR",
                "LOCAL",
                FIXED_TIME,
                FIXED_TIME
        );
        return new ProductIds(tenantId, productId);
    }

    private record ProductIds(UUID tenantId, UUID productId) {
    }
}
