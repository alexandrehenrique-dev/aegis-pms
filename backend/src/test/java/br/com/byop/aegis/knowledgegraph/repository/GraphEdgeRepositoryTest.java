package br.com.byop.aegis.knowledgegraph.repository;

import br.com.byop.aegis.knowledgegraph.domain.GraphEdge;
import br.com.byop.aegis.knowledgegraph.domain.GraphEdgeType;
import br.com.byop.aegis.knowledgegraph.domain.GraphNode;
import br.com.byop.aegis.knowledgegraph.domain.GraphNodeType;

import br.com.byop.aegis.core.RepositoryTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GraphEdgeRepositoryTest extends RepositoryTestSupport {

    private static final OffsetDateTime FIXED_TIME = OffsetDateTime.parse("2026-01-01T00:00:00Z");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private GraphNodeRepository graphNodeRepository;

    @Autowired
    private GraphEdgeRepository graphEdgeRepository;

    @Test
    void shouldSaveGraphEdge() {
        ProductIds product = persistedProduct("edge-save");
        GraphNode source = graphNodeRepository.saveAndFlush(node(product, GraphNodeType.ARTICLE, "source-save"));
        GraphNode target = graphNodeRepository.saveAndFlush(node(product, GraphNodeType.TOPIC, "target-save"));

        GraphEdge saved = graphEdgeRepository.saveAndFlush(
                new GraphEdge(
                        product.tenantId(),
                        product.productId(),
                        source.getId(),
                        target.getId(),
                        GraphEdgeType.TAGGED_WITH,
                        BigDecimal.ONE,
                        "{\"origin\":\"test\"}"
                )
        );

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTenantId()).isEqualTo(product.tenantId());
        assertThat(saved.getProductId()).isEqualTo(product.productId());
        assertThat(saved.getSourceNodeId()).isEqualTo(source.getId());
        assertThat(saved.getTargetNodeId()).isEqualTo(target.getId());
        assertThat(saved.getEdgeType()).isEqualTo(GraphEdgeType.TAGGED_WITH);
        assertThat(saved.getWeight()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(saved.getMetadataJson()).isEqualTo("{\"origin\":\"test\"}");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldRejectDuplicateEdgeBetweenSameNodesAndType() {
        ProductIds product = persistedProduct("edge-duplicate");
        GraphNode source = graphNodeRepository.saveAndFlush(node(product, GraphNodeType.ARTICLE, "source-duplicate"));
        GraphNode target = graphNodeRepository.saveAndFlush(node(product, GraphNodeType.TOPIC, "target-duplicate"));
        graphEdgeRepository.saveAndFlush(edge(product, source, target, GraphEdgeType.RELATED_TO));

        GraphEdge duplicate = edge(product, source, target, GraphEdgeType.RELATED_TO);

        assertThatThrownBy(() -> graphEdgeRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldFindGraphEdgesByTenantProductSourceTargetAndType() {
        ProductIds product = persistedProduct("edge-query");
        GraphNode source = graphNodeRepository.saveAndFlush(node(product, GraphNodeType.ARTICLE, "source-query"));
        GraphNode target = graphNodeRepository.saveAndFlush(node(product, GraphNodeType.TOPIC, "target-query"));
        GraphEdge edge = graphEdgeRepository.saveAndFlush(edge(product, source, target, GraphEdgeType.RELATED_TO));

        assertThat(graphEdgeRepository.findAllByTenantId(product.tenantId())).containsExactly(edge);
        assertThat(graphEdgeRepository.findAllByProductId(product.productId())).containsExactly(edge);
        assertThat(graphEdgeRepository.findAllBySourceNodeId(source.getId())).containsExactly(edge);
        assertThat(graphEdgeRepository.findAllByTargetNodeId(target.getId())).containsExactly(edge);
        assertThat(graphEdgeRepository.findAllByEdgeType(GraphEdgeType.RELATED_TO)).contains(edge);
        assertThat(graphEdgeRepository.existsBySourceNodeIdAndTargetNodeIdAndEdgeType(
                source.getId(),
                target.getId(),
                GraphEdgeType.RELATED_TO
        )).isTrue();
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

    private GraphNode node(ProductIds product, GraphNodeType nodeType, String suffix) {
        return new GraphNode(new GraphNode.Creation(
                product.tenantId(),
                product.productId(),
                nodeType,
                nodeType.name(),
                suffix,
                "Label " + suffix,
                "slug-" + suffix,
                "{}"
        ));
    }

    private GraphEdge edge(ProductIds product, GraphNode source, GraphNode target, GraphEdgeType edgeType) {
        return new GraphEdge(
                product.tenantId(),
                product.productId(),
                source.getId(),
                target.getId(),
                edgeType
        );
    }

    private record ProductIds(UUID tenantId, UUID productId) {
    }
}
