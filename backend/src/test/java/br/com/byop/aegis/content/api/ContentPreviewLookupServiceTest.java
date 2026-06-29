package br.com.byop.aegis.content.api;

import br.com.byop.aegis.content.domain.Content;
import br.com.byop.aegis.content.domain.DifficultyLevel;
import br.com.byop.aegis.content.repository.ContentRepository;
import br.com.byop.aegis.knowledgegraph.api.GraphNodeContentPreview;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentPreviewLookupServiceTest {

    private static final UUID PRODUCT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID NODE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID CONTENT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final String SUMMARY = "Resumo";
    private static final String DIFFICULTY = "beginner";
    private static final String THUMBNAIL = "cover.png";

    @Mock
    private ContentRepository contentRepository;

    private ContentPreviewLookupService service;

    @BeforeEach
    void setUp() {
        service = new ContentPreviewLookupService(contentRepository);
    }

    @Test
    void shouldFindPreviewByGraphNodeId() {
        Content content = content("{\"thumbnail\":\"cover.png\"}");
        when(contentRepository.findByProductIdAndGraphNodeId(PRODUCT_ID, NODE_ID)).thenReturn(Optional.of(content));

        Optional<GraphNodeContentPreview> result = service.findPreview(PRODUCT_ID, NODE_ID, CONTENT_ID.toString());

        assertThat(result).contains(new GraphNodeContentPreview(SUMMARY, DIFFICULTY, THUMBNAIL));
    }

    @Test
    void shouldFallbackToContentReferenceId() {
        Content content = content(null);
        when(contentRepository.findByProductIdAndGraphNodeId(PRODUCT_ID, NODE_ID)).thenReturn(Optional.empty());
        when(contentRepository.findByProductIdAndId(PRODUCT_ID, CONTENT_ID)).thenReturn(Optional.of(content));

        Optional<GraphNodeContentPreview> result = service.findPreview(PRODUCT_ID, NODE_ID, CONTENT_ID.toString());

        assertThat(result).contains(new GraphNodeContentPreview(SUMMARY, DIFFICULTY, null));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nullablePreviewScenarios")
    void shouldMapNullablePreviewFields(String scenario, String metadataJson, DifficultyLevel difficultyLevel,
                                        String expectedDifficulty) {
        Content content = content(metadataJson, difficultyLevel);
        when(contentRepository.findByProductIdAndGraphNodeId(PRODUCT_ID, NODE_ID)).thenReturn(Optional.of(content));

        Optional<GraphNodeContentPreview> result = service.findPreview(PRODUCT_ID, NODE_ID, CONTENT_ID.toString());

        assertThat(result).contains(new GraphNodeContentPreview(SUMMARY, expectedDifficulty, null));
    }

    @Test
    void shouldReturnEmptyWhenReferenceIdIsNotUuid() {
        when(contentRepository.findByProductIdAndGraphNodeId(PRODUCT_ID, NODE_ID)).thenReturn(Optional.empty());

        Optional<GraphNodeContentPreview> result = service.findPreview(PRODUCT_ID, NODE_ID, "article-slug");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldIgnoreInvalidThumbnailMetadata() {
        Content content = content("{");
        when(contentRepository.findByProductIdAndGraphNodeId(PRODUCT_ID, NODE_ID)).thenReturn(Optional.of(content));

        Optional<GraphNodeContentPreview> result = service.findPreview(PRODUCT_ID, NODE_ID, CONTENT_ID.toString());

        assertThat(result).contains(new GraphNodeContentPreview(SUMMARY, DIFFICULTY, null));
    }

    private Content content(String metadataJson) {
        return content(metadataJson, DifficultyLevel.BEGINNER);
    }

    private Content content(String metadataJson, DifficultyLevel difficultyLevel) {
        Content content = new Content(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                PRODUCT_ID,
                "Spring Boot",
                "article",
                "pt-BR",
                "author-1"
        );
        content.applyEdit(new Content.Edit(
                "Spring Boot",
                "article",
                "pt-BR",
                "body",
                SUMMARY,
                difficultyLevel,
                "Java",
                "Spring",
                metadataJson
        ));
        content.linkGraphNode(NODE_ID);
        ReflectionTestUtils.setField(content, "id", CONTENT_ID);
        return content;
    }

    private static Stream<Arguments> nullablePreviewScenarios() {
        return Stream.of(
                Arguments.of("difficulty nula e thumbnail em branco", "{\"thumbnail\":\" \"}", null, null),
                Arguments.of("thumbnail ausente", "{}", DifficultyLevel.BEGINNER, DIFFICULTY),
                Arguments.of("metadata em branco", " ", DifficultyLevel.BEGINNER, DIFFICULTY)
        );
    }
}
