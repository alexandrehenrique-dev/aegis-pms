package br.com.byop.aegis.content.service;

import br.com.byop.aegis.content.contract.ContentTransitionRequest;
import br.com.byop.aegis.content.contract.CreateContentRequest;
import br.com.byop.aegis.content.contract.PublishContentRequest;
import br.com.byop.aegis.content.contract.UpdateContentRequest;
import br.com.byop.aegis.content.domain.Content;
import br.com.byop.aegis.content.domain.ContentStatus;
import br.com.byop.aegis.content.domain.ContentVersion;
import br.com.byop.aegis.content.dto.ContentSummary;
import br.com.byop.aegis.content.dto.ContentVersionSummary;
import br.com.byop.aegis.content.dto.WorkflowItemSummary;
import br.com.byop.aegis.content.exception.ContentNotFoundException;
import br.com.byop.aegis.content.exception.InsufficientContentRoleException;
import br.com.byop.aegis.content.exception.InvalidContentReferenceException;
import br.com.byop.aegis.content.exception.InvalidContentStatusException;
import br.com.byop.aegis.content.exception.InvalidContentTransitionException;
import br.com.byop.aegis.content.exception.InvalidDifficultyLevelException;
import br.com.byop.aegis.content.mapper.ContentMapper;
import br.com.byop.aegis.content.mapper.ContentVersionMapper;
import br.com.byop.aegis.content.repository.ContentRepository;
import br.com.byop.aegis.content.repository.ContentVersionRepository;
import br.com.byop.aegis.identity.api.IdentityUser;
import br.com.byop.aegis.identity.api.IdentityUserDirectory;
import br.com.byop.aegis.knowledgegraph.api.KnowledgeGraphPort;
import br.com.byop.aegis.product.api.ProductReference;
import br.com.byop.aegis.product.api.ProductReferenceService;
import br.com.byop.aegis.security.AuthenticatedUser;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.core.JacksonException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentServiceTest {

    private static final OffsetDateTime FIXED_UPDATED_AT = OffsetDateTime.parse("2026-06-26T10:00:00-03:00");

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private ContentVersionRepository versionRepository;

    @Mock
    private ContentMapper contentMapper;

    @Mock
    private ContentVersionMapper versionMapper;

    @Mock
    private IdentityUserDirectory identityUserDirectory;

    @Mock
    private KnowledgeGraphPort knowledgeGraphPort;

    @Mock
    private ProductReferenceService productReferenceService;

    private ContentService service;

    @BeforeEach
    void setUp() {
        service = new ContentService(
                contentRepository,
                versionRepository,
                contentMapper,
                versionMapper,
                new ContentWorkflowPolicy(),
                new MarkdownSanitizer(),
                identityUserDirectory,
                knowledgeGraphPort,
                productReferenceService,
                new ObjectMapper()
        );
    }

    @Test
    void shouldCreateContent() {
        UUID productId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        when(productReferenceService.getRequiredReference(productId)).thenReturn(new ProductReference(productId, tenantId));
        when(identityUserDirectory.getRequiredUser("subject-1")).thenReturn(user());
        ContentSummary summary = mock(ContentSummary.class);
        when(contentMapper.toSummary(any(), eq("Alexandre Silva"), any())).thenReturn(summary);
        CreateContentRequest request = new CreateContentRequest(
                "Artigo novo", "article", "pt-BR", "<p>corpo</p><script>alert(1)</script>",
                "resumo", "beginner", "categoria", "topico", Map.of("k", "v")
        );
        AuthenticatedUser caller = caller(Set.of("ROLE_EDITOR"));

        ContentSummary result = service.createContent(productId, request, caller);

        assertThat(result).isEqualTo(summary);
        ArgumentCaptor<Content> contentCaptor = ArgumentCaptor.forClass(Content.class);
        verify(contentRepository).save(contentCaptor.capture());
        Content created = contentCaptor.getValue();
        assertThat(created.getTenantId()).isEqualTo(tenantId);
        assertThat(created.getProductId()).isEqualTo(productId);
        assertThat(created.getAuthorSubject()).isEqualTo("subject-1");
        assertThat(created.getTitle()).isEqualTo("Artigo novo");
        assertThat(created.getBodyMarkdown()).isEqualTo("<p>corpo</p>");
        assertThat(created.getStatus()).isEqualTo(ContentStatus.DRAFT);
        assertThat(created.getCurrentVersion()).isEqualTo(1);
    }

    @Test
    void shouldCreateContentAndSyncKnowledgeGraphWhenBodyHasValidKgRef() {
        UUID productId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID targetNodeId = UUID.randomUUID();
        UUID createdNodeId = UUID.randomUUID();
        when(productReferenceService.getRequiredReference(productId)).thenReturn(new ProductReference(productId, tenantId));
        when(identityUserDirectory.getRequiredUser("subject-1")).thenReturn(user());
        when(contentMapper.toSummary(any(), any(), any())).thenReturn(mock(ContentSummary.class));
        when(knowledgeGraphPort.nodeExists(productId, targetNodeId)).thenReturn(true);
        when(knowledgeGraphPort.ensureContentNode(eq(productId), any(), eq("Artigo novo"))).thenReturn(createdNodeId);
        when(contentRepository.save(any(Content.class))).thenAnswer(invocation -> {
            Content saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
            }
            return saved;
        });
        CreateContentRequest request = new CreateContentRequest(
                "Artigo novo", "article", "pt-BR",
                "corpo com {{kg-ref:" + targetNodeId + ":Outro Artigo}}", null, null, null, null, null
        );
        AuthenticatedUser caller = caller(Set.of("ROLE_EDITOR"));

        service.createContent(productId, request, caller);

        verify(knowledgeGraphPort).ensureRelatedToEdge(productId, createdNodeId, targetNodeId);
    }

    @Test
    void shouldRejectCreateWhenKgRefNodeDoesNotExist() {
        UUID productId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID missingNodeId = UUID.randomUUID();
        when(productReferenceService.getRequiredReference(productId)).thenReturn(new ProductReference(productId, tenantId));
        when(knowledgeGraphPort.nodeExists(productId, missingNodeId)).thenReturn(false);
        CreateContentRequest request = new CreateContentRequest(
                "Artigo novo", "article", "pt-BR",
                "corpo com {{kg-ref:" + missingNodeId + ":Outro Artigo}}", null, null, null, null, null
        );
        AuthenticatedUser caller = caller(Set.of("ROLE_EDITOR"));

        assertThatThrownBy(() -> service.createContent(productId, request, caller))
                .isInstanceOf(InvalidContentReferenceException.class);

        verify(contentRepository, never()).save(any(Content.class));
    }

    @Test
    void shouldListContentForProduct() {
        UUID productId = UUID.randomUUID();
        Content content = content(productId, ContentStatus.DRAFT);
        ContentSummary summary = mock(ContentSummary.class);
        when(contentRepository.findAllByProductId(productId)).thenReturn(List.of(content));
        when(identityUserDirectory.getRequiredUser("subject-1")).thenReturn(user());
        when(contentMapper.toSummary(content, "Alexandre Silva", Map.of())).thenReturn(summary);

        assertThat(service.listContent(productId)).containsExactly(summary);
    }

    @Test
    void shouldGetContentScopedByProduct() {
        UUID productId = UUID.randomUUID();
        Content content = content(productId, ContentStatus.DRAFT);
        ContentSummary summary = mock(ContentSummary.class);
        when(contentRepository.findByProductIdAndId(productId, content.getId())).thenReturn(Optional.of(content));
        when(identityUserDirectory.getRequiredUser("subject-1")).thenReturn(user());
        when(contentMapper.toSummary(content, "Alexandre Silva", Map.of())).thenReturn(summary);

        assertThat(service.getContent(productId, content.getId())).isEqualTo(summary);
    }

    @Test
    void shouldTreatBlankMetadataJsonAsEmptyMap() {
        UUID productId = UUID.randomUUID();
        Content content = content(productId, ContentStatus.DRAFT);
        ReflectionTestUtils.setField(content, "metadataJson", "   ");
        when(contentRepository.findByProductIdAndId(productId, content.getId())).thenReturn(Optional.of(content));
        when(identityUserDirectory.getRequiredUser("subject-1")).thenReturn(user());
        ContentSummary summary = mock(ContentSummary.class);
        when(contentMapper.toSummary(content, "Alexandre Silva", Map.of())).thenReturn(summary);

        assertThat(service.getContent(productId, content.getId())).isEqualTo(summary);
    }

    @Test
    void shouldRejectGetWhenContentDoesNotBelongToProduct() {
        UUID productId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        when(contentRepository.findByProductIdAndId(productId, contentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getContent(productId, contentId))
                .isInstanceOf(ContentNotFoundException.class)
                .hasMessage("Content not found: " + contentId);
    }

    @Test
    void shouldUpdateContentAndSanitizeBody() {
        UUID productId = UUID.randomUUID();
        Content content = content(productId, ContentStatus.DRAFT);
        when(contentRepository.findByProductIdAndId(productId, content.getId())).thenReturn(Optional.of(content));
        when(identityUserDirectory.getRequiredUser("subject-1")).thenReturn(user());
        ContentSummary summary = mock(ContentSummary.class);
        when(contentMapper.toSummary(eq(content), eq("Alexandre Silva"), any())).thenReturn(summary);

        UpdateContentRequest request = new UpdateContentRequest(
                "Titulo editado", "article", "pt-BR",
                "<p>corpo</p><script>alert(1)</script>", "resumo", "beginner",
                "categoria", "topico", Map.of("k", "v")
        );

        ContentSummary result = service.updateContent(productId, content.getId(), request);

        assertThat(result).isEqualTo(summary);
        assertThat(content.getTitle()).isEqualTo("Titulo editado");
        assertThat(content.getBodyMarkdown()).isEqualTo("<p>corpo</p>");
        assertThat(content.getDifficultyLevel().contractValue()).isEqualTo("beginner");
        verify(contentRepository).save(content);
        verify(knowledgeGraphPort, never()).ensureContentNode(any(), any(), any());
    }

    @Test
    void shouldEnsureGraphNodeAndCreateEdgeWhenBodyHasValidKgRef() {
        UUID productId = UUID.randomUUID();
        UUID targetNodeId = UUID.randomUUID();
        Content content = content(productId, ContentStatus.DRAFT);
        when(contentRepository.findByProductIdAndId(productId, content.getId())).thenReturn(Optional.of(content));
        when(identityUserDirectory.getRequiredUser("subject-1")).thenReturn(user());
        when(contentMapper.toSummary(any(), any(), any())).thenReturn(mock(ContentSummary.class));
        when(knowledgeGraphPort.nodeExists(productId, targetNodeId)).thenReturn(true);
        UUID createdNodeId = UUID.randomUUID();
        when(knowledgeGraphPort.ensureContentNode(productId, content.getId().toString(), "Titulo")).thenReturn(createdNodeId);

        UpdateContentRequest request = new UpdateContentRequest(
                "Titulo", "article", "pt-BR",
                "corpo com {{kg-ref:" + targetNodeId + ":Outro Artigo}}", null, null, null, null, null
        );

        service.updateContent(productId, content.getId(), request);

        verify(knowledgeGraphPort).nodeExists(productId, targetNodeId);
        verify(knowledgeGraphPort).ensureContentNode(productId, content.getId().toString(), "Titulo");
        verify(knowledgeGraphPort).ensureRelatedToEdge(productId, createdNodeId, targetNodeId);
        assertThat(content.getGraphNodeId()).isEqualTo(createdNodeId);
    }

    @Test
    void shouldReuseExistingGraphNodeWhenAlreadyLinked() {
        UUID productId = UUID.randomUUID();
        UUID targetNodeId = UUID.randomUUID();
        UUID existingNodeId = UUID.randomUUID();
        Content content = content(productId, ContentStatus.DRAFT);
        content.linkGraphNode(existingNodeId);
        when(contentRepository.findByProductIdAndId(productId, content.getId())).thenReturn(Optional.of(content));
        when(identityUserDirectory.getRequiredUser("subject-1")).thenReturn(user());
        when(contentMapper.toSummary(any(), any(), any())).thenReturn(mock(ContentSummary.class));
        when(knowledgeGraphPort.nodeExists(productId, targetNodeId)).thenReturn(true);

        UpdateContentRequest request = new UpdateContentRequest(
                "Titulo", "article", "pt-BR",
                "corpo com {{kg-ref:" + targetNodeId + ":Outro Artigo}}", null, null, null, null, null
        );

        service.updateContent(productId, content.getId(), request);

        verify(knowledgeGraphPort, never()).ensureContentNode(any(), any(), any());
        verify(knowledgeGraphPort).ensureRelatedToEdge(productId, existingNodeId, targetNodeId);
    }

    @Test
    void shouldRejectUpdateWhenKgRefNodeDoesNotExist() {
        UUID productId = UUID.randomUUID();
        UUID missingNodeId = UUID.randomUUID();
        Content content = content(productId, ContentStatus.DRAFT);
        when(contentRepository.findByProductIdAndId(productId, content.getId())).thenReturn(Optional.of(content));
        when(knowledgeGraphPort.nodeExists(productId, missingNodeId)).thenReturn(false);

        UpdateContentRequest request = new UpdateContentRequest(
                "Titulo", "article", "pt-BR",
                "corpo com {{kg-ref:" + missingNodeId + ":Outro Artigo}}", null, null, null, null, null
        );
        UUID contentId = content.getId();

        assertThatThrownBy(() -> service.updateContent(productId, contentId, request))
                .isInstanceOf(InvalidContentReferenceException.class);

        verify(contentRepository, never()).save(any(Content.class));
    }

    @Test
    void shouldRejectUpdateWhenKgRefNodeIdIsNotAValidUuid() {
        UUID productId = UUID.randomUUID();
        Content content = content(productId, ContentStatus.DRAFT);
        when(contentRepository.findByProductIdAndId(productId, content.getId())).thenReturn(Optional.of(content));

        UpdateContentRequest request = new UpdateContentRequest(
                "Titulo", "article", "pt-BR",
                "corpo com {{kg-ref:not-a-uuid:Outro Artigo}}", null, null, null, null, null
        );
        UUID contentId = content.getId();

        assertThatThrownBy(() -> service.updateContent(productId, contentId, request))
                .isInstanceOf(InvalidContentReferenceException.class);

        verify(contentRepository, never()).save(any(Content.class));
    }

    @Test
    void shouldSkipKgRefValidationWhenBodyIsNull() {
        UUID productId = UUID.randomUUID();
        Content content = content(productId, ContentStatus.DRAFT);
        when(contentRepository.findByProductIdAndId(productId, content.getId())).thenReturn(Optional.of(content));
        when(identityUserDirectory.getRequiredUser("subject-1")).thenReturn(user());
        when(contentMapper.toSummary(any(), any(), any())).thenReturn(mock(ContentSummary.class));

        UpdateContentRequest request = new UpdateContentRequest(
                "Titulo", "article", "pt-BR", null, null, null, null, null, null
        );

        service.updateContent(productId, content.getId(), request);

        verify(knowledgeGraphPort, never()).ensureRelatedToEdge(any(), any(), any());
    }

    @Test
    void shouldRejectInvalidDifficultyLevel() {
        UUID productId = UUID.randomUUID();
        Content content = content(productId, ContentStatus.DRAFT);
        when(contentRepository.findByProductIdAndId(productId, content.getId())).thenReturn(Optional.of(content));

        UpdateContentRequest request = new UpdateContentRequest(
                "Titulo", "article", "pt-BR", "corpo", null, "invalid", null, null, null
        );
        UUID contentId = content.getId();

        assertThatThrownBy(() -> service.updateContent(productId, contentId, request))
                .isInstanceOf(InvalidDifficultyLevelException.class);

        verify(contentRepository, never()).save(any(Content.class));
    }

    @Test
    void shouldTransitionDraftToInReviewAndRecordVersion() {
        UUID productId = UUID.randomUUID();
        Content content = content(productId, ContentStatus.DRAFT);
        when(contentRepository.findByProductIdAndId(productId, content.getId())).thenReturn(Optional.of(content));
        when(identityUserDirectory.getRequiredUser("subject-1")).thenReturn(user());
        when(contentMapper.toSummary(any(), any(), any())).thenReturn(mock(ContentSummary.class));

        ContentTransitionRequest request = new ContentTransitionRequest("Draft", "In Review", "pronto para revisao");
        AuthenticatedUser caller = caller(Set.of("ROLE_EDITOR"));

        service.transition(productId, content.getId(), request, caller);

        assertThat(content.getStatus()).isEqualTo(ContentStatus.IN_REVIEW);
        assertThat(content.getCurrentVersion()).isEqualTo(2);
        ArgumentCaptor<ContentVersion> versionCaptor = ArgumentCaptor.forClass(ContentVersion.class);
        verify(versionRepository).save(versionCaptor.capture());
        assertThat(versionCaptor.getValue().getVersionLabel()).isEqualTo("v2");
        assertThat(versionCaptor.getValue().getCreatedBySubject()).isEqualTo(caller.subject());
        assertThat(versionCaptor.getValue().getSnapshotJson()).contains("pronto para revisao");
    }

    @ParameterizedTest
    @MethodSource("invalidTransitionScenarios")
    void shouldRejectInvalidTransitionScenarios(String from, String to, Class<? extends RuntimeException> expectedException) {
        UUID productId = UUID.randomUUID();
        Content content = content(productId, ContentStatus.DRAFT);
        when(contentRepository.findByProductIdAndId(productId, content.getId())).thenReturn(Optional.of(content));
        ContentTransitionRequest request = new ContentTransitionRequest(from, to, null);
        AuthenticatedUser superAdmin = caller(Set.of("ROLE_SUPER_ADMIN"));
        UUID contentId = content.getId();

        assertThatThrownBy(() -> service.transition(productId, contentId, request, superAdmin))
                .isInstanceOf(expectedException);

        verify(versionRepository, never()).save(any(ContentVersion.class));
    }

    private static Stream<Arguments> invalidTransitionScenarios() {
        return Stream.of(
                Arguments.of("In Review", "Published", InvalidContentTransitionException.class),
                Arguments.of("Draft", "Published", InvalidContentTransitionException.class),
                Arguments.of("Draft", "Unknown", InvalidContentStatusException.class)
        );
    }

    @Test
    void shouldRejectEditorPublishing() {
        UUID productId = UUID.randomUUID();
        Content content = content(productId, ContentStatus.IN_REVIEW);
        when(contentRepository.findByProductIdAndId(productId, content.getId())).thenReturn(Optional.of(content));
        ContentTransitionRequest request = new ContentTransitionRequest("In Review", "Published", null);
        AuthenticatedUser editor = caller(Set.of("ROLE_EDITOR"));
        UUID contentId = content.getId();

        assertThatThrownBy(() -> service.transition(productId, contentId, request, editor))
                .isInstanceOf(InsufficientContentRoleException.class);

        verify(versionRepository, never()).save(any(ContentVersion.class));
        assertThat(content.getStatus()).isEqualTo(ContentStatus.IN_REVIEW);
    }

    @ParameterizedTest
    @CsvSource({"ROLE_SUPER_ADMIN", "ROLE_TENANT_ADMIN", "ROLE_PRODUCT_MANAGER"})
    void shouldAllowAuthorizedRolesToPublishViaTransition(String role) {
        UUID productId = UUID.randomUUID();
        Content content = content(productId, ContentStatus.IN_REVIEW);
        when(contentRepository.findByProductIdAndId(productId, content.getId())).thenReturn(Optional.of(content));
        when(identityUserDirectory.getRequiredUser("subject-1")).thenReturn(user());
        when(contentMapper.toSummary(any(), any(), any())).thenReturn(mock(ContentSummary.class));
        ContentTransitionRequest request = new ContentTransitionRequest("In Review", "Published", null);
        AuthenticatedUser caller = caller(Set.of(role));
        UUID contentId = content.getId();

        service.transition(productId, contentId, request, caller);

        assertThat(content.getStatus()).isEqualTo(ContentStatus.PUBLISHED);
        assertThat(content.getPublication()).isNotBlank();
    }

    @Test
    void shouldPublishAsShortcutForTransitionToPublished() {
        UUID productId = UUID.randomUUID();
        Content content = content(productId, ContentStatus.IN_REVIEW);
        when(contentRepository.findByProductIdAndId(productId, content.getId())).thenReturn(Optional.of(content));
        when(identityUserDirectory.getRequiredUser("subject-1")).thenReturn(user());
        ContentSummary summary = mock(ContentSummary.class);
        when(contentMapper.toSummary(any(), any(), any())).thenReturn(summary);

        ContentSummary result = service.publish(productId, content.getId(), new PublishContentRequest("foi pra ar"),
                caller(Set.of("ROLE_TENANT_ADMIN")));

        assertThat(result).isEqualTo(summary);
        assertThat(content.getStatus()).isEqualTo(ContentStatus.PUBLISHED);
        ArgumentCaptor<ContentVersion> versionCaptor = ArgumentCaptor.forClass(ContentVersion.class);
        verify(versionRepository).save(versionCaptor.capture());
        assertThat(versionCaptor.getValue().getSnapshotJson()).contains("foi pra ar");
    }

    @Test
    void shouldPublishWithNullRequestBody() {
        UUID productId = UUID.randomUUID();
        Content content = content(productId, ContentStatus.IN_REVIEW);
        when(contentRepository.findByProductIdAndId(productId, content.getId())).thenReturn(Optional.of(content));
        when(identityUserDirectory.getRequiredUser("subject-1")).thenReturn(user());
        when(contentMapper.toSummary(any(), any(), any())).thenReturn(mock(ContentSummary.class));

        service.publish(productId, content.getId(), null, caller(Set.of("ROLE_SUPER_ADMIN")));

        assertThat(content.getStatus()).isEqualTo(ContentStatus.PUBLISHED);
    }

    @Test
    void shouldRecordVersionSnapshotWithDifficultyLevel() {
        UUID productId = UUID.randomUUID();
        Content content = content(productId, ContentStatus.DRAFT);
        content.applyEdit(new Content.Edit("Artigo", "article", "pt-BR", "corpo", "resumo",
                br.com.byop.aegis.content.domain.DifficultyLevel.BEGINNER, "categoria", "topico", null));
        when(contentRepository.findByProductIdAndId(productId, content.getId())).thenReturn(Optional.of(content));
        when(identityUserDirectory.getRequiredUser("subject-1")).thenReturn(user());
        when(contentMapper.toSummary(any(), any(), any())).thenReturn(mock(ContentSummary.class));

        ContentTransitionRequest request = new ContentTransitionRequest("Draft", "In Review", null);
        service.transition(productId, content.getId(), request, caller(Set.of("ROLE_EDITOR")));

        ArgumentCaptor<ContentVersion> versionCaptor = ArgumentCaptor.forClass(ContentVersion.class);
        verify(versionRepository).save(versionCaptor.capture());
        assertThat(versionCaptor.getValue().getSnapshotJson()).contains("beginner");
    }

    @Test
    void shouldRejectEditorPublishingViaShortcut() {
        UUID productId = UUID.randomUUID();
        Content content = content(productId, ContentStatus.IN_REVIEW);
        when(contentRepository.findByProductIdAndId(productId, content.getId())).thenReturn(Optional.of(content));
        PublishContentRequest request = new PublishContentRequest(null);
        AuthenticatedUser editor = caller(Set.of("ROLE_EDITOR"));
        UUID contentId = content.getId();

        assertThatThrownBy(() -> service.publish(productId, contentId, request, editor))
                .isInstanceOf(InsufficientContentRoleException.class);
    }

    @Test
    void shouldListVersionsInChronologicalOrder() {
        UUID productId = UUID.randomUUID();
        Content content = content(productId, ContentStatus.DRAFT);
        ContentVersion version = new ContentVersion(content.getId(), "v1", "{}", "subject-1");
        when(contentRepository.findByProductIdAndId(productId, content.getId())).thenReturn(Optional.of(content));
        when(versionRepository.findAllByContentIdOrderByCreatedAtAsc(content.getId())).thenReturn(List.of(version));
        when(identityUserDirectory.getRequiredUser("subject-1")).thenReturn(user());
        ContentVersionSummary summary = mock(ContentVersionSummary.class);
        when(versionMapper.toSummary(version, "Alexandre Silva")).thenReturn(summary);

        assertThat(service.listVersions(productId, content.getId())).containsExactly(summary);
    }

    @Test
    void shouldRejectListVersionsWhenContentNotInProduct() {
        UUID productId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        when(contentRepository.findByProductIdAndId(productId, contentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listVersions(productId, contentId))
                .isInstanceOf(ContentNotFoundException.class);
    }

    @Test
    void shouldListWorkflowItems() {
        UUID productId = UUID.randomUUID();
        Content content = content(productId, ContentStatus.DRAFT);
        when(contentRepository.findAllByProductId(productId)).thenReturn(List.of(content));
        when(identityUserDirectory.getRequiredUser("subject-1")).thenReturn(user());
        WorkflowItemSummary item = mock(WorkflowItemSummary.class);
        when(contentMapper.toWorkflowItem(content, "Alexandre Silva")).thenReturn(item);

        assertThat(service.listWorkflowItems(productId)).containsExactly(item);
    }

    @ParameterizedTest
    @MethodSource("editEventsScenarios")
    void shouldListEditEventsDerivedFromVersions(String versionLabel, String snapshotJson, String expectedEvent) {
        UUID productId = UUID.randomUUID();
        Content content = content(productId, ContentStatus.DRAFT);
        ContentVersion version = new ContentVersion(content.getId(), versionLabel, snapshotJson, "subject-1");
        when(contentRepository.findAllByProductId(productId)).thenReturn(List.of(content));
        when(versionRepository.findAllByContentIdOrderByCreatedAtAsc(content.getId())).thenReturn(List.of(version));
        when(identityUserDirectory.getRequiredUser("subject-1")).thenReturn(user());

        assertThat(service.listEditEvents(productId)).containsExactly(expectedEvent);
    }

    private static Stream<Arguments> editEventsScenarios() {
        return Stream.of(
                Arguments.of("v2", "{\"toStatus\":\"In Review\"}", "Alexandre Silva moveu o conteudo para In Review (v2)."),
                Arguments.of("v1", "{\"other\":\"value\"}", "Alexandre Silva moveu o conteudo para  (v1)."),
                Arguments.of("v1", "not valid json", "Alexandre Silva moveu o conteudo para  (v1).")
        );
    }

    @Test
    void shouldOrderEditEventsChronologicallyAcrossMultipleVersions() {
        UUID productId = UUID.randomUUID();
        Content content = content(productId, ContentStatus.DRAFT);
        ContentVersion second = new ContentVersion(content.getId(), "v2", "{\"toStatus\":\"Published\"}", "subject-1");
        ReflectionTestUtils.setField(second, "createdAt", FIXED_UPDATED_AT.plusMinutes(10));
        ContentVersion first = new ContentVersion(content.getId(), "v1", "{\"toStatus\":\"In Review\"}", "subject-1");
        ReflectionTestUtils.setField(first, "createdAt", FIXED_UPDATED_AT);
        when(contentRepository.findAllByProductId(productId)).thenReturn(List.of(content));
        when(versionRepository.findAllByContentIdOrderByCreatedAtAsc(content.getId())).thenReturn(List.of(first, second));
        when(identityUserDirectory.getRequiredUser("subject-1")).thenReturn(user());

        assertThat(service.listEditEvents(productId)).containsExactly(
                "Alexandre Silva moveu o conteudo para In Review (v1).",
                "Alexandre Silva moveu o conteudo para Published (v2)."
        );
    }

    @Test
    void shouldWrapSnapshotSerializationFailure() {
        ObjectMapper brokenObjectMapper = org.mockito.Mockito.mock(ObjectMapper.class);
        ContentService brokenService = new ContentService(
                contentRepository, versionRepository, contentMapper, versionMapper,
                new ContentWorkflowPolicy(), new MarkdownSanitizer(), identityUserDirectory,
                knowledgeGraphPort, productReferenceService, brokenObjectMapper
        );
        UUID productId = UUID.randomUUID();
        Content content = content(productId, ContentStatus.DRAFT);
        when(contentRepository.findByProductIdAndId(productId, content.getId())).thenReturn(Optional.of(content));
        when(brokenObjectMapper.writeValueAsString(any())).thenThrow(org.mockito.Mockito.mock(JacksonException.class));

        ContentTransitionRequest request = new ContentTransitionRequest("Draft", "In Review", null);
        AuthenticatedUser editor = caller(Set.of("ROLE_EDITOR"));
        UUID contentId = content.getId();

        assertThatThrownBy(() -> brokenService.transition(productId, contentId, request, editor))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("snapshot");
    }

    @Test
    void shouldWrapMetadataSerializationFailure() {
        ObjectMapper brokenObjectMapper = org.mockito.Mockito.mock(ObjectMapper.class);
        ContentService brokenService = new ContentService(
                contentRepository, versionRepository, contentMapper, versionMapper,
                new ContentWorkflowPolicy(), new MarkdownSanitizer(), identityUserDirectory,
                knowledgeGraphPort, productReferenceService, brokenObjectMapper
        );
        UUID productId = UUID.randomUUID();
        Content content = content(productId, ContentStatus.DRAFT);
        when(contentRepository.findByProductIdAndId(productId, content.getId())).thenReturn(Optional.of(content));
        when(brokenObjectMapper.writeValueAsString(any())).thenThrow(org.mockito.Mockito.mock(JacksonException.class));

        UpdateContentRequest request = new UpdateContentRequest(
                "Titulo", "article", "pt-BR", "corpo", null, null, null, null, Map.of("k", "v")
        );
        UUID contentId = content.getId();

        assertThatThrownBy(() -> brokenService.updateContent(productId, contentId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("metadata");
    }

    @Test
    void shouldWrapMetadataDeserializationFailure() {
        ObjectMapper brokenObjectMapper = org.mockito.Mockito.mock(ObjectMapper.class);
        ContentService brokenService = new ContentService(
                contentRepository, versionRepository, contentMapper, versionMapper,
                new ContentWorkflowPolicy(), new MarkdownSanitizer(), identityUserDirectory,
                knowledgeGraphPort, productReferenceService, brokenObjectMapper
        );
        UUID productId = UUID.randomUUID();
        Content content = content(productId, ContentStatus.DRAFT);
        ReflectionTestUtils.setField(content, "metadataJson", "{\"k\":\"v\"}");
        when(contentRepository.findByProductIdAndId(productId, content.getId())).thenReturn(Optional.of(content));
        when(brokenObjectMapper.readValue("{\"k\":\"v\"}", Map.class))
                .thenThrow(org.mockito.Mockito.mock(JacksonException.class));
        UUID contentId = content.getId();

        assertThatThrownBy(() -> brokenService.getContent(productId, contentId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("metadata");
    }

    @Test
    void shouldFallBackToSubjectWhenAuthorCannotBeResolved() {
        UUID productId = UUID.randomUUID();
        Content content = content(productId, ContentStatus.DRAFT);
        when(contentRepository.findAllByProductId(productId)).thenReturn(List.of(content));
        when(identityUserDirectory.getRequiredUser("subject-1")).thenThrow(new RuntimeException("keycloak unavailable"));
        WorkflowItemSummary item = mock(WorkflowItemSummary.class);
        when(contentMapper.toWorkflowItem(content, "subject-1")).thenReturn(item);

        assertThat(service.listWorkflowItems(productId)).containsExactly(item);
    }

    private Content content(UUID productId, ContentStatus status) {
        Content content = new Content(UUID.randomUUID(), productId, "Artigo inicial", "article", "pt-BR", "subject-1");
        ReflectionTestUtils.setField(content, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(content, "status", status);
        ReflectionTestUtils.setField(content, "updatedAt", FIXED_UPDATED_AT);
        return content;
    }

    private IdentityUser user() {
        return new IdentityUser("subject-1", "alexandre", "alexandre@byop.dev", "Alexandre", "Silva");
    }

    private AuthenticatedUser caller(Set<String> authorities) {
        return new AuthenticatedUser("subject-1", "alexandre@byop.dev", "alexandre", "Alexandre Silva", authorities);
    }

    private <T> T mock(Class<T> type) {
        return org.mockito.Mockito.mock(type);
    }
}
