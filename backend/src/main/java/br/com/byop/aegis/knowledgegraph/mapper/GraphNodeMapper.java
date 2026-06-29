package br.com.byop.aegis.knowledgegraph.mapper;

import br.com.byop.aegis.knowledgegraph.contract.CreateGraphNodeRequest;
import br.com.byop.aegis.knowledgegraph.domain.GraphNode;
import br.com.byop.aegis.knowledgegraph.dto.GraphNodeDetail;
import br.com.byop.aegis.knowledgegraph.dto.GraphNodeProp;
import br.com.byop.aegis.knowledgegraph.dto.GraphNodeSummary;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mapper(componentModel = "spring")
public abstract class GraphNodeMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphNodeMapper.class);
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> METADATA_TYPE = new TypeReference<>() {
    };
    private static final String STATUS_KEY = "status";
    private static final String DEFAULT_STATUS = "ativo";

    public GraphNode toEntity(CreateGraphNodeRequest request, UUID tenantId, UUID productId) {
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
                normalizedMetadataJson(request.metadataJson()),
                0.0,
                0.0
        ));
    }

    public GraphNodeSummary toSummary(GraphNode graphNode) {
        if (graphNode == null) {
            return null;
        }
        return new GraphNodeSummary(
                graphNode.getId(),
                graphNode.getTenantId(),
                graphNode.getProductId(),
                graphNode.getNodeType(),
                graphNode.getRefType(),
                graphNode.getRefId(),
                graphNode.getLabel(),
                graphNode.getSlug(),
                graphNode.getNodeType().name(),
                statusFrom(graphNode.getMetadataJson()),
                graphNode.getX(),
                graphNode.getY(),
                propsFrom(graphNode.getMetadataJson()),
                graphNode.getCreatedAt(),
                graphNode.getUpdatedAt()
        );
    }

    public GraphNodeDetail toDetail(GraphNode graphNode) {
        if (graphNode == null) {
            return null;
        }
        return new GraphNodeDetail(
                graphNode.getId(),
                graphNode.getTenantId(),
                graphNode.getProductId(),
                graphNode.getNodeType(),
                graphNode.getRefType(),
                graphNode.getRefId(),
                graphNode.getLabel(),
                graphNode.getSlug(),
                graphNode.getNodeType().name(),
                statusFrom(graphNode.getMetadataJson()),
                graphNode.getX(),
                graphNode.getY(),
                propsFrom(graphNode.getMetadataJson()),
                graphNode.getMetadataJson(),
                graphNode.getCreatedAt(),
                graphNode.getUpdatedAt()
        );
    }

    public List<GraphNodeProp> propsFrom(String metadataJson) {
        return metadataFrom(metadataJson).entrySet()
                .stream()
                .filter(entry -> entry.getValue() != null)
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(entry -> new GraphNodeProp(entry.getKey(), String.valueOf(entry.getValue())))
                .toList();
    }

    public String statusFrom(String metadataJson) {
        Object status = metadataFrom(metadataJson).get(STATUS_KEY);
        return status == null || String.valueOf(status).isBlank() ? DEFAULT_STATUS : String.valueOf(status);
    }

    private String normalizedSlug(String slug, String refId) {
        return slug == null || slug.isBlank() ? refId : slug;
    }

    private String normalizedMetadataJson(String metadataJson) {
        return metadataJson == null || metadataJson.isBlank() ? "{}" : metadataJson;
    }

    private Map<String, Object> metadataFrom(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Map.of();
        }
        try {
            return JSON_MAPPER.readValue(metadataJson, METADATA_TYPE);
        } catch (JsonProcessingException exception) {
            LOGGER.debug("Ignoring invalid graph node metadata JSON while mapping props", exception);
            return Map.of();
        }
    }
}
