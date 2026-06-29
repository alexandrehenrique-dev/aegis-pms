package br.com.byop.aegis.knowledgegraph.repository;

import br.com.byop.aegis.knowledgegraph.domain.GraphNode;
import br.com.byop.aegis.knowledgegraph.domain.GraphNodeType;

import br.com.byop.aegis.core.RepositoryTestSupport;
import br.com.byop.aegis.knowledgegraph.domain.GraphEdge;
import br.com.byop.aegis.knowledgegraph.domain.GraphEdgeType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GraphNodeRepositoryTest extends RepositoryTestSupport {

    private static final OffsetDateTime FIXED_TIME = OffsetDateTime.parse("2026-01-01T00:00:00Z");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private GraphNodeRepository graphNodeRepository;

    @Autowired
    private GraphEdgeRepository graphEdgeRepository;

    @Test
    void shouldSaveGraphNode() {
        ProductIds product = persistedProduct("node-save");

        GraphNode saved = graphNodeRepository.saveAndFlush(node(product, GraphNodeType.ARTICLE, "ARTICLE", "article-1"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTenantId()).isEqualTo(product.tenantId());
        assertThat(saved.getProductId()).isEqualTo(product.productId());
        assertThat(saved.getNodeType()).isEqualTo(GraphNodeType.ARTICLE);
        assertThat(saved.getRefType()).isEqualTo("ARTICLE");
        assertThat(saved.getRefId()).isEqualTo("article-1");
        assertThat(saved.getLabel()).isEqualTo("Label article-1");
        assertThat(saved.getSlug()).isEqualTo("slug-article-1");
        assertThat(saved.getMetadataJson()).isEqualTo("{}");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldRejectDuplicateReferenceWithinProduct() {
        ProductIds product = persistedProduct("node-duplicate");
        graphNodeRepository.saveAndFlush(node(product, GraphNodeType.ARTICLE, "ARTICLE", "same-ref"));

        GraphNode duplicate = node(product, GraphNodeType.CONTENT, "ARTICLE", "same-ref");

        assertThatThrownBy(() -> graphNodeRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldFindGraphNodesByTenantProductTypeAndReference() {
        ProductIds product = persistedProduct("node-query");
        GraphNode article = graphNodeRepository.saveAndFlush(
                node(product, GraphNodeType.ARTICLE, "ARTICLE", "article-query")
        );

        assertThat(graphNodeRepository.findAllByTenantId(product.tenantId())).containsExactly(article);
        assertThat(graphNodeRepository.findAllByProductId(product.productId())).containsExactly(article);
        assertThat(graphNodeRepository.findAllByNodeType(GraphNodeType.ARTICLE)).contains(article);
        assertThat(graphNodeRepository.findAllByRefTypeAndRefId("ARTICLE", "article-query")).containsExactly(article);
        assertThat(graphNodeRepository.findByProductIdAndRefTypeAndRefId(
                product.productId(),
                "ARTICLE",
                "article-query"
        )).contains(article);
        assertThat(graphNodeRepository.existsByProductIdAndRefTypeAndRefId(
                product.productId(),
                "ARTICLE",
                "article-query"
        )).isTrue();
    }

    @Test
    void shouldFindOnlyNodesWithoutIncomingOrOutgoingEdges() {
        ProductIds product = persistedProduct("node-orphans");
        GraphNode source = graphNodeRepository.saveAndFlush(node(product, GraphNodeType.ARTICLE, "ARTICLE", "source"));
        GraphNode target = graphNodeRepository.saveAndFlush(node(product, GraphNodeType.TOPIC, "TOPIC", "target"));
        GraphNode orphan = graphNodeRepository.saveAndFlush(node(product, GraphNodeType.TAG, "TAG", "orphan"));
        graphEdgeRepository.saveAndFlush(new GraphEdge(
                product.tenantId(),
                product.productId(),
                source.getId(),
                target.getId(),
                GraphEdgeType.RELATED_TO
        ));

        assertThat(graphNodeRepository.findOrphansByProductId(product.productId())).containsExactly(orphan);
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

    private GraphNode node(ProductIds product, GraphNodeType nodeType, String refType, String refId) {
        return new GraphNode(new GraphNode.Creation(
                product.tenantId(),
                product.productId(),
                nodeType,
                refType,
                refId,
                "Label " + refId,
                "slug-" + refId,
                "{}"
        ));
    }

    private record ProductIds(UUID tenantId, UUID productId) {
    }
}
