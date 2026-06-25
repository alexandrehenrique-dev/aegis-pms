package br.com.byop.aegis.knowledgegraph.mapper;

import br.com.byop.aegis.knowledgegraph.contract.CreateGraphNodeRequest;
import br.com.byop.aegis.knowledgegraph.domain.GraphNode;
import br.com.byop.aegis.knowledgegraph.domain.GraphNodeType;
import br.com.byop.aegis.knowledgegraph.dto.GraphNodeDetail;
import br.com.byop.aegis.knowledgegraph.dto.GraphNodeSummary;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GraphNodeMapperTest {

    private final GraphNodeMapper mapper = Mappers.getMapper(GraphNodeMapper.class);

    @Test
    void shouldMapRequestToEntity() {
        UUID tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID productId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        CreateGraphNodeRequest request = new CreateGraphNodeRequest(
                GraphNodeType.ARTICLE,
                "ARTICLE",
                "article-1",
                "Article 1",
                "article-1",
                "{\"source\":\"manual\"}"
        );

        GraphNode entity = mapper.toEntity(request, tenantId, productId);

        assertThat(entity).isNotNull();
        assertThat(entity.getTenantId()).isEqualTo(tenantId);
        assertThat(entity.getProductId()).isEqualTo(productId);
        assertThat(entity.getNodeType()).isEqualTo(GraphNodeType.ARTICLE);
        assertThat(entity.getRefType()).isEqualTo("ARTICLE");
        assertThat(entity.getRefId()).isEqualTo("article-1");
        assertThat(entity.getLabel()).isEqualTo("Article 1");
        assertThat(entity.getSlug()).isEqualTo("article-1");
        assertThat(entity.getMetadataJson()).isEqualTo("{\"source\":\"manual\"}");
    }

    @Test
    void shouldApplyRequestDefaultsWhenOptionalFieldsAreBlankOrNull() {
        CreateGraphNodeRequest request = new CreateGraphNodeRequest(
                GraphNodeType.TOPIC,
                "TOPIC",
                "spring-boot",
                "Spring Boot",
                " ",
                null
        );

        GraphNode entity = mapper.toEntity(request, UUID.randomUUID(), UUID.randomUUID());

        assertThat(entity.getSlug()).isEqualTo("spring-boot");
        assertThat(entity.getMetadataJson()).isEqualTo("{}");
    }

    @Test
    void shouldApplyRefIdAsSlugWhenRequestSlugIsNull() {
        CreateGraphNodeRequest request = new CreateGraphNodeRequest(
                GraphNodeType.TAG,
                "TAG",
                "java",
                "Java",
                null,
                " "
        );

        GraphNode entity = mapper.toEntity(request, UUID.randomUUID(), UUID.randomUUID());

        assertThat(entity.getSlug()).isEqualTo("java");
        assertThat(entity.getMetadataJson()).isEqualTo("{}");
    }

    @Test
    void shouldReturnNullEntityWhenNodeRequestIsNull() {
        assertThat(mapper.toEntity(null, UUID.randomUUID(), UUID.randomUUID())).isNull();
    }

    @Test
    void shouldMapEntityToSummary() {
        UUID nodeId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        UUID tenantId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        UUID productId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-06-25T12:00:00-03:00");
        OffsetDateTime updatedAt = OffsetDateTime.parse("2026-06-25T12:10:00-03:00");
        GraphNode entity = node(nodeId, tenantId, productId, createdAt, updatedAt);

        GraphNodeSummary summary = mapper.toSummary(entity);

        assertThat(summary).isNotNull();
        assertThat(summary.id()).isEqualTo(nodeId);
        assertThat(summary.tenantId()).isEqualTo(tenantId);
        assertThat(summary.productId()).isEqualTo(productId);
        assertThat(summary.nodeType()).isEqualTo(GraphNodeType.CONTENT);
        assertThat(summary.refType()).isEqualTo("CONTENT");
        assertThat(summary.refId()).isEqualTo("content-1");
        assertThat(summary.label()).isEqualTo("Content 1");
        assertThat(summary.slug()).isEqualTo("content-1");
        assertThat(summary.createdAt()).isEqualTo(createdAt);
        assertThat(summary.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void shouldMapEntityToDetail() {
        UUID nodeId = UUID.fromString("66666666-6666-6666-6666-666666666666");
        UUID tenantId = UUID.fromString("77777777-7777-7777-7777-777777777777");
        UUID productId = UUID.fromString("88888888-8888-8888-8888-888888888888");
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-06-25T13:00:00-03:00");
        OffsetDateTime updatedAt = OffsetDateTime.parse("2026-06-25T13:10:00-03:00");
        GraphNode entity = node(nodeId, tenantId, productId, createdAt, updatedAt);

        GraphNodeDetail detail = mapper.toDetail(entity);

        assertThat(detail).isNotNull();
        assertThat(detail.id()).isEqualTo(nodeId);
        assertThat(detail.tenantId()).isEqualTo(tenantId);
        assertThat(detail.productId()).isEqualTo(productId);
        assertThat(detail.nodeType()).isEqualTo(GraphNodeType.CONTENT);
        assertThat(detail.metadataJson()).isEqualTo("{\"kind\":\"article\"}");
        assertThat(detail.createdAt()).isEqualTo(createdAt);
        assertThat(detail.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void shouldReturnNullWhenNodeIsNull() {
        assertThat(mapper.toSummary(null)).isNull();
        assertThat(mapper.toDetail(null)).isNull();
    }

    private GraphNode node(UUID nodeId, UUID tenantId, UUID productId, OffsetDateTime createdAt,
                           OffsetDateTime updatedAt) {
        GraphNode node = new GraphNode(new GraphNode.Creation(
                tenantId,
                productId,
                GraphNodeType.CONTENT,
                "CONTENT",
                "content-1",
                "Content 1",
                "content-1",
                "{\"kind\":\"article\"}"
        ));
        ReflectionTestUtils.setField(node, "id", nodeId);
        ReflectionTestUtils.setField(node, "createdAt", createdAt);
        ReflectionTestUtils.setField(node, "updatedAt", updatedAt);
        return node;
    }
}
