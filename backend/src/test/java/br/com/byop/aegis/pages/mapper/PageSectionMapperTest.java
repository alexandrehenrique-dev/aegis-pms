package br.com.byop.aegis.pages.mapper;

import br.com.byop.aegis.pages.domain.BlockType;
import br.com.byop.aegis.pages.domain.PageSection;
import br.com.byop.aegis.pages.dto.PageSectionResponse;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PageSectionMapperTest {

    private final PageSectionMapper mapper = Mappers.getMapper(PageSectionMapper.class);

    @Test
    void shouldMapSectionToResponse() {
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        PageSection section = section(id);
        Map<String, Object> content = Map.of("title", "Ola");
        Map<String, Object> settings = Map.of("align", "center");

        PageSectionResponse response = mapper.toResponse(section, content, settings);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.type()).isEqualTo("hero");
        assertThat(response.variant()).isEqualTo("centered");
        assertThat(response.order()).isZero();
        assertThat(response.content()).containsEntry("title", "Ola");
        assertThat(response.settings()).containsEntry("align", "center");
    }

    @Test
    void shouldReturnNullWhenSectionIsNull() {
        assertThat(mapper.toResponse(null, null, null)).isNull();
    }

    @Test
    void shouldMapOnlyContentWhenSectionIsNull() {
        Map<String, Object> content = Map.of("title", "Ola");

        PageSectionResponse response = mapper.toResponse(null, content, null);

        assertThat(response.content()).containsEntry("title", "Ola");
        assertThat(response.type()).isNull();
    }

    @Test
    void shouldMapOnlySettingsWhenSectionAndContentAreNull() {
        Map<String, Object> settings = Map.of("align", "center");

        PageSectionResponse response = mapper.toResponse(null, null, settings);

        assertThat(response.settings()).containsEntry("align", "center");
        assertThat(response.content()).isNull();
    }

    @Test
    void shouldExposeNullContractType() {
        assertThat(mapper.toContractType(null)).isNull();
    }

    private PageSection section(UUID id) {
        PageSection section = new PageSection(UUID.randomUUID(), BlockType.HERO, "centered", 0, "{\"title\":\"Ola\"}", null);
        ReflectionTestUtils.setField(section, "id", id);
        return section;
    }
}
