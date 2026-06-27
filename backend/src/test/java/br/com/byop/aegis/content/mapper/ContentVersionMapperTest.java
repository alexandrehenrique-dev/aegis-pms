package br.com.byop.aegis.content.mapper;

import br.com.byop.aegis.content.domain.ContentVersion;
import br.com.byop.aegis.content.dto.ContentVersionSummary;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ContentVersionMapperTest {

    private final ContentVersionMapper mapper = Mappers.getMapper(ContentVersionMapper.class);

    @Test
    void shouldMapVersionToSummary() {
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-06-26T10:00:00-03:00");
        ContentVersion version = new ContentVersion(UUID.randomUUID(), "v1", "{\"title\":\"Artigo\"}", "subject-1");
        ReflectionTestUtils.setField(version, "id", id);
        ReflectionTestUtils.setField(version, "createdAt", createdAt);

        ContentVersionSummary summary = mapper.toSummary(version, "Alexandre Silva");

        assertThat(summary.id()).isEqualTo(id);
        assertThat(summary.versionLabel()).isEqualTo("v1");
        assertThat(summary.createdByName()).isEqualTo("Alexandre Silva");
        assertThat(summary.createdAt()).isEqualTo(createdAt);
        assertThat(summary.snapshotJson()).isEqualTo("{\"title\":\"Artigo\"}");
    }

    @Test
    void shouldReturnNullWhenAllSourcesAreNull() {
        assertThat(mapper.toSummary(null, null)).isNull();
    }

    @Test
    void shouldMapOnlyCreatedByNameWhenVersionIsNull() {
        ContentVersionSummary summary = mapper.toSummary(null, "Only Author");

        assertThat(summary.createdByName()).isEqualTo("Only Author");
        assertThat(summary.versionLabel()).isNull();
    }
}
