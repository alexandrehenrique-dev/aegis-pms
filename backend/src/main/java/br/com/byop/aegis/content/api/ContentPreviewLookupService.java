package br.com.byop.aegis.content.api;

import br.com.byop.aegis.content.domain.Content;
import br.com.byop.aegis.content.repository.ContentRepository;
import br.com.byop.aegis.knowledgegraph.api.GraphNodeContentPreview;
import br.com.byop.aegis.knowledgegraph.api.GraphNodeContentPreviewPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class ContentPreviewLookupService implements GraphNodeContentPreviewPort {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> METADATA_TYPE = new TypeReference<>() {
    };
    private static final String THUMBNAIL_KEY = "thumbnail";

    private final ContentRepository contentRepository;

    public ContentPreviewLookupService(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<GraphNodeContentPreview> findPreview(UUID productId, UUID graphNodeId, String refId) {
        log.debug("findPreview: productId='{}', graphNodeId='{}'", productId, graphNodeId);
        Optional<Content> content = contentRepository.findByProductIdAndGraphNodeId(productId, graphNodeId)
                .or(() -> findByReferenceId(productId, refId));
        return content.map(this::toPreview);
    }

    private Optional<Content> findByReferenceId(UUID productId, String refId) {
        try {
            return contentRepository.findByProductIdAndId(productId, UUID.fromString(refId));
        } catch (IllegalArgumentException exception) {
            log.debug("findByReferenceId: refId='{}' nao e um UUID de content valido — ignorado na preview", refId, exception);
            return Optional.empty();
        }
    }

    private GraphNodeContentPreview toPreview(Content content) {
        String difficulty = content.getDifficultyLevel() == null ? null : content.getDifficultyLevel().contractValue();
        return new GraphNodeContentPreview(
                content.getSummary(),
                difficulty,
                thumbnailFrom(content.getMetadataJson())
        );
    }

    private String thumbnailFrom(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return null;
        }
        try {
            Object thumbnail = JSON_MAPPER.readValue(metadataJson, METADATA_TYPE).get(THUMBNAIL_KEY);
            return thumbnail == null || String.valueOf(thumbnail).isBlank() ? null : String.valueOf(thumbnail);
        } catch (JsonProcessingException exception) {
            log.debug("thumbnailFrom: metadata JSON invalido — thumbnail ignorado", exception);
            return null;
        }
    }
}
