package br.com.byop.aegis.pages.mapper;

import br.com.byop.aegis.pages.domain.Event;
import br.com.byop.aegis.pages.domain.EventAccessType;
import br.com.byop.aegis.pages.domain.EventVisibility;
import br.com.byop.aegis.pages.dto.EventDetail;
import br.com.byop.aegis.pages.dto.EventSummary;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventMapperTest {

    private final EventMapper mapper = Mappers.getMapper(EventMapper.class);

    @Test
    void shouldMapEventToSummary() {
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Event event = event(id);

        EventSummary summary = mapper.toSummary(event);

        assertThat(summary.id()).isEqualTo(id);
        assertThat(summary.title()).isEqualTo("Show ao vivo");
        assertThat(summary.datetime()).isEqualTo(LocalDateTime.parse("2026-07-12T16:00"));
        assertThat(summary.location()).isEqualTo("Teatro Municipal");
        assertThat(summary.type()).isEqualTo("public");
        assertThat(summary.visibility()).isEqualTo("public");
    }

    @Test
    void shouldMapEventToDetail() {
        UUID id = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID imageAssetId = UUID.randomUUID();
        Event event = event(id);
        event.applyEdit(new Event.Edit("Show remarcado", LocalDateTime.parse("2026-08-01T20:30"), "Novo local",
                EventAccessType.PRIVATE, EventVisibility.PUBLIC_SUMMARY, "descricao completa", imageAssetId));

        EventDetail detail = mapper.toDetail(event);

        assertThat(detail.id()).isEqualTo(id);
        assertThat(detail.title()).isEqualTo("Show remarcado");
        assertThat(detail.type()).isEqualTo("private");
        assertThat(detail.visibility()).isEqualTo("public-summary");
        assertThat(detail.description()).isEqualTo("descricao completa");
        assertThat(detail.imageAssetId()).isEqualTo(imageAssetId);
    }

    @Test
    void shouldReturnNullWhenEventIsNull() {
        assertThat(mapper.toSummary(null)).isNull();
        assertThat(mapper.toDetail(null)).isNull();
    }

    @Test
    void shouldExposeNullContractValues() {
        assertThat(mapper.toContractType(null)).isNull();
        assertThat(mapper.toContractVisibility(null)).isNull();
    }

    private Event event(UUID id) {
        Event event = new Event(UUID.randomUUID(), UUID.randomUUID(), "Show ao vivo",
                LocalDateTime.parse("2026-07-12T16:00"), "Teatro Municipal", EventAccessType.PUBLIC, EventVisibility.PUBLIC);
        ReflectionTestUtils.setField(event, "id", id);
        return event;
    }
}
