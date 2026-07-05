package br.com.byop.aegis.content.service;

import br.com.byop.aegis.audit.api.AuditRecordCommand;
import br.com.byop.aegis.audit.api.AuditService;
import br.com.byop.aegis.content.contract.ContentTransitionRequest;
import br.com.byop.aegis.content.contract.CreateContentRequest;
import br.com.byop.aegis.content.contract.PublishContentRequest;
import br.com.byop.aegis.content.contract.UpdateContentRequest;
import br.com.byop.aegis.content.domain.Content;
import br.com.byop.aegis.content.domain.ContentStatus;
import br.com.byop.aegis.content.domain.ContentVersion;
import br.com.byop.aegis.content.domain.DifficultyLevel;
import br.com.byop.aegis.content.dto.ContentSummary;
import br.com.byop.aegis.content.dto.ContentVersionSummary;
import br.com.byop.aegis.content.dto.WorkflowItemSummary;
import br.com.byop.aegis.content.exception.ContentNotFoundException;
import br.com.byop.aegis.content.exception.DuplicateContentTitleException;
import br.com.byop.aegis.content.exception.InvalidContentReferenceException;
import br.com.byop.aegis.content.exception.InvalidContentStatusException;
import br.com.byop.aegis.content.exception.InvalidContentTransitionException;
import br.com.byop.aegis.content.exception.InvalidDifficultyLevelException;
import br.com.byop.aegis.content.mapper.ContentMapper;
import br.com.byop.aegis.content.mapper.ContentVersionMapper;
import br.com.byop.aegis.content.repository.ContentRepository;
import br.com.byop.aegis.content.repository.ContentVersionRepository;
import br.com.byop.aegis.identity.api.IdentityUserDirectory;
import br.com.byop.aegis.knowledgegraph.api.KnowledgeGraphPort;
import br.com.byop.aegis.product.api.ProductReference;
import br.com.byop.aegis.product.api.ProductReferenceService;
import br.com.byop.aegis.security.AuthenticatedUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ContentService {

    private static final Pattern KG_REF_PATTERN = Pattern.compile("\\{\\{kg-ref:([\\w-]+):([^}]+)\\}\\}");
    private static final TypeReference<Map<String, Object>> METADATA_TYPE = new TypeReference<>() {
    };
    private static final String TARGET_TYPE_CONTENT = "Content";
    private static final String MODULE_CONTENT = "CONTENT";
    private static final String DIFF_KEY_STATUS = "status";

    private final ContentRepository contentRepository;
    private final ContentVersionRepository versionRepository;
    private final ContentMapper contentMapper;
    private final ContentVersionMapper versionMapper;
    private final ContentWorkflowPolicy workflowPolicy;
    private final MarkdownSanitizer markdownSanitizer;
    private final IdentityUserDirectory identityUserDirectory;
    private final KnowledgeGraphPort knowledgeGraphPort;
    private final ProductReferenceService productReferenceService;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    public ContentService(ContentRepository contentRepository, ContentVersionRepository versionRepository,
                          ContentMapper contentMapper, ContentVersionMapper versionMapper,
                          ContentWorkflowPolicy workflowPolicy, MarkdownSanitizer markdownSanitizer,
                          IdentityUserDirectory identityUserDirectory, KnowledgeGraphPort knowledgeGraphPort,
                          ProductReferenceService productReferenceService, ObjectMapper objectMapper,
                          AuditService auditService) {
        this.contentRepository = contentRepository;
        this.versionRepository = versionRepository;
        this.contentMapper = contentMapper;
        this.versionMapper = versionMapper;
        this.workflowPolicy = workflowPolicy;
        this.markdownSanitizer = markdownSanitizer;
        this.identityUserDirectory = identityUserDirectory;
        this.knowledgeGraphPort = knowledgeGraphPort;
        this.productReferenceService = productReferenceService;
        this.objectMapper = objectMapper;
        this.auditService = auditService;
    }

    @Transactional
    public ContentSummary createContent(UUID productId, CreateContentRequest request, AuthenticatedUser caller) {
        log.debug("createContent: productId='{}', title='{}', type='{}'", productId, request.title(), request.type());
        ProductReference product = productReferenceService.getRequiredReference(productId);
        if (contentRepository.existsByProductIdAndTitle(productId, request.title())) {
            log.warn("createContent: titulo ja existe productId='{}', title='{}'", productId, request.title());
            throw new DuplicateContentTitleException(request.title());
        }

        String sanitizedBody = markdownSanitizer.sanitize(request.body());
        List<UUID> referencedNodeIds = extractAndValidateKgRefs(productId, sanitizedBody);
        DifficultyLevel difficultyLevel = parseDifficultyLevel(request.difficultyLevel());
        String metadataJson = writeMetadataJson(request.metadata());

        Content content = new Content(product.tenantId(), productId, request.title(), request.type(),
                request.lang(), caller.subject());
        content.applyEdit(new Content.Edit(request.title(), request.type(), request.lang(), sanitizedBody,
                request.summary(), difficultyLevel, request.category(), request.topic(), metadataJson));
        contentRepository.save(content);
        recordContentCreationAudit(product.tenantId(), productId, caller.subject(), content);
        log.info("createContent: conteudo criado id='{}', productId='{}'", content.getId(), productId);

        syncKnowledgeGraph(productId, content, referencedNodeIds);

        return toSummary(content);
    }

    @Transactional(readOnly = true)
    public List<ContentSummary> listContent(UUID productId) {
        log.debug("listContent: productId='{}'", productId);
        return contentRepository.findAllByProductId(productId).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public ContentSummary getContent(UUID productId, UUID contentId) {
        log.debug("getContent: productId='{}', contentId='{}'", productId, contentId);
        return toSummary(findContentInProduct(productId, contentId));
    }

    @Transactional
    public ContentSummary updateContent(UUID productId, UUID contentId, UpdateContentRequest request) {
        log.debug("updateContent: productId='{}', contentId='{}'", productId, contentId);
        Content content = findContentInProduct(productId, contentId);

        String sanitizedBody = markdownSanitizer.sanitize(request.body());
        List<UUID> referencedNodeIds = extractAndValidateKgRefs(productId, sanitizedBody);
        DifficultyLevel difficultyLevel = parseDifficultyLevel(request.difficultyLevel());
        String metadataJson = writeMetadataJson(request.metadata());

        content.applyEdit(new Content.Edit(request.title(), request.type(), request.lang(), sanitizedBody,
                request.summary(), difficultyLevel, request.category(), request.topic(), metadataJson));
        contentRepository.save(content);
        log.info("updateContent: conteudo atualizado id='{}'", content.getId());

        syncKnowledgeGraph(productId, content, referencedNodeIds);

        return toSummary(content);
    }

    @Transactional
    public ContentSummary transition(UUID productId, UUID contentId, ContentTransitionRequest request,
                                     AuthenticatedUser caller) {
        log.debug("transition: productId='{}', contentId='{}', from='{}', to='{}'", productId, contentId, request.from(), request.to());
        return doTransition(productId, contentId, request, caller);
    }

    @Transactional
    public ContentSummary publish(UUID productId, UUID contentId, PublishContentRequest request,
                                  AuthenticatedUser caller) {
        log.debug("publish: productId='{}', contentId='{}'", productId, contentId);
        Content content = findContentInProduct(productId, contentId);
        ContentTransitionRequest asTransition = new ContentTransitionRequest(
                content.getStatus().contractValue(),
                ContentStatus.PUBLISHED.contractValue(),
                request == null ? null : request.comment()
        );
        return doTransition(productId, contentId, asTransition, caller);
    }

    @Transactional(readOnly = true)
    public List<ContentVersionSummary> listVersions(UUID productId, UUID contentId) {
        log.debug("listVersions: productId='{}', contentId='{}'", productId, contentId);
        findContentInProduct(productId, contentId);
        return versionRepository.findAllByContentIdOrderByCreatedAtAsc(contentId).stream()
                .map(version -> versionMapper.toSummary(version, resolveAuthorName(version.getCreatedBySubject())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> listEditEvents(UUID productId) {
        log.debug("listEditEvents: productId='{}'", productId);
        return contentRepository.findAllByProductId(productId).stream()
                .flatMap(content -> versionRepository.findAllByContentIdOrderByCreatedAtAsc(content.getId()).stream())
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .map(version -> editEventDescription(version, resolveAuthorName(version.getCreatedBySubject())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WorkflowItemSummary> listWorkflowItems(UUID productId) {
        log.debug("listWorkflowItems: productId='{}'", productId);
        return contentRepository.findAllByProductId(productId).stream()
                .map(content -> contentMapper.toWorkflowItem(content, resolveAuthorName(content.getAuthorSubject())))
                .toList();
    }

    /**
     * Nucleo de {@link #transition} e {@link #publish}, executado dentro da
     * transacao aberta pelo metodo publico chamador — nunca invocado via
     * {@code this} a partir de outro metodo {@code @Transactional} desta classe,
     * para nao contornar o proxy do Spring.
     */
    private ContentSummary doTransition(UUID productId, UUID contentId, ContentTransitionRequest request,
                                        AuthenticatedUser caller) {
        Content content = findContentInProduct(productId, contentId);
        ContentStatus from = parseStatus(request.from());
        ContentStatus to = parseStatus(request.to());
        if (content.getStatus() != from) {
            log.warn("doTransition: transicao rejeitada, status atual diverge do esperado contentId='{}', statusAtual='{}', statusEsperado='{}'",
                    contentId, content.getStatus(), from);
            throw new InvalidContentTransitionException(content.getStatus(), to);
        }

        workflowPolicy.assertAllowedTransition(from, to, caller.authorities());

        content.changeStatus(to);
        content.bumpVersion();
        if (to == ContentStatus.PUBLISHED) {
            content.markPublication(publicationTimestamp());
        }
        contentRepository.save(content);
        recordVersion(content, from, to, request.comment(), caller.subject());
        if (to == ContentStatus.PUBLISHED) {
            recordContentPublicationAudit(productId, caller.subject(), content, from);
        }
        log.info("doTransition: conteudo transicionado id='{}', status='{}'", content.getId(), content.getStatus());

        return toSummary(content);
    }

    private Content findContentInProduct(UUID productId, UUID contentId) {
        return contentRepository.findByProductIdAndId(productId, contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));
    }

    private ContentSummary toSummary(Content content) {
        return contentMapper.toSummary(content, resolveAuthorName(content.getAuthorSubject()), readMetadataJson(content.getMetadataJson()));
    }

    private ContentStatus parseStatus(String value) {
        try {
            return ContentStatus.fromContractValue(value);
        } catch (IllegalArgumentException _) {
            throw new InvalidContentStatusException(value);
        }
    }

    private DifficultyLevel parseDifficultyLevel(String value) {
        if (value == null) {
            return null;
        }
        try {
            return DifficultyLevel.fromContractValue(value);
        } catch (IllegalArgumentException _) {
            throw new InvalidDifficultyLevelException(value);
        }
    }

    private String publicationTimestamp() {
        return OffsetDateTime.now(ZoneOffset.UTC).toString();
    }

    private void recordContentCreationAudit(UUID tenantId, UUID productId, String actorSubject, Content content) {
        auditService.recordEvent(new AuditRecordCommand(
                tenantId, productId, actorSubject, "CONTENT_CREATED", TARGET_TYPE_CONTENT,
                content.getId().toString(), content.getTitle(), MODULE_CONTENT, null,
                Map.of("title", content.getTitle(), "type", content.getType(), DIFF_KEY_STATUS, content.getStatus().contractValue())
        ));
    }

    private void recordContentPublicationAudit(UUID productId, String actorSubject, Content content, ContentStatus from) {
        ProductReference product = productReferenceService.getRequiredReference(productId);
        auditService.recordEvent(new AuditRecordCommand(
                product.tenantId(), productId, actorSubject, "CONTENT_PUBLISHED", TARGET_TYPE_CONTENT,
                content.getId().toString(), content.getTitle(), MODULE_CONTENT,
                Map.of(DIFF_KEY_STATUS, from.contractValue()),
                Map.of(DIFF_KEY_STATUS, ContentStatus.PUBLISHED.contractValue())
        ));
    }

    private void recordVersion(Content content, ContentStatus from, ContentStatus to, String comment, String createdBySubject) {
        String snapshotJson = writeSnapshotJson(content, from, to, comment);
        versionRepository.save(new ContentVersion(content.getId(), "v" + content.getCurrentVersion(), snapshotJson, createdBySubject));
    }

    private String writeSnapshotJson(Content content, ContentStatus from, ContentStatus to, String comment) {
        try {
            return objectMapper.writeValueAsString(new ContentSnapshot(
                    content.getTitle(), content.getType(), content.getLang(), content.getBodyMarkdown(),
                    content.getSummary(), content.getDifficultyLevel() == null ? null : content.getDifficultyLevel().contractValue(),
                    content.getCategory(), content.getTopic(), from.contractValue(), to.contractValue(), comment
            ));
        } catch (JacksonException ex) {
            throw new IllegalStateException("Unable to serialize content version snapshot", ex);
        }
    }

    private String writeMetadataJson(Map<String, Object> metadata) {
        if (metadata == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Unable to serialize content metadata", ex);
        }
    }

    private Map<String, Object> readMetadataJson(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(metadataJson, METADATA_TYPE);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Unable to deserialize content metadata", ex);
        }
    }

    private String resolveAuthorName(String subject) {
        try {
            return identityUserDirectory.getRequiredUser(subject).displayName();
        } catch (RuntimeException _) {
            return subject;
        }
    }

    private String editEventDescription(ContentVersion version, String authorName) {
        return authorName + " moveu o conteudo para " + extractToStatus(version.getSnapshotJson()) + " (" + version.getVersionLabel() + ").";
    }

    private String extractToStatus(String snapshotJson) {
        try {
            Map<?, ?> snapshot = objectMapper.readValue(snapshotJson, Map.class);
            Object toStatus = snapshot.get("toStatus");
            return toStatus == null ? "" : toStatus.toString();
        } catch (JacksonException _) {
            return "";
        }
    }

    private List<UUID> extractAndValidateKgRefs(UUID productId, String body) {
        if (body == null) {
            return List.of();
        }
        List<UUID> nodeIds = new ArrayList<>();
        Matcher matcher = KG_REF_PATTERN.matcher(body);
        while (matcher.find()) {
            String rawNodeId = matcher.group(1);
            UUID nodeId = parseNodeId(rawNodeId);
            assertNodeExists(productId, nodeId, rawNodeId);
            nodeIds.add(nodeId);
        }
        return nodeIds;
    }

    private UUID parseNodeId(String rawNodeId) {
        try {
            return UUID.fromString(rawNodeId);
        } catch (IllegalArgumentException _) {
            throw new InvalidContentReferenceException(rawNodeId);
        }
    }

    private void assertNodeExists(UUID productId, UUID nodeId, String rawNodeId) {
        if (!knowledgeGraphPort.nodeExists(productId, nodeId)) {
            log.warn("assertNodeExists: referencia kg-ref invalida rejeitada productId='{}', nodeId='{}'", productId, rawNodeId);
            throw new InvalidContentReferenceException(rawNodeId);
        }
    }

    private void syncKnowledgeGraph(UUID productId, Content content, List<UUID> referencedNodeIds) {
        if (referencedNodeIds.isEmpty()) {
            return;
        }
        UUID sourceNodeId = ensureContentGraphNode(productId, content);
        for (UUID targetNodeId : referencedNodeIds) {
            knowledgeGraphPort.ensureRelatedToEdge(productId, sourceNodeId, targetNodeId);
        }
    }

    private UUID ensureContentGraphNode(UUID productId, Content content) {
        if (content.getGraphNodeId() != null) {
            return content.getGraphNodeId();
        }
        UUID nodeId = knowledgeGraphPort.ensureContentNode(productId, content.getId().toString(), content.getTitle());
        content.linkGraphNode(nodeId);
        contentRepository.save(content);
        return nodeId;
    }

    private record ContentSnapshot(
            String title, String type, String lang, String bodyMarkdown, String summary, String difficultyLevel,
            String category, String topic, String fromStatus, String toStatus, String comment
    ) {
    }
}
