package br.com.byop.aegis.pages.mapper;

import br.com.byop.aegis.pages.domain.Page;
import br.com.byop.aegis.pages.domain.PageStatus;
import br.com.byop.aegis.pages.dto.PageDetail;
import br.com.byop.aegis.pages.dto.PageSectionResponse;
import br.com.byop.aegis.pages.dto.PageSummary;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PageMapperTest {

    private final PageMapper mapper = Mappers.getMapper(PageMapper.class);

    @Test
    void shouldMapPageToSummary() {
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Page page = page(id);

        PageSummary summary = mapper.toSummary(page);

        assertThat(summary.id()).isEqualTo(id);
        assertThat(summary.slug()).isEqualTo("home");
        assertThat(summary.title()).isEqualTo("Home");
        assertThat(summary.locale()).isEqualTo("pt-BR");
        assertThat(summary.status()).isEqualTo("draft");
        assertThat(summary.version()).isEqualTo(1);
    }

    @Test
    void shouldMapPageToDetailWithSeoAndSections() {
        UUID id = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID ogImageAssetId = UUID.randomUUID();
        Page page = page(id);
        page.applyEdit(new Page.Edit("home", "Home", "pt-BR", PageStatus.PUBLISHED, "Home | Aegis",
                "Descricao", "https://example.com", ogImageAssetId, true));
        List<PageSectionResponse> sections = List.of(
                new PageSectionResponse(UUID.randomUUID(), "hero", null, 0, Map.of("title", "Ola"), null)
        );

        PageDetail detail = mapper.toDetail(page, sections);

        assertThat(detail.id()).isEqualTo(id);
        assertThat(detail.status()).isEqualTo("published");
        assertThat(detail.seo().title()).isEqualTo("Home | Aegis");
        assertThat(detail.seo().description()).isEqualTo("Descricao");
        assertThat(detail.seo().canonical()).isEqualTo("https://example.com");
        assertThat(detail.seo().ogImageAssetId()).isEqualTo(ogImageAssetId);
        assertThat(detail.seo().noIndex()).isTrue();
        assertThat(detail.sections()).containsExactlyElementsOf(sections);
    }

    @Test
    void shouldReturnNullWhenPageIsNull() {
        assertThat(mapper.toSummary(null)).isNull();
        assertThat(mapper.toDetail(null, null)).isNull();
    }

    @Test
    void shouldMapOnlySectionsWhenPageIsNull() {
        List<PageSectionResponse> sections = List.of(
                new PageSectionResponse(UUID.randomUUID(), "text", null, 0, Map.of("body", "x"), null)
        );

        PageDetail detail = mapper.toDetail(null, sections);

        assertThat(detail.sections()).containsExactlyElementsOf(sections);
        assertThat(detail.title()).isNull();
        assertThat(detail.seo()).isNull();
    }

    @Test
    void shouldMapOnlyPageWhenSectionsAreNull() {
        Page page = page(UUID.fromString("33333333-3333-3333-3333-333333333333"));

        PageDetail detail = mapper.toDetail(page, null);

        assertThat(detail.title()).isEqualTo("Home");
        assertThat(detail.sections()).isNull();
    }

    @Test
    void shouldExposeNullContractStatus() {
        assertThat(mapper.toContractStatus(null)).isNull();
    }

    private Page page(UUID id) {
        Page page = new Page(UUID.randomUUID(), UUID.randomUUID(), "home", "Home", "pt-BR");
        ReflectionTestUtils.setField(page, "id", id);
        return page;
    }
}
