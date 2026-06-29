package br.com.byop.aegis.knowledgegraph.api;

import java.util.Optional;
import java.util.UUID;

public interface GraphNodeContentPreviewPort {

    Optional<GraphNodeContentPreview> findPreview(UUID productId, UUID graphNodeId, String refId);
}
