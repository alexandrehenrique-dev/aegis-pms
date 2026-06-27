package br.com.byop.aegis.form.mapper;

import br.com.byop.aegis.form.domain.FormDefinition;
import br.com.byop.aegis.form.dto.FormDetail;
import br.com.byop.aegis.form.dto.FormSummary;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FormDefinitionMapperTest {

    private static final OffsetDateTime UPDATED_AT = OffsetDateTime.parse("2026-06-27T10:00:00Z");
    private static final OffsetDateTime LAST_ACTIVITY = OffsetDateTime.parse("2026-06-27T11:00:00Z");

    private final FormDefinitionMapper mapper = Mappers.getMapper(FormDefinitionMapper.class);

    @Test
    void shouldMapFormToSummary() {
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        FormDefinition form = form(id);
        form.publish("2026-06-27/forms/contato");

        FormSummary summary = mapper.toSummary(form, 12L, "8%", LAST_ACTIVITY);

        assertThat(summary.id()).isEqualTo(id);
        assertThat(summary.name()).isEqualTo("Contato");
        assertThat(summary.type()).isEqualTo("lead");
        assertThat(summary.status()).isEqualTo("Published");
        assertThat(summary.responses()).isEqualTo("12");
        assertThat(summary.conversion()).isEqualTo("8%");
        assertThat(summary.lastActivity()).isEqualTo(LAST_ACTIVITY.toString());
        assertThat(summary.publication()).isEqualTo("2026-06-27/forms/contato");
    }

    @Test
    void shouldUsePlaceholdersWhenActivityAndPublicationAreAbsent() {
        FormSummary summary = mapper.toSummary(form(UUID.randomUUID()), 0L, "0%", null);

        assertThat(summary.responses()).isEqualTo("0");
        assertThat(summary.lastActivity()).isEqualTo("—");
        assertThat(summary.publication()).isEqualTo("—");
    }

    @Test
    void shouldMapFormToDetail() {
        UUID id = UUID.fromString("22222222-2222-2222-2222-222222222222");
        FormDefinition form = form(id);
        List<Map<String, Object>> fields = List.of(Map.of("label", "Email", "type", "Email", "required", true));
        List<Map<String, Object>> delivery = List.of(Map.of("type", "email", "enabled", true));

        FormDetail detail = mapper.toDetail(form, fields, delivery);

        assertThat(detail.id()).isEqualTo(id);
        assertThat(detail.name()).isEqualTo("Contato");
        assertThat(detail.status()).isEqualTo("Draft");
        assertThat(detail.fields()).containsExactlyElementsOf(fields);
        assertThat(detail.deliveryChannels()).containsExactlyElementsOf(delivery);
        assertThat(detail.updatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    void shouldReturnNullWhenAllSourcesAreNull() {
        assertThat(mapper.toSummary(null, 0L, null, null)).isNull();
        assertThat(mapper.toDetail(null, null, null)).isNull();
    }

    @Test
    void shouldMapOnlyFieldsWhenFormIsNull() {
        List<Map<String, Object>> fields = List.of(Map.of("label", "Email"));

        FormDetail detail = mapper.toDetail(null, fields, null);

        assertThat(detail.fields()).containsExactlyElementsOf(fields);
        assertThat(detail.name()).isNull();
        assertThat(detail.status()).isNull();
    }

    @Test
    void shouldMapSummaryWithoutFormWhenDerivedSourcesExist() {
        FormSummary summary = mapper.toSummary(null, 7L, "2%", LAST_ACTIVITY);

        assertThat(summary.id()).isNull();
        assertThat(summary.responses()).isEqualTo("7");
        assertThat(summary.conversion()).isEqualTo("2%");
        assertThat(summary.lastActivity()).isEqualTo(LAST_ACTIVITY.toString());
        assertThat(summary.publication()).isNull();
    }

    @Test
    void shouldMapSummaryWithoutFormWhenOnlyConversionExists() {
        FormSummary summary = mapper.toSummary(null, 7L, "2%", null);

        assertThat(summary.conversion()).isEqualTo("2%");
        assertThat(summary.lastActivity()).isEqualTo("—");
    }

    @Test
    void shouldMapSummaryWithoutFormWhenOnlyLastActivityExists() {
        FormSummary summary = mapper.toSummary(null, 7L, null, LAST_ACTIVITY);

        assertThat(summary.conversion()).isNull();
        assertThat(summary.lastActivity()).isEqualTo(LAST_ACTIVITY.toString());
    }

    @Test
    void shouldMapOnlyDeliveryWhenFormIsNull() {
        List<Map<String, Object>> delivery = List.of(Map.of("type", "email", "enabled", true));

        FormDetail detail = mapper.toDetail(null, null, delivery);

        assertThat(detail.deliveryChannels()).containsExactlyElementsOf(delivery);
        assertThat(detail.fields()).isNull();
        assertThat(detail.publication()).isNull();
    }

    @Test
    void shouldExposeNullContractStatus() {
        assertThat(mapper.toContractStatus(null)).isNull();
    }

    private FormDefinition form(UUID id) {
        FormDefinition form = new FormDefinition(new FormDefinition.Creation(
                UUID.randomUUID(), UUID.randomUUID(), "Contato", "lead",
                "[{\"label\":\"Email\",\"type\":\"Email\",\"required\":true}]", "[]"
        ));
        ReflectionTestUtils.setField(form, "id", id);
        ReflectionTestUtils.setField(form, "updatedAt", UPDATED_AT);
        return form;
    }
}
