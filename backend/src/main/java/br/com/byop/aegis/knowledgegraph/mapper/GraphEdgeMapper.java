package br.com.byop.aegis.knowledgegraph.mapper;

import br.com.byop.aegis.knowledgegraph.contract.CreateGraphEdgeRequest;
import br.com.byop.aegis.knowledgegraph.domain.GraphEdge;
import br.com.byop.aegis.knowledgegraph.dto.GraphEdgeDetail;
import br.com.byop.aegis.knowledgegraph.dto.GraphEdgeSummary;
import org.mapstruct.Mapper;

import java.math.BigDecimal;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface GraphEdgeMapper {

    default GraphEdge toEntity(CreateGraphEdgeRequest request, UUID tenantId, UUID productId) {
        if (request == null) {
            return null;
        }
        return new GraphEdge(
                tenantId,
                productId,
                request.sourceNodeId(),
                request.targetNodeId(),
                request.edgeType(),
                normalizedWeight(request.weight()),
                normalizedMetadataJson(request.metadataJson())
        );
    }

    GraphEdgeSummary toSummary(GraphEdge graphEdge);

    GraphEdgeDetail toDetail(GraphEdge graphEdge);

    private BigDecimal normalizedWeight(BigDecimal weight) {
        return weight == null ? BigDecimal.ONE : weight;
    }

    private String normalizedMetadataJson(String metadataJson) {
        return metadataJson == null || metadataJson.isBlank() ? "{}" : metadataJson;
    }
}
