package br.com.byop.aegis.knowledgegraph.mapper;

import br.com.byop.aegis.knowledgegraph.contract.CreateGraphEdgeRequest;
import br.com.byop.aegis.knowledgegraph.domain.GraphEdge;
import br.com.byop.aegis.knowledgegraph.domain.GraphEdgeType;
import br.com.byop.aegis.knowledgegraph.dto.GraphEdgeDetail;
import br.com.byop.aegis.knowledgegraph.dto.GraphEdgeSummary;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GraphEdgeMapperTest {

    private final GraphEdgeMapper mapper = Mappers.getMapper(GraphEdgeMapper.class);

    @Test
    void shouldMapRequestToEntity() {
        UUID tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID productId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID sourceNodeId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        UUID targetNodeId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        CreateGraphEdgeRequest request = new CreateGraphEdgeRequest(
                sourceNodeId,
                targetNodeId,
                GraphEdgeType.RELATED_TO,
                new BigDecimal("2.5000"),
                "{\"origin\":\"inline\"}"
        );

        GraphEdge entity = mapper.toEntity(request, tenantId, productId);

        assertThat(entity).isNotNull();
        assertThat(entity.getTenantId()).isEqualTo(tenantId);
        assertThat(entity.getProductId()).isEqualTo(productId);
        assertThat(entity.getSourceNodeId()).isEqualTo(sourceNodeId);
        assertThat(entity.getTargetNodeId()).isEqualTo(targetNodeId);
        assertThat(entity.getEdgeType()).isEqualTo(GraphEdgeType.RELATED_TO);
        assertThat(entity.getWeight()).isEqualByComparingTo("2.5000");
        assertThat(entity.getMetadataJson()).isEqualTo("{\"origin\":\"inline\"}");
    }

    @Test
    void shouldApplyRequestDefaultsWhenOptionalFieldsAreBlankOrNull() {
        CreateGraphEdgeRequest request = new CreateGraphEdgeRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                GraphEdgeType.TAGGED_WITH,
                null,
                " "
        );

        GraphEdge entity = mapper.toEntity(request, UUID.randomUUID(), UUID.randomUUID());

        assertThat(entity.getWeight()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(entity.getMetadataJson()).isEqualTo("{}");
    }

    @Test
    void shouldApplyDefaultMetadataWhenRequestMetadataIsNull() {
        CreateGraphEdgeRequest request = new CreateGraphEdgeRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                GraphEdgeType.DEPENDS_ON,
                BigDecimal.TEN,
                null
        );

        GraphEdge entity = mapper.toEntity(request, UUID.randomUUID(), UUID.randomUUID());

        assertThat(entity.getWeight()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(entity.getMetadataJson()).isEqualTo("{}");
    }

    @Test
    void shouldReturnNullEntityWhenEdgeRequestIsNull() {
        assertThat(mapper.toEntity(null, UUID.randomUUID(), UUID.randomUUID())).isNull();
    }

    @Test
    void shouldMapEntityToSummary() {
        UUID edgeId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        UUID tenantId = UUID.fromString("66666666-6666-6666-6666-666666666666");
        UUID productId = UUID.fromString("77777777-7777-7777-7777-777777777777");
        UUID sourceNodeId = UUID.fromString("88888888-8888-8888-8888-888888888888");
        UUID targetNodeId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-06-25T14:00:00-03:00");
        OffsetDateTime updatedAt = OffsetDateTime.parse("2026-06-25T14:10:00-03:00");
        GraphEdge entity = edge(edgeId, tenantId, productId, sourceNodeId, targetNodeId, createdAt, updatedAt);

        GraphEdgeSummary summary = mapper.toSummary(entity);

        assertThat(summary).isNotNull();
        assertThat(summary.id()).isEqualTo(edgeId);
        assertThat(summary.tenantId()).isEqualTo(tenantId);
        assertThat(summary.productId()).isEqualTo(productId);
        assertThat(summary.sourceNodeId()).isEqualTo(sourceNodeId);
        assertThat(summary.targetNodeId()).isEqualTo(targetNodeId);
        assertThat(summary.edgeType()).isEqualTo(GraphEdgeType.INSPIRED_BY);
        assertThat(summary.weight()).isEqualByComparingTo("0.7500");
        assertThat(summary.createdAt()).isEqualTo(createdAt);
        assertThat(summary.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void shouldMapEntityToDetail() {
        UUID edgeId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID tenantId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        UUID productId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        UUID sourceNodeId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        UUID targetNodeId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-06-25T15:00:00-03:00");
        OffsetDateTime updatedAt = OffsetDateTime.parse("2026-06-25T15:10:00-03:00");
        GraphEdge entity = edge(edgeId, tenantId, productId, sourceNodeId, targetNodeId, createdAt, updatedAt);

        GraphEdgeDetail detail = mapper.toDetail(entity);

        assertThat(detail).isNotNull();
        assertThat(detail.id()).isEqualTo(edgeId);
        assertThat(detail.tenantId()).isEqualTo(tenantId);
        assertThat(detail.productId()).isEqualTo(productId);
        assertThat(detail.sourceNodeId()).isEqualTo(sourceNodeId);
        assertThat(detail.targetNodeId()).isEqualTo(targetNodeId);
        assertThat(detail.edgeType()).isEqualTo(GraphEdgeType.INSPIRED_BY);
        assertThat(detail.weight()).isEqualByComparingTo("0.7500");
        assertThat(detail.metadataJson()).isEqualTo("{\"reason\":\"poem\"}");
        assertThat(detail.createdAt()).isEqualTo(createdAt);
        assertThat(detail.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void shouldReturnNullWhenEdgeIsNull() {
        assertThat(mapper.toSummary(null)).isNull();
        assertThat(mapper.toDetail(null)).isNull();
    }

    private GraphEdge edge(UUID edgeId, UUID tenantId, UUID productId, UUID sourceNodeId, UUID targetNodeId,
                           OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        GraphEdge edge = new GraphEdge(
                tenantId,
                productId,
                sourceNodeId,
                targetNodeId,
                GraphEdgeType.INSPIRED_BY,
                new BigDecimal("0.7500"),
                "{\"reason\":\"poem\"}"
        );
        ReflectionTestUtils.setField(edge, "id", edgeId);
        ReflectionTestUtils.setField(edge, "createdAt", createdAt);
        ReflectionTestUtils.setField(edge, "updatedAt", updatedAt);
        return edge;
    }
}
