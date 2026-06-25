package br.com.byop.aegis.knowledgegraph.mapper;

import br.com.byop.aegis.knowledgegraph.contract.CreateGraphNodeRequest;
import br.com.byop.aegis.knowledgegraph.domain.GraphNode;
import br.com.byop.aegis.knowledgegraph.dto.GraphNodeDetail;
import br.com.byop.aegis.knowledgegraph.dto.GraphNodeSummary;
import org.mapstruct.Mapper;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface GraphNodeMapper {

    default GraphNode toEntity(CreateGraphNodeRequest request, UUID tenantId, UUID productId) {
        if (request == null) {
            return null;
        }
        return new GraphNode(new GraphNode.Creation(
                tenantId,
                productId,
                request.nodeType(),
                request.refType(),
                request.refId(),
                request.label(),
                normalizedSlug(request.slug(), request.refId()),
                normalizedMetadataJson(request.metadataJson())
        ));
    }

    GraphNodeSummary toSummary(GraphNode graphNode);

    GraphNodeDetail toDetail(GraphNode graphNode);

    private String normalizedSlug(String slug, String refId) {
        return slug == null || slug.isBlank() ? refId : slug;
    }

    private String normalizedMetadataJson(String metadataJson) {
        return metadataJson == null || metadataJson.isBlank() ? "{}" : metadataJson;
    }
}
