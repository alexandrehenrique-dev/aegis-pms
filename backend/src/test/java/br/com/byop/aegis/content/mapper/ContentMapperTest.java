package br.com.byop.aegis.content.mapper;

import br.com.byop.aegis.content.domain.Content;
import br.com.byop.aegis.content.domain.ContentStatus;
import br.com.byop.aegis.content.domain.DifficultyLevel;
import br.com.byop.aegis.content.dto.ContentSummary;
import br.com.byop.aegis.content.dto.WorkflowItemSummary;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ContentMapperTest {

    private static final OffsetDateTime FIXED_UPDATED_AT = OffsetDateTime.parse("2026-06-26T10:00:00-03:00");

    private final ContentMapper mapper = Mappers.getMapper(ContentMapper.class);

    @Test
    void shouldMapContentToSummaryWithAllFields() {
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Content content = content(id, FIXED_UPDATED_AT);
        content.applyEdit(new Content.Edit("Artigo", "article", "pt-BR", "corpo", "resumo",
                DifficultyLevel.BEGINNER, "categoria", "topico", "{}"));
        content.changeStatus(ContentStatus.IN_REVIEW);
        content.bumpVersion();
        content.markPublication("2026-06-26/site");

        ContentSummary summary = mapper.toSummary(content, "Alexandre Silva", Map.of("k", "v"));

        assertThat(summary.id()).isEqualTo(id);
        assertThat(summary.title()).isEqualTo("Artigo");
        assertThat(summary.type()).isEqualTo("article");
        assertThat(summary.lang()).isEqualTo("pt-BR");
        assertThat(summary.author()).isEqualTo("Alexandre Silva");
        assertThat(summary.status()).isEqualTo("In Review");
        assertThat(summary.updatedAt()).isEqualTo(FIXED_UPDATED_AT);
        assertThat(summary.publication()).isEqualTo("2026-06-26/site");
        assertThat(summary.version()).isEqualTo("v2");
        assertThat(summary.summary()).isEqualTo("resumo");
        assertThat(summary.difficultyLevel()).isEqualTo("beginner");
        assertThat(summary.body()).isEqualTo("corpo");
        assertThat(summary.category()).isEqualTo("categoria");
        assertThat(summary.topic()).isEqualTo("topico");
        assertThat(summary.metadata()).isEqualTo(Map.of("k", "v"));
    }

    @Test
    void shouldUsePlaceholderWhenPublicationIsAbsent() {
        Content content = content(UUID.randomUUID(), FIXED_UPDATED_AT);

        ContentSummary summary = mapper.toSummary(content, "Author", null);

        assertThat(summary.publication()).isEqualTo("—");
        assertThat(summary.difficultyLevel()).isNull();
    }

    @Test
    void shouldReturnNullWhenAllSourcesAreNull() {
        assertThat(mapper.toSummary(null, null, null)).isNull();
        assertThat(mapper.toWorkflowItem(null, null)).isNull();
    }

    @Test
    void shouldMapOnlyAuthorWhenContentIsNull() {
        ContentSummary summary = mapper.toSummary(null, "Only Author", null);

        assertThat(summary.author()).isEqualTo("Only Author");
        assertThat(summary.status()).isNull();
        assertThat(summary.version()).isNull();
        assertThat(summary.publication()).isNull();
    }

    @Test
    void shouldMapOnlyMetadataWhenContentAndAuthorAreNull() {
        ContentSummary summary = mapper.toSummary(null, null, Map.of("k", "v"));

        assertThat(summary.metadata()).isEqualTo(Map.of("k", "v"));
        assertThat(summary.author()).isNull();
        assertThat(summary.status()).isNull();
    }

    @Test
    void shouldMapOnlyAuthorWhenContentIsNullForWorkflowItem() {
        WorkflowItemSummary item = mapper.toWorkflowItem(null, "Only Author");

        assertThat(item.author()).isEqualTo("Only Author");
        assertThat(item.status()).isNull();
        assertThat(item.version()).isNull();
    }

    @Test
    void shouldMapContentToWorkflowItem() {
        UUID id = UUID.fromString("22222222-2222-2222-2222-222222222222");
        Content content = content(id, FIXED_UPDATED_AT);

        WorkflowItemSummary item = mapper.toWorkflowItem(content, "Alexandre Silva");

        assertThat(item.id()).isEqualTo(id);
        assertThat(item.title()).isEqualTo("Artigo inicial");
        assertThat(item.author()).isEqualTo("Alexandre Silva");
        assertThat(item.status()).isEqualTo("Draft");
        assertThat(item.version()).isEqualTo("v1");
    }

    @Test
    void shouldMapNullStatusAndDifficultyToNullContractValues() {
        assertThat(mapper.toContractStatus(null)).isNull();
        assertThat(mapper.toContractDifficulty(null)).isNull();
        assertThat(mapper.toContractPublication(null)).isEqualTo("—");
    }

    private Content content(UUID id, OffsetDateTime updatedAt) {
        Content content = new Content(UUID.randomUUID(), UUID.randomUUID(), "Artigo inicial", "article", "pt-BR", "subject-1");
        ReflectionTestUtils.setField(content, "id", id);
        ReflectionTestUtils.setField(content, "updatedAt", updatedAt);
        return content;
    }
}
